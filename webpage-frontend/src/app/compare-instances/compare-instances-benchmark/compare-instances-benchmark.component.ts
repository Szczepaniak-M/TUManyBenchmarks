import {Component, Input, OnInit} from "@angular/core";
import {BenchmarkResult, Instance, Plot} from "../../instance-list/instance.model";
import {environment} from "../../../environments/environment";

@Component({
  selector: "app-compare-instances-benchmark",
  template: `
    <div class="border-2 rounded p-2">
      <div class="flex flex-row content-center">
        <h3 class="text-xl">
          {{ benchmarkName }}
        </h3>
        <a [href]="environment.repositoryUrl + '/tree/main/' + benchmarkDirectory"
           class="text-lg text-gray-500 ml-1 font-medium hover:underline cursor-pointer">
          (see details)
        </a>
      </div>
      <div class="text-base mb-2">{{ this.benchmarkDescription }}</div>
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
  benchmarkDirectory: string = "";
  benchmarkResults: BenchmarkResult[][] = [];
  plots: Plot[] = [];
  instanceNames: string[] = [];

  ngOnInit(): void {
    this.instances.forEach(instance => {
      const foundBenchmark = instance.benchmarks.find(benchmark => benchmark.id === this.benchmarkId);
      if (foundBenchmark !== undefined) {
        this.benchmarkName = foundBenchmark.name;
        this.benchmarkDescription = foundBenchmark.description;
        this.benchmarkDirectory = foundBenchmark.directory;
        this.benchmarkResults.push(foundBenchmark.results);
        this.plots = foundBenchmark.plots;
        this.instanceNames.push(instance.name);
      }
    });
  }

  protected readonly environment = environment;
}
