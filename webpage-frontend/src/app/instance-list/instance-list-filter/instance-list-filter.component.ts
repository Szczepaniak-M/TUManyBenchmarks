import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {Filter} from "./instance-list-filter.model";

@Component({
  selector: 'app-instance-list-filter',
  template: `
    <div class="p-2 border border-gray-300 rounded flex">
      <mat-form-field class="border rounded m-1 w-1/8">
        <mat-label class="font-medium">Name:</mat-label>
        <input matInput type="text" [(ngModel)]="filter.name" (ngModelChange)="onFilterChange()"
               class="border p-2 rounded w-full" placeholder="Name">
      </mat-form-field>
      <mat-form-field class="border rounded m-1 w-1/8">
        <mat-label class="font-medium">Min vCPU:</mat-label>
        <input matInput type="text" [(ngModel)]="filter.minCpu" (ngModelChange)="onFilterChange()"
               class="border p-2 rounded w-full" placeholder="Min vCPU">
      </mat-form-field>
      <mat-form-field class="border rounded m-1 w-1/8">
        <mat-label class="font-medium">Max vCPU:</mat-label>
        <input matInput type="text" [(ngModel)]="filter.maxCpu" (ngModelChange)="onFilterChange()"
               class="border p-2 rounded w-full" placeholder="Min vCPU">
      </mat-form-field>
      <mat-form-field class="border rounded m-1 w-1/8">
        <mat-label class="font-medium">Min Memory:</mat-label>
        <input matInput type="text" [(ngModel)]="filter.minMemory" (ngModelChange)="onFilterChange()"
               class="border p-2 rounded w-full" placeholder="Min Memory">
      </mat-form-field>
      <mat-form-field class="border rounded m-1 w-1/8">
        <mat-label class="font-medium">Max Memory:</mat-label>
        <input matInput type="text" [(ngModel)]="filter.maxMemory" (ngModelChange)="onFilterChange()"
               class="border p-2 rounded w-full" placeholder="Max Memory">
      </mat-form-field>
      <mat-form-field class="border rounded m-1 w-1/8">
        <mat-label>Network</mat-label>
        <mat-select multiple [(ngModel)]="filter.network" (ngModelChange)="onFilterChange()">
          <mat-option
            *ngFor="let network of allNetworks.sort()"
            [value]="network">{{ network }}
          </mat-option>
        </mat-select>
      </mat-form-field>
      <mat-form-field class="border rounded m-1 w-1/8">
        <mat-label>Tags:</mat-label>
        <mat-select multiple [(ngModel)]="filter.tags" (ngModelChange)="onFilterChange()">
          <mat-option
            *ngFor="let tag of allTags.sort()"
            [value]="tag">{{ tag }}
          </mat-option>
        </mat-select>
      </mat-form-field>
      <button (click)="onRedirectToComparison()"
              [disabled]="selectedInstances < 2"
              class=" m-1 bg-gray-800 text-white w-1/8 p-2 rounded disabled:bg-slate-500">
        <p>Compare</p>
        <p>({{ selectedInstances }} / 3)</p>
      </button>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush

})
export class InstanceListFilterComponent {
  @Input({ required: true }) allNetworks!: string[];
  @Input({ required: true }) allTags!: string[];
  @Input({ required: true }) selectedInstances!: number;
  @Output() filterChange = new EventEmitter<Filter>();
  @Output() redirectToComparison = new EventEmitter<void>();

  filter: Filter = {}

  onFilterChange(): void {
    this.filterChange.emit(this.filter);
  }

  onRedirectToComparison(): void {
    this.redirectToComparison.emit();
  }
}
