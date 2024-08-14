import {Component, Input, OnInit} from "@angular/core";
import {BenchmarkResult, Instance, Plot} from "../../instance-list/instance.model";

@Component({
  selector: "app-compare-instances-benchmark",
  template: `
    <div class="border-2 rounded p-2">
      <div class="text-2xl">{{ this.benchmarkName }}</div>
      <div class="text-lg mb-2">{{ this.benchmarkDescription }}</div>
      <div *ngFor="let plot of plots" class="mb-4 border-2 rounded">
        <app-benchmark-plot [benchmarkResults]="benchmarkResults"
                            [plot]="plot"
                            [instances]="instanceNames"/>
      </div>
    </div>
  `
})
export class CompareInstancesBenchmarkComponent implements OnInit {
  @Input({required: true}) benchmarkId!: string;
  @Input({required: true}) instances!: Instance[];
  benchmarkName: string = "";
  benchmarkDescription: string = "";
  benchmarkResults: BenchmarkResult[][] = [];
  plots: Plot[] = [];
  instanceNames: string[] = [];

  ngOnInit(): void {
    this.instances.forEach(instance => {
      const foundBenchmark = instance.benchmarks.find(benchmark => benchmark.id === this.benchmarkId);
      if (foundBenchmark !== undefined) {
        this.benchmarkName = foundBenchmark.name;
        this.benchmarkDescription = foundBenchmark.description;
        this.benchmarkResults.push(foundBenchmark.results);
        this.plots = foundBenchmark.plots;
        this.instanceNames.push(instance.name);
      }
    });
  }
}
