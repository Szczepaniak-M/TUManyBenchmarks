package de.tum.cit.cs.benchmarkservice.config

import com.mongodb.client.model.Indexes
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
    fun createView() = runBlocking {
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
            // add benchmark field if missing
            Document(
                "\$addFields",
                Document(
                    "benchmarks",
                    Document("\$ifNull", listOf("\$benchmarks", mutableListOf<Any>()))
                )
            ),
            // prepare lookup table for join
            Document(
                "\$lookup",
                Document("from", "benchmarks")
                    .append("localField", "benchmarks.benchmark")
                    .append("foreignField", "_id")
                    .append("as", "benchmarksLookup")
            ),
            // map all elements in 'benchmark' list and perform join
            Document(
                "\$addFields",
                Document(
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
                                                        mutableListOf(
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
            // format output
            Document(
                "\$project",
                Document("_id", 1L)
                    .append("name", 1L)
                    .append("tags", 1L)
                    .append(
                        "benchmarks",
                        Document(
                            "\$map",
                            Document("input", "\$benchmarks")
                                .append("as", "b")
                                .append(
                                    "in",
                                    Document("_id", "\$\$b.benchmark._id")
                                        .append("name", "\$\$b.benchmark.configuration.name")
                                        .append("description", "\$\$b.benchmark.configuration.description")
                                        .append("timestamp", "\$\$b.timestamp")
                                        .append("values", "\$\$b.values")
                                        .append("nodes", "\$\$b.benchmark.nodes")
                                        .append("instanceTypes", "\$\$b.benchmark.configuration.instanceTypes")
                                        .append("instanceTags", "\$\$b.benchmark.configuration.instanceTags")
                                )
                        )
                    )
            )
        )
        database.createView(viewName, "instances", pipeline).awaitFirstOrNull()
        logger.info { "Created view `$viewName` in database" }
    }
}
