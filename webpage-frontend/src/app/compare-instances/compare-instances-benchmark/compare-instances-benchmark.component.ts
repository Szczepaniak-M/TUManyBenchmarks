import {Component, Input, OnInit} from "@angular/core";
import {InstanceDetails} from "../../instance-details/instance-details.model";
import {CompareInstancesBenchmark} from "./compare-instances-benchmark.model";

@Component({
  selector: "app-compare-instances-benchmark",
  template: `
    <div>
      <div>{{this.benchmarkName}}</div>
      <div>{{this.benchmarkDescription}}</div>
      <div *ngFor="let plot of benchmarkResults[0].plots" class="mb-2">
        <app-benchmark-plot [benchmarkResults]="benchmarkResults"
                            [plot]="plot"/>
      </div>
    </div>
  `
})
export class CompareInstancesBenchmarkComponent implements OnInit {
  @Input({required: true}) benchmarkId!: string;
  @Input({required: true}) instances!: InstanceDetails[];
  benchmarkResults: CompareInstancesBenchmark[] = []
  benchmarkName: string = ""
  benchmarkDescription: string = ""

  ngOnInit(): void {
    this.instances.forEach(instance => {
      const foundBenchmark = instance.benchmarks.find(benchmark => benchmark.id === this.benchmarkId)
      if (foundBenchmark !== undefined) {
        this.benchmarkResults.push({
          instanceName: instance.name,
          benchmarkResults: foundBenchmark.results,
          plots: foundBenchmark.plots
        })
        this.benchmarkName = foundBenchmark.name
        this.benchmarkDescription = foundBenchmark.description
      }
    })
  }
}
