package de.tum.cit.cs.benchmarkservice.service

import de.tum.cit.cs.benchmarkservice.model.Benchmark
import de.tum.cit.cs.benchmarkservice.model.Instance
import de.tum.cit.cs.benchmarkservice.model.InstanceWithBenchmarks
import org.springframework.stereotype.Service

@Service
class InstanceService {
    fun findMatchingBenchmarks(instance: Instance, benchmarks: List<Benchmark>): InstanceWithBenchmarks {
        return InstanceWithBenchmarks(instance, benchmarks)
    }
}
