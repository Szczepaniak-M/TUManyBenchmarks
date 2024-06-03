package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.Instance
import de.tum.cit.cs.benchmarkservice.model.InstanceWithBenchmarks
import org.springframework.stereotype.Service

@Service
class InstanceService {
    fun findMatchingBenchmarks(instance: Instance, benchmarks: List<Benchmark>): InstanceWithBenchmarks {
        val applicableBenchmarks = mutableListOf<Benchmark>()
        for (benchmark in benchmarks) {
            if (benchmark.configuration.instanceType?.contains(instance.name) == true) {
                applicableBenchmarks.add(benchmark)
            } else if (benchmark.configuration.instanceTags != null){
                for(tagList in benchmark.configuration.instanceTags) {
                    if (instance.tags.containsAll(tagList)) {
                        applicableBenchmarks.add(benchmark)
                        break
                    }
                }
            }
        }
        return InstanceWithBenchmarks(instance, applicableBenchmarks)
    }
}
