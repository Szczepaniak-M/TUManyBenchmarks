import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, QueryList, ViewChildren} from '@angular/core';
import {Router} from '@angular/router';
import {Instance} from "./instance.model";
import {InstanceListService} from "./instance-list.service";
import {Filter} from "./instance-list-filter/instance-list-filter.model";
import {SortDirection, SortEvent} from "./instance-list-sort/instance-list-sort.model";
import {InstanceListSortComponent} from "./instance-list-sort/instance-list-sort.component";

@Component({
  selector: 'app-instance-list',
  template: `
    <div class="container mx-auto py-4">
      <app-instance-list-filter
        [allTags]="allTags"
        [allNetworks]="allNetworks"
        [selectedInstances]="selectedInstances.length"
        (filterChange)="applyFilters($event)"
        (redirectToComparison)="compareSelectedItems()">
      </app-instance-list-filter>

      <div class="flex flex-col">
        <div class="flex flex-row border bg-gray-100">
          <div class="py-2 px-4 w-1/6">
            <app-instance-list-sort
              [column]="columns.name"
              (sort)="onSort($event)">
            </app-instance-list-sort>
          </div>
          <div class="py-2 px-4 w-1/6 text-left font-bold">
            <app-instance-list-sort
              [column]="columns.vCpu"
              (sort)="onSort($event)">
            </app-instance-list-sort>
          </div>
          <div class="py-2 px-4 w-1/6 text-left font-bold">
            <app-instance-list-sort
              [column]="columns.memory"
              (sort)="onSort($event)">
            </app-instance-list-sort>
          </div>
          <div class="py-2 px-4 w-1/6 text-left font-bold">
            <app-instance-list-sort
              [column]="columns.network"
              (sort)="onSort($event)">
            </app-instance-list-sort>
          </div>
          <div class="py-2 px-4 w-1/3 flex items-center">
            <p class="text-left font-bold">Tags</p>
          </div>
        </div>
        <app-instance-list-row
          *ngFor="let instance of displayedInstances; trackBy: trackByName"
          [instance]="instance"
          [isInComparison]="isInComparison(instance)"
          [onToggleComparison]="toggleComparison.bind(this)"
        />
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InstanceListComponent implements OnInit {
  columns = {
    name: "Name",
    vCpu: "vCPU",
    memory: "Memory",
    network: "Network"
  }
  instances: Instance[] = [];
  displayedInstances: Instance[] = [];
  selectedInstances: Instance[] = [];
  allTags: string[] = [];
  allNetworks: string[] = [];

  @ViewChildren(InstanceListSortComponent) headers!: QueryList<InstanceListSortComponent>;


  constructor(private instanceListService: InstanceListService,
              private router: Router,
              private changeDetectorRef: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.instanceListService.getInstances().subscribe(response => {
      this.instances = response.sort((a, b) => a.name.localeCompare(b.name));
      this.displayedInstances = this.instances;
      this.updateFilters()
      this.changeDetectorRef.markForCheck();
    });
  }

  updateFilters(): void {
    const tagsSet = new Set<string>();
    const networksSet = new Set<string>();

    this.instances.forEach(instance => {
      instance.otherTags.forEach(tag => tagsSet.add(tag));
      networksSet.add(instance.network);
    });

    this.allTags = Array.from(tagsSet);
    this.allNetworks = Array.from(networksSet);
  }

  applyFilters(filter: Filter): void {
    this.displayedInstances = this.instances.filter(instance => {
      const matchesName = filter.name ? instance.name.toLowerCase().includes(filter.name.toLowerCase()) : true;
      const matchesMinCpu = filter.minCpu ? instance.vcpu >= filter.minCpu : true
      const matchesMaxCpu = filter.maxCpu ? instance.vcpu <= filter.maxCpu : true
      const matchesMinMemory = filter.minMemory ? instance.memory >= filter.minMemory : true
      const matchesMaxMemory = filter.maxMemory ? instance.memory <= filter.maxMemory : true
      const matchesNetwork = filter.network && filter.network.length ? filter.network.includes(instance.network) : true;
      const matchesTags = filter.tags && filter.tags.length ? filter.tags.every(tag => instance.otherTags.includes(tag)) : true;
      return matchesName && matchesMinCpu && matchesMaxCpu && matchesMinMemory && matchesMaxMemory && matchesNetwork && matchesTags;
    });
  }

  toggleComparison(instance: Instance): boolean {
    if (this.isInComparison(instance)) {
      this.selectedInstances = this.selectedInstances.filter(item => item !== instance);
      return false
    } else if (this.selectedInstances.length < 3) {
      this.selectedInstances.push(instance);
      return true
    }
    return false
  }

  goToDetails(instance: Instance): void {
    this.router.navigate(['/instance', instance.name]).then();
  }

  compareSelectedItems(): void {
    const selectedNames = this.selectedInstances.map(item => item.name);
    this.router.navigate(['/instance/compare'],
      {queryParams: {instances: selectedNames.join(',')}})
      .then();
  }

  isInComparison(instance: Instance): boolean {
    return this.selectedInstances.includes(instance);
  }

  trackByName(index: number, instance: Instance): string {
    return instance.name;
  }

  onSort($event: SortEvent) {
    this.headers.forEach(header => {
        if (header.column !== $event.column)
          header.resetDirection();
      }
    );
    this.sort($event.column.toLowerCase(), $event.direction);
  }

  compare(v1: string | number, v2: string | number) {
    return v1 < v2 ? -1 : v1 > v2 ? 1 : 0;
  }

  sort(column: string, direction: SortDirection) {
    if (direction === '' || column === '') {
      direction = 'asc'
      column = 'name'
    }
    this.displayedInstances = this.displayedInstances.sort((a, b) => {
      // @ts-ignore
      const res = this.compare(a[column], b[column]);
      return direction === 'asc' ? res : -res;
    });

  }
}
