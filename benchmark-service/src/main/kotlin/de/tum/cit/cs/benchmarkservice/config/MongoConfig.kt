package de.tum.cit.cs.benchmarkservice.config

import de.tum.cit.cs.benchmarkservice.model.OutputType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions


@Configuration
class MongoConfig {

    @Bean
    fun customConversions(): MongoCustomConversions {
        return MongoCustomConversions(
            listOf(
                StringToOutputTypeConverter(),
                OutputTypeToStringConverter()
            )
        )
    }
}

@ReadingConverter
class StringToOutputTypeConverter : Converter<String, OutputType> {
    override fun convert(source: String): OutputType {
        return when (source) {
            "single-node-single-value" -> OutputType.SINGLE_NODE_SINGLE_VALUE
            "single-node-multiple-value" -> OutputType.SINGLE_NODE_MULTIPLE_VALUES
            "multiple-nodes-single-value" -> OutputType.MULTIPLE_NODES_SINGLE_VALUE
            "multiple-nodes-multiple-values" -> OutputType.MULTIPLE_NODES_MULTIPLE_VALUES
            else -> throw IllegalArgumentException("Unsupported output type $source")
        }
    }
}

@WritingConverter
class OutputTypeToStringConverter : Converter<OutputType, String> {
    override fun convert(source: OutputType): String {
        return when (source) {
            OutputType.SINGLE_NODE_SINGLE_VALUE -> "single-node-single-value"
            OutputType.SINGLE_NODE_MULTIPLE_VALUES -> "single-node-multiple-value"
            OutputType.MULTIPLE_NODES_SINGLE_VALUE -> "multiple-nodes-single-value"
            OutputType.MULTIPLE_NODES_MULTIPLE_VALUES -> "multiple-nodes-multiple-values"

        }
    }
}
