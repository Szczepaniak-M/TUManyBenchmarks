import {Component, OnInit} from "@angular/core";
import {ActivatedRoute} from "@angular/router";
import {InstanceDetails} from "../instance-details/instance-details.model";
import {InstanceDetailsService} from "../instance-details/instance-details.service";

@Component({
  selector: "app-compare-instances",
  template: `
    <div class="container mx-auto p-4">
      <h1 class="text-2xl font-bold mb-4">Comparison</h1>

      <div class="mb-4">
        <mat-slide-toggle (change)="toggleShowOnlyCommon()">
          <span class="ml-2 text-l">Show only common benchmarks</span>
        </mat-slide-toggle>
      </div>

      <div class="flex flex-row">
        <div class="mb-2 border-2 p-2"
             *ngFor="let instance of instances" [style.width.%]="100/instances.length">
          <h1 class="text-2xl font-bold mb-4">{{ instance.name }}</h1>

          <div class="mb-4">
            <h2 class="text-xl font-semibold">Instance Details</h2>
            <p><strong>vCPU:</strong> {{ instance.vcpu }}</p>
            <p><strong>Network:</strong> {{ instance.network }}</p>
            <p><strong>Memory:</strong> {{ instance.memory }}</p>
            <p><strong>Tags:</strong> {{ instance.otherTags.join(", ") }}</p>
          </div>
        </div>
      </div>

      <div *ngFor="let benchmarkId of benchmarkIds">
        <app-compare-instances-benchmark
          *ngIf="showOnlyCommon ? commonBenchmarkIds.has(benchmarkId) : true"
          [benchmarkId]="benchmarkId"
          [instances]="instances"
        />
      </div>
    </div>
  `
})
export class CompareInstancesComponent implements OnInit {
  instances: InstanceDetails[] = [];
  showOnlyCommon: boolean = false;
  benchmarkIds: Set<string> = new Set<string>();
  commonBenchmarkIds: Set<string> = new Set<string>();

  constructor(private route: ActivatedRoute, private instanceDetailsService: InstanceDetailsService) {
  }

  ngOnInit(): void {
    const names = this.route.snapshot.queryParamMap.get("instances")!.split(",");
    names.forEach(name => {
      this.instanceDetailsService.getInstanceDetails(name).subscribe(data => {
        this.instances.push(data);
        this.calculateBenchmarks()
        this.calculateCommonBenchmarks();
      });
    });
  }

  toggleShowOnlyCommon(): void {
    this.showOnlyCommon = !this.showOnlyCommon;
  }

  private calculateBenchmarks() {
    if (this.instances.length > 0) {
      this.instances.forEach(instance => {
        instance.benchmarks.forEach(benchmark => {
          this.benchmarkIds.add(benchmark.id);
        })
      })
    }
  }

  private calculateCommonBenchmarks(): void {
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

}
