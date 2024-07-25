import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Instance} from "./instance.model";
import {InstanceListService} from "./instance-list.service";

@Component({
  selector: 'app-instance-list',
  template: `
    <div class="container mx-auto p-4">
      <app-instance-list-filter
        [allTags]="allTags"
        [allVCPUs]="allVCPUs"
        [allNetworks]="allNetworks"
        [allRAMs]="allRAMs"
        (filterChange)="applyFilters($event)"
        (redirectToComparison)="compareSelectedItems()">
      </app-instance-list-filter>

      <div class="flex flex-col">
        <div class="flex flex-row border bg-gray-100">
          <div class="py-2 px-4 w-1/6 text-left font-bold">Name</div>
          <div class="py-2 px-4 w-1/12 text-left font-bold">vCPU</div>
          <div class="py-2 px-4 w-1/6 text-left font-bold">RAM</div>
          <div class="py-2 px-4 w-1/6 text-left font-bold">Network</div>
          <div class="py-2 px-4 w-1/4 text-left font-bold">Tags</div>
        </div>
        <app-instance-list-row
          *ngFor="let instance of displayedInstances; trackBy: trackById"
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
  instances: Instance[] = [];
  displayedInstances: Instance[] = [];
  selectedInstances: Instance[] = [];
  selectedFilters = {
    name: '',
    vCPU: [] as string[],
    network: [] as string[],
    RAM: [] as string[],
    tags: [] as string[],
  };

  allTags: string[] = [];
  allVCPUs: string[] = [];
  allNetworks: string[] = [];
  allRAMs: string[] = [];

  constructor(private instanceListService: InstanceListService,
              private router: Router,
              private changeDetectorRef: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.instanceListService.getInstances().subscribe(response => {
      this.instances = response;
      this.displayedInstances = response;
      this.updateFilters()
      this.changeDetectorRef.markForCheck();
    });
  }

  updateFilters(): void {
    const tagsSet = new Set<string>();
    const vCPUsSet = new Set<string>();
    const networksSet = new Set<string>();
    const RAMsSet = new Set<string>();

    this.instances.forEach(obj => {
      obj.otherTags.forEach(tag => tagsSet.add(tag));
      vCPUsSet.add(obj.vCpu);
      networksSet.add(obj.network);
      RAMsSet.add(obj.memory);
    });

    this.allTags = Array.from(tagsSet);
    this.allVCPUs = Array.from(vCPUsSet);
    this.allNetworks = Array.from(networksSet);
    this.allRAMs = Array.from(RAMsSet);
  }

  applyFilters(filters: any): void {
    this.displayedInstances = this.instances.filter(obj => {
      const matchesName = filters.name ? obj.name.toLowerCase().includes(filters.name.toLowerCase()) : true;
      const matchesVCPU = filters.vCPU.length ? filters.vCPU.includes(obj.vCpu) : true;
      const matchesNetwork = filters.network.length ? filters.network.includes(obj.network) : true;
      const matchesRAM = filters.RAM.length ? filters.RAM.includes(obj.memory) : true;
      const matchesTags = filters.tags.length ? filters.tags.some((tag: string) => obj.otherTags.includes(tag)) : true;

      return matchesName && matchesVCPU && matchesNetwork && matchesRAM && matchesTags;
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

  trackById(index: number, instance: Instance): string {
    return instance.name;
  }
}
