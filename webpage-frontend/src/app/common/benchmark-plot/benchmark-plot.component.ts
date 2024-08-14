import {Component, Input} from "@angular/core";
import {BenchmarkResult, Plot} from "../../instance-list/instance.model";

@Component({
  selector: "app-benchmark-plot",
  template: `
    <div class="text-center">
      <app-benchmark-scatter-plot
        *ngIf="plot.type === 'scatter'"
        [benchmarkResults]="benchmarkResults"
        [plot]="plot"
        [instances]="instances"/>
      <app-benchmark-line-plot
        *ngIf="plot.type === 'line'"
        [benchmarkResults]="benchmarkResults"
        [plot]="plot"
        [instances]="instances"
      />
    </div>
  `
})
export class BenchmarkPlotComponent {
  @Input({required: true}) benchmarkResults!: BenchmarkResult[][];
  @Input({required: true}) plot!: Plot;
  @Input() instances: string[] = [];
}
