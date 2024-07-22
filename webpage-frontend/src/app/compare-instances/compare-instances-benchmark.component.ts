import {Component, Input} from '@angular/core';
import {Benchmark} from "../instance-details/instance-details.model";

@Component({
  selector: 'app-compare-instances-benchmark',
  template:`
    <div>
      <div>{{ benchmarksMap[benchmarkId][comparisonList[0].id]?.name }}</div>
      <div>{{ benchmarksMap[benchmarkId][comparisonList[0].id]?.description }}</div>
      <div class="flex flex-row">
        <div class="py-2 px-4" *ngFor="let instance of comparisonList" [style.width.%]="100/comparisonList.length">
          <div *ngIf="benchmarksMap[benchmarkId][instance.id]">
            <div>{{instance.name}}</div>
            <div>{{ JSON.stringify(benchmarksMap[benchmarkId][instance.id])}}</div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class CompareInstancesBenchmarkComponent {
  @Input() benchmarkId!: string;
  @Input() benchmarksMap!: { [key: string]: { [key: string]: Benchmark } };
  @Input() comparisonList!: { id: string, name: string }[];
  protected readonly JSON = JSON;
}
