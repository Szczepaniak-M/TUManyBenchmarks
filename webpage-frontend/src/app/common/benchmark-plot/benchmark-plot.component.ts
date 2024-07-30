import {Component, Input} from "@angular/core";
import {BenchmarkResult, Plot} from "../../instance-details/instance-details.model";


@Component({
  selector: "app-benchmark-plot",
  template: `
    <div style="text-align:center">
      <app-benchmark-scatter-plot
        *ngIf="plot.type === 'scatter'"
        [benchmarkResults]="benchmarkResults"
        [plot]="plot"
        [multipleInstances]="multipleInstances"/>
      <app-benchmark-line-plot
        *ngIf="plot.type === 'line'"
        [benchmarkResults]="benchmarkResults"
        [plot]="plot"
        [multipleInstances]="multipleInstances"
      />
    </div>
  `
})
export class BenchmarkPlotComponent {
  @Input({required: true}) benchmarkResults!: BenchmarkResult[]
  @Input({required: true}) plot!: Plot;
  @Input() multipleInstances: boolean = false;
}