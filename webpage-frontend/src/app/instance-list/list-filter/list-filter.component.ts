import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";
import {Filter} from "./list-filter.model";

@Component({
  selector: "app-list-filter",
  template: `
    <div class="p-2 border border-gray-300 rounded flex flex-col">
      <div class="flex flex-row">
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Name:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Name"
                 [(ngModel)]="filter.name"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Min On-Demand Price:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Min On-Demand Price"
                 [(ngModel)]="filter.minOnDemandPrice"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Max On-Demand Price:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Max On-Demand Price"
                 [(ngModel)]="filter.maxOnDemandPrice"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Min Spot Price:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Min Spot Price"
                 [(ngModel)]="filter.minSpotPrice"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Max Spot Price:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Max Spot Price"
                 [(ngModel)]="filter.maxSpotPrice"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Min vCPU:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Min vCPU"
                 [(ngModel)]="filter.minCpu"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Max vCPU:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Max vCPU"
                 [(ngModel)]="filter.maxCpu"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
      </div>
      <div class="flex flex-row">
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Min Memory:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Min Memory"
                 [(ngModel)]="filter.minMemory"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/7">
          <mat-label class="font-medium">Max Memory:</mat-label>
          <input matInput type="text"
                 class="border p-2 rounded w-full"
                 placeholder="Max Memory"
                 [(ngModel)]="filter.maxMemory"
                 (ngModelChange)="onFilterChange()">
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/5">
          <mat-label class="font-medium">Network</mat-label>
          <mat-select multiple [(ngModel)]="filter.network" (ngModelChange)="onFilterChange()">
            <mat-option
              *ngFor="let network of allNetworks.sort()"
              [value]="network">{{ network }}
            </mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/5">
          <mat-label class="font-medium">Storage</mat-label>
          <mat-select multiple [(ngModel)]="filter.storage" (ngModelChange)="onFilterChange()">
            <mat-option
              *ngFor="let storage of allStorage.sort()"
              [value]="storage">{{ storage }}
            </mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/5">
          <mat-label class="font-medium">Tags (all of):</mat-label>
          <mat-select multiple [(ngModel)]="filter.tagsAll" (ngModelChange)="onFilterChange()">
            <mat-option
              *ngFor="let tag of allTags.sort()"
              [value]="tag">{{ tag }}
            </mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/5">
          <mat-label class="font-medium">Tags (any of):</mat-label>
          <mat-select multiple [(ngModel)]="filter.tagsAny" (ngModelChange)="onFilterChange()">
            <mat-option
              *ngFor="let tag of allTags.sort()"
              [value]="tag">{{ tag }}
            </mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field class="border rounded m-1 w-1/5">
          <mat-label class="font-medium">Benchmark:</mat-label>
          <mat-select [(ngModel)]="filter.benchmark" (ngModelChange)="onFilterChange()">
            <mat-option [value]="">None</mat-option>
            <mat-option
              *ngFor="let benchmark of allBenchmarks.sort()"
              [value]="benchmark.id">{{ benchmark.name }}
            </mat-option>
          </mat-select>
        </mat-form-field>
        <button class=" m-1 bg-gray-800 text-white w-1/7 p-2 rounded disabled:bg-slate-500"
                (click)="onRedirectToComparison()"
                [disabled]="selectedInstances < 2">
          <p>Compare</p>
          <p>({{ selectedInstances }} / 3)</p>
        </button>
      </div>
    </div>
  `,
  styleUrls: ["./list-filter.component.css"],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListFilterComponent {
  @Input({required: true}) allNetworks!: string[];
  @Input({required: true}) allStorage!: string[];
  @Input({required: true}) allTags!: string[];
  @Input({required: true}) allBenchmarks!: { name: string, id: string }[];
  @Input({required: true}) selectedInstances!: number;
  @Output() filterChange = new EventEmitter<Filter>();
  @Output() redirectToComparison = new EventEmitter<void>();
  filter: Filter = {};

  onFilterChange(): void {
    this.filterChange.emit(this.filter);
  }

  onRedirectToComparison(): void {
    this.redirectToComparison.emit();
  }
}
