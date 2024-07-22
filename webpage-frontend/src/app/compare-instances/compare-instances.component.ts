import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Benchmark, InstanceDetails} from "../instance-details/instance-details.model";
import {InstanceDetailsService} from "../instance-details/instance-details.service";

@Component({
  selector: 'app-compare-instances',
  template: `
    <div class="container mx-auto p-4">
      <h1 class="text-2xl font-bold mb-4">Comparison</h1>

      <div class="mb-4">
        <label class="inline-flex items-center">
          <input type="checkbox" (change)="toggleShowOnlyCommon()" class="form-checkbox h-5 w-5">
          <span class="ml-2">Show only common benchmarks</span>
        </label>
      </div>

      <div class="flex flex-row">
        <div class="mb-2" *ngFor="let instance of instances" [style.width.%]="100/instances.length">
          <h1 class="text-2xl font-bold mb-4">{{ instance.name }}</h1>

          <div class="mb-4">
            <h2 class="text-xl font-semibold">Instance Details</h2>
            <p><strong>vCPU:</strong> {{ instance.vCpu }}</p>
            <p><strong>Network:</strong> {{ instance.network }}</p>
            <p><strong>Memory:</strong> {{ instance.memory }}</p>
            <p><strong>Tags:</strong> {{ instance.otherTags.join(', ') }}</p>
          </div>
        </div>
      </div>

      <app-compare-instances-benchmark
        *ngFor="let benchmarkId of getBenchmarksToDisplay() | keyvalue"
        [benchmarkId]="benchmarkId.key"
        [benchmarksMap]="getBenchmarksToDisplay()"
        [comparisonList]="instances"
      />
    </div>
  `
})
export class CompareInstancesComponent implements OnInit {
  instances: InstanceDetails[] = [];
  showOnlyCommon: boolean = false;
  commonBenchmarkIds: Set<string> = new Set<string>();

  constructor(private route: ActivatedRoute, private instanceDetailsService: InstanceDetailsService) {
  }

  ngOnInit(): void {
    const names = this.route.snapshot.queryParamMap.get('instances')!.split(',');
    names.forEach(name => {
      this.instanceDetailsService.getInstanceDetails(name).subscribe(data => {
        this.instances.push(data);
        this.calculateCommonBenchmarks();
      });
    });
  }

  calculateCommonBenchmarks(): void {
    if (this.instances.length > 0) {
      const benchmarkCounts: { [key: string]: number } = {};
      this.instances.forEach(instance => {
        instance.benchmarks.forEach(benchmark => {
          benchmarkCounts[benchmark.id] = (benchmarkCounts[benchmark.id] || 0) + 1;
        });
      });
      this.commonBenchmarkIds = new Set<string>(
        Object.keys(benchmarkCounts).filter(id => benchmarkCounts[id] === this.instances.length)
      );
    }
  }

  toggleShowOnlyCommon(): void {
    this.showOnlyCommon = !this.showOnlyCommon;
  }

  getBenchmarksToDisplay(): { [key: string]: { [key: string]: Benchmark } } {
    const benchmarksMap: { [key: string]: { [key: string]: Benchmark } } = {};
    this.instances.forEach(instance => {
      instance.benchmarks.forEach(benchmark => {
        if (!benchmarksMap[benchmark.id]) {
          benchmarksMap[benchmark.id] = {};
        }
        benchmarksMap[benchmark.id][instance.id] = benchmark;
      });
    });

    if (this.showOnlyCommon) {
      Object.keys(benchmarksMap).forEach(key => {
        if (!this.commonBenchmarkIds.has(key)) {
          delete benchmarksMap[key];
        }
      });
    }

    return benchmarksMap;
  }

}
