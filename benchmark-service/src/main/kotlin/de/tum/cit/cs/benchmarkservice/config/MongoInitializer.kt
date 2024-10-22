package de.tum.cit.cs.benchmarkservice.config

import com.mongodb.client.model.Accumulators.*
import com.mongodb.client.model.Aggregates.*
import com.mongodb.client.model.Field
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Projections.*
import com.mongodb.client.model.QuantileMethod
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


@Component
@Profile("!test")
class MongoInitializer(
    @Value("\${spring.data.mongodb.uri}")
    private val mongoUri: String,
    private val mongoClient: MongoClient
) {
    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun initializeMongoDb() = runBlocking {
        val databaseName = extractDatabaseName(mongoUri)
        val database = mongoClient.getDatabase(databaseName)
        createIndexOnInstanceName(database)
        createInstanceDetailsView(database)
        createBenchmarkStatisticsView(database)
        createBenchmarkDetailsView(database)
    }

    private fun extractDatabaseName(uri: String): String {
        val uriParts = uri.split("/")
        return uriParts.last().split("?").first()
    }

    private suspend fun createIndexOnInstanceName(database: MongoDatabase) {
        database.getCollection("instances")
            .createIndex(Indexes.ascending("name"))
            .awaitFirstOrNull()
        logger.info { "Created index on field 'name' in collection 'instances'" }
    }

    private suspend fun createInstanceDetailsView(database: MongoDatabase) {
        val viewName = "instance-details"
        deleteViewIfExists(database, viewName)
        val pipeline = listOf(
            // ensure benchmarks array exists
            set(Field("benchmarks", Document("\$ifNull", listOf("\$benchmarks", emptyList<Document>())))),
            // create a lookup for 'benchmarks' collection
            lookup(
                "benchmarks",
                "benchmarks.benchmark",
                "_id",
                "benchmarksLookup"
            ),
            // merge benchmarks in 'instances' collection with benchmarks from 'benchmarks' collection
            set(
                Field(
                    "benchmarks",
                    Document(
                        "\$map",
                        Document("input", "\$benchmarks")
                            .append(
                                "in",
                                Document(
                                    "\$mergeObjects", listOf(
                                        "\$\$this",
                                        Document(
                                            "benchmark",
                                            Document(
                                                "\$arrayElemAt", listOf(
                                                    "\$benchmarksLookup",
                                                    Document(
                                                        "\$indexOfArray",
                                                        listOf(
                                                            "\$benchmarksLookup._id",
                                                            "\$\$this.benchmark"
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                    )
                )
            ),
            // select specific fields and reshape benchmarks
            project(
                fields(
                    include("_id", "name", "vCpu", "memory", "network", "storage", "tags"),
                    computed(
                        "benchmarks",
                        Document(
                            "\$map",
                            Document("input", "\$benchmarks")
                                .append("as", "b")
                                .append(
                                    "in", Document()
                                        .append("name", "\$\$b.benchmark.configuration.name")
                                        .append("description", "\$\$b.benchmark.configuration.description")
                                        .append("directory", "\$\$b.benchmark.configuration.directory")
                                        .append("results", "\$\$b.results")
                                        .append("_id", "\$\$b.benchmark._id")
                                        .append("plots", "\$\$b.benchmark.plots")
                                )
                        )
                    )
                )
            )
        )

        database.createView(viewName, "instances", pipeline).awaitFirstOrNull()
        logger.info { "Created view `$viewName` in database" }
    }

    private suspend fun createBenchmarkStatisticsView(database: MongoDatabase) {
        val viewName = "benchmark-statistics"
        deleteViewIfExists(database, viewName)
        val pipeline = listOf(
            // unwind to split data
            unwind("\$benchmarks"),
            unwind("\$benchmarks.results"),
            // select required fields and transform values
            project(
                fields(
                    computed("instanceId", "\$_id"),
                    computed("benchmarkId", "\$benchmarks.benchmark"),
                    computed(
                        "kv",
                        Document("\$objectToArray", "\$benchmarks.results.values")
                    )
                )
            ),
            // unwind key-value pairs
            unwind("\$kv"),
            // change single value results to single element lists
            set(
                Field("key", "\$kv.k"),
                Field(
                    "value", Document(
                        "\$cond",
                        Document("if", Document("\$isArray", "\$kv.v"))
                            .append("then", "\$kv.v")
                            .append("else", listOf("\$kv.v"))
                    )
                )
            ),
            // unwind and group by instanceId, benchmarkId, and series
            // while grouping calculate statistics
            unwind("\$value"),
            group(
                Document("instanceId", "\$instanceId")
                    .append("benchmarkId", "\$benchmarkId")
                    .append("key", "\$key"),
                min("min", "\$value"),
                max("max", "\$value"),
                avg("avg", "\$value"),
                median("median", "\$value", QuantileMethod.approximate())
            ),
            // project values
            project(
                fields(
                    excludeId(),
                    computed("instanceId", "\$_id.instanceId"),
                    computed("benchmarkId", "\$_id.benchmarkId"),
                    computed("series", "\$_id.key"),
                    include("min", "max", "avg", "median")
                )
            ),
            match(
                Document("avg", Document("\$ne", null))
            )
        )

        database.createView(viewName, "instances", pipeline).awaitFirstOrNull()
        logger.info { "Created view `$viewName` in database" }
    }

    private suspend fun createBenchmarkDetailsView(database: MongoDatabase) {
        val viewName = "benchmark-details"
        deleteViewIfExists(database, viewName)
        val pipeline = listOf(
            unwind("\$plots"),
            unwind("\$plots.series"),
            group(
                Document("_id", "\$_id"),
                first("name", "\$configuration.name"),
                first("description", "\$configuration.description"),
                first("instanceTypes", "\$configuration.instanceTypes"),
                first("instanceTags", "\$configuration.instanceTags"),
                first("seriesOther", "\$seriesOther"),
                push("seriesX", "\$plots.series.x"),
                push("seriesY", "\$plots.series.y")
            ),
            set(Field("_id", "\$_id._id"))
        )

        database.createView(viewName, "benchmarks", pipeline).awaitFirstOrNull()
        logger.info { "Created view `$viewName` in database" }
    }

    private suspend fun deleteViewIfExists(database: MongoDatabase, viewName: String) {
        val isViewCreated = database.listCollectionNames()
            .filter(Document("name", viewName))
            .awaitFirstOrNull()
        if (isViewCreated != null) {
            database.getCollection(viewName).drop().awaitFirstOrNull()
            logger.info { "Old view `$viewName` deleted from database" }
        }
    }
}
