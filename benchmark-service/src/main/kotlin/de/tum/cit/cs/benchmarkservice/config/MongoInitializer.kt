package de.tum.cit.cs.benchmarkservice.config

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Field
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Projections
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoDatabase
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component


@Component
class MongoInitializer {

    @Value("\${spring.data.mongodb.uri}")
    lateinit var mongoUri: String

    @Autowired
    lateinit var mongoClient: MongoClient

    private val viewName = "instance-details"

    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun initializeMongoDb() = runBlocking {
        val databaseName = extractDatabaseName(mongoUri)
        val database = mongoClient.getDatabase(databaseName)
        createIndexOnInstanceName(database)
        createInstanceDetailsViewIfNotExists(database)
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

    private suspend fun createInstanceDetailsViewIfNotExists(database: MongoDatabase) {
        val isViewCreated = database.listCollectionNames()
            .filter(Document("name", viewName))
            .awaitFirstOrNull()
        if (isViewCreated != null) {
            database.getCollection(viewName).drop().awaitFirstOrNull()
            logger.info { "Old view `$viewName` deleted from database" }
        }
        val pipeline = listOf(
            // ensure benchmarks array exists
            Aggregates.addFields(Field("benchmarks", Document("\$ifNull", listOf("\$benchmarks", emptyList<Any>())))),

            // create a lookup for 'benchmarks' collection
            Aggregates.lookup("benchmarks", "benchmarks.benchmark", "_id", "benchmarksLookup"),

            // merge benchmarks in 'instances' collection with benchmarks from 'benchmarks' collection
            Aggregates.addFields(
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
            Aggregates.project(
                Projections.fields(
                    Projections.include("_id", "name", "tags"),
                    Projections.computed(
                        "benchmarks", Document(
                            "\$map",
                            Document("input", "\$benchmarks")
                                .append("as", "b")
                                .append(
                                    "in", Document()
                                        .append("name", "\$\$b.benchmark.configuration.name")
                                        .append("description", "\$\$b.benchmark.configuration.description")
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
}
