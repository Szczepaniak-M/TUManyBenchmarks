import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-instance-list-filter',
  template:`
    <div class="mb-4 p-4 border border-gray-300 rounded flex">
      <div class="mb-4">
        <label class="block mb-1 font-medium">Name:</label>
        <input type="text" [(ngModel)]="selectedFilters.name" (ngModelChange)="onFilterChange()"
               class="border p-2 rounded w-full" placeholder="Filter by name">
      </div>
      <div class="mb-4">
        <label class="block mb-1 font-medium">vCPU:</label>
        <select multiple [(ngModel)]="selectedFilters.vCPU" (ngModelChange)="onFilterChange()"
                class="border p-2 rounded w-full">
          <option *ngFor="let vCPU of allVCPUs" [value]="vCPU">{{ vCPU }}</option>
        </select>
      </div>
      <div class="mb-4">
        <label class="block mb-1 font-medium">Network:</label>
        <select multiple [(ngModel)]="selectedFilters.network" (ngModelChange)="onFilterChange()"
                class="border p-2 rounded w-full">
          <option *ngFor="let network of allNetworks" [value]="network">{{ network }}</option>
        </select>
      </div>
      <div class="mb-4">
        <label class="block mb-1 font-medium">RAM:</label>
        <select multiple [(ngModel)]="selectedFilters.RAM" (ngModelChange)="onFilterChange()"
                class="border p-2 rounded w-full">
          <option *ngFor="let ram of allRAMs" [value]="ram">{{ ram }}</option>
        </select>
      </div>
      <div class="mb-4">
        <label class="block mb-1 font-medium">Tags:</label>
        <select multiple [(ngModel)]="selectedFilters.tags" (ngModelChange)="onFilterChange()"
                class="border p-2 rounded w-full">
          <option *ngFor="let tag of allTags" [value]="tag">{{ tag }}</option>
        </select>
      </div>
      <button (click)="onRedirectToComparison()" class="bg-blue-500 text-white px-4 py-2 rounded">Go to Comparison
        ({{ selectedFilters.tags.length }})
      </button>
    </div>
  `
})
export class InstanceListFilterComponent {
  @Input() allTags: string[] = [];
  @Input() allVCPUs: string[] = [];
  @Input() allNetworks: string[] = [];
  @Input() allRAMs: string[] = [];
  @Output() filterChange = new EventEmitter<any>();
  @Output() redirectToComparison = new EventEmitter<void>();

  selectedFilters = {
    name: '',
    vCPU: [] as string[],
    network: [] as string[],
    RAM: [] as string[],
    tags: [] as string[],
  };

  constructor() {}

  ngOnInit(): void {}

  onFilterChange(): void {
    this.filterChange.emit(this.selectedFilters);
  }

  onRedirectToComparison(): void {
    this.redirectToComparison.emit();
  }
}
