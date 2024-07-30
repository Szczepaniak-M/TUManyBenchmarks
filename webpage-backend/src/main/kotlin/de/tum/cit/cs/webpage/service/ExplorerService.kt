package de.tum.cit.cs.webpage.service

import com.mongodb.MongoCommandException
import de.tum.cit.cs.webpage.model.ExplorerRequest
import de.tum.cit.cs.webpage.model.ExplorerResponse
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.Document
import org.bson.json.JsonParseException
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.dao.UncategorizedDataAccessException
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.LimitOperation
import org.springframework.stereotype.Service

@Service
class ExplorerService(
    private val reactiveMongoTemplate: ReactiveMongoTemplate
) {
    suspend fun parseQuery(explorerRequest: ExplorerRequest, requestId: String?, apiKey: String?): ExplorerResponse {
        val (partialResults, aggregationStages) = explorerRequest
        return if (partialResults) {
            processStagesWithPartialResults(aggregationStages)
        } else {
            processStagesWithoutPartialResults(aggregationStages)
        }
    }

    private suspend fun processStagesWithPartialResults(aggregationStages: List<String>): ExplorerResponse {
        var totalQueries = 0
        var successfulQueries = 0
        var error: String? = null
        val results = mutableListOf<String>()
        val compiledStages = mutableListOf<AggregationOperation>()
        for (stage in aggregationStages) {
            totalQueries++
            compiledStages.add(AggregationOperation { _ -> Document.parse(stage) })
            if (aggregationStages.size != totalQueries) {
                compiledStages.add(LimitOperation(5))
            }
            val (result, e) = tryToExecuteQuery(compiledStages)
            if (result != null) {
                results.add(result)
                successfulQueries++
            } else {
                error = e
                break
            }
            if (aggregationStages.size != totalQueries) {
                compiledStages.removeLast()
            }
        }
        return ExplorerResponse(totalQueries, successfulQueries, results, error)
    }

    private suspend fun processStagesWithoutPartialResults(aggregationStages: List<String>): ExplorerResponse {
        val compiledStages = aggregationStages.map { AggregationOperation { _ -> Document.parse(it) } }
        val (result, error) = tryToExecuteQuery(compiledStages)
        return if (result != null) {
            ExplorerResponse(1, 1, listOf(result), null)
        } else {
            ExplorerResponse(1, 0, emptyList(), error)
        }
    }

    private suspend fun tryToExecuteQuery(stages: List<AggregationOperation>): Pair<String?, String?> {
        var error: String? = null
        var result: String? = null
        val aggregation = Aggregation.newAggregation(stages)
        try {
            result = reactiveMongoTemplate.aggregate(aggregation, "instance-details", Document::class.java)
                .map { it.toJson() }
                .collectList()
                .map { it.joinToString(", ", "[", "]") }.awaitSingle()
        } catch (e: JsonParseException) {
            error = e.message
        } catch (e: InvalidDataAccessApiUsageException) {
            error = "All elements in the list should be JSON objects"
        } catch (e: UncategorizedDataAccessException) {
            val exception = e.cause as MongoCommandException
            error = exception.errorMessage.replace("Atlas documentation", "MongoDB documentation")
        }catch (e :Exception) {
            error = "Unknown error occurred"
        }
        return result to error
    }
}
