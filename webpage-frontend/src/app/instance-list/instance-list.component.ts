import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  ViewChild,
} from "@angular/core";
import {Router} from "@angular/router";
import {BenchmarkDetails, BenchmarkStatistics, Instance, InstanceDefaultRow} from "./instance.model";
import {InstanceListService} from "./instance-list.service";
import {Filter} from "./list-filter/list-filter.model";
import {SortEvent} from "./list-header/list-sort/list-sort.model";
import {forkJoin} from "rxjs";
import {groupByToMap} from "../common/instance/instance.utils";
import {ListQueryService} from "./list-query/list-query.service";
import {ListQueryComponent} from "./list-query/list-query.component";
import {ListHeaderComponent} from "./list-header/list-header.component";

@Component({
  selector: "app-instance-list",
  template: `
    <div class="container mx-auto my-2">
      <div class="flex flex-col items-center mb-2"
           [ngClass]="queryConsoleActive ? 'expanded' : ''">
        <app-list-query [filter]="filter"
                        [rows]="rows"
                        [columns]="columns"
                        [selectedInstances]="selectedInstances.size"
                        (redirectToComparison)="compareSelectedItems()"
                        (queryResult)="onQueryExecution($event)"
                        class="w-full console"/>
        <app-list-filter
          [allTags]="allTags"
          [allNetworks]="allNetworks"
          [allBenchmarks]="allBenchmarks"
          [selectedInstances]="selectedInstances.size"
          (filterChange)="applyFilters($event)"
          (redirectToComparison)="compareSelectedItems()"
          class="w-full filter">
        </app-list-filter>
        <div class="half-circle" (click)="onShowConsoleClick()">
          <div class="triangle">
          </div>
        </div>
      </div>

      <div class="flex flex-col overflow-x-auto border rounded">
        <div class="m-1 text-xl text-center">
          Found <b>{{ countRows() }}</b> instances meeting criteria
        </div>
        <div class="w-auto min-w-max">
          <app-list-header
            [columns]="columns"
            (sort)="onSort($event)"/>
          <app-list-row
            *ngFor="let row of rows; trackBy trackById"
            [hidden]="row['hidden']"
            [row]="row"
            [columns]="columns"
            [isInComparison]="isInComparison(row)"
            [onToggleComparison]="toggleComparison.bind(this)"
          />
        </div>
      </div>
    </div>
  `,
  styleUrls: ["./instance-list.component.css"],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InstanceListComponent implements OnInit {
  defaultRows: InstanceDefaultRow[] = [];
  rows: { [index: string]: any }[] = [];
  selectedInstances: Set<string> = new Set<string>();
  allTags: string[] = [];
  allNetworks: string[] = [];
  allBenchmarks: { name: string, id: string }[] = [];
  filter: Filter = {}
  queryConsoleActive: boolean = false
  @ViewChild(ListQueryComponent) queryComponent!: ListQueryComponent
  @ViewChild(ListHeaderComponent) header!: ListHeaderComponent;
  private defaultColumnsWithoutBenchmark = ["Name", "On-Demand Price [$/h]", "Spot Price [$/h]", "vCPUs", "Memory", "Network", "Tags"]
  columns: string[] = this.defaultColumnsWithoutBenchmark
  private defaultColumnsWithBenchmark = ["Name", "On-Demand Price [$/h]", "Spot Price [$/h]", "vCPUs", "Memory", "Network", "Minimum",
    "Average", "Median", "Maximum", "Tags"]

  constructor(private instanceListService: InstanceListService,
              private router: Router,
              private changeDetectorRef: ChangeDetectorRef,
              private listQueryService: ListQueryService) {
  }

  ngOnInit(): void {
    forkJoin({
      instances: this.instanceListService.getInstances(),
      benchmarks: this.instanceListService.getBenchmarks(),
      statistics: this.instanceListService.getStatistics()
    }).subscribe(({instances, benchmarks, statistics}) => {
      localStorage.setItem("instances", JSON.stringify(instances));
      localStorage.setItem("benchmarks", JSON.stringify(benchmarks));
      localStorage.setItem("statistics", JSON.stringify(statistics));

      this.defaultRows = this.getDefaultRows(instances, statistics);
      this.rows = this.defaultRows
      this.updateFilters(benchmarks);
      this.changeDetectorRef.markForCheck();
      this.listQueryService.initializeDuckDB()
        .then(() => this.listQueryService.loadDatabase())
    })
  }

  applyFilters(filter: Filter): void {
    this.filter = Object.assign({}, filter)
    const benchmarkSplit = filter.benchmark?.split("-")
    const benchmark = benchmarkSplit ? benchmarkSplit[0] : null;
    const series = benchmarkSplit ? benchmarkSplit[1] : null;
    const rowsToDisplay = this.defaultRows.map(instance => {
      const matchesName = filter.name ? instance.Name.toLowerCase().includes(filter.name.toLowerCase()) : true;
      const matchesMinOnDemandPrice = filter.minOnDemandPrice ? instance["On-Demand Price [$/h]"] >= filter.minOnDemandPrice : true;
      const matchesMaxOnDemandPrice = filter.maxOnDemandPrice ? instance["On-Demand Price [$/h]"] <= filter.maxOnDemandPrice : true;
      const matchesMinSpotPrice = filter.minSpotPrice ? instance["Spot Price [$/h]"] >= filter.minSpotPrice : true;
      const matchesMaxSpotPrice = filter.maxSpotPrice ? instance["Spot Price [$/h]"] <= filter.maxSpotPrice : true;
      const matchesMinCpu = filter.minCpu ? instance.vCPUs >= filter.minCpu : true;
      const matchesMaxCpu = filter.maxCpu ? instance.vCPUs <= filter.maxCpu : true;
      const matchesMinMemory = filter.minMemory ? instance.Memory >= filter.minMemory : true;
      const matchesMaxMemory = filter.maxMemory ? instance.Memory <= filter.maxMemory : true;
      const matchesNetwork = filter.network && filter.network.length ? filter.network.includes(instance.Network) : true;
      const matchesTagsAnd = filter.tagsAll && filter.tagsAll.length ? filter.tagsAll.every(tag => instance.Tags.includes(tag)) : true;
      const matchesTagsOr = filter.tagsAny && filter.tagsAny.length ? filter.tagsAny.some(tag => instance.Tags.includes(tag)) : true;
      const matchesBenchmark = !!benchmark ? instance.benchmarks.map(stat => stat.benchmarkId).includes(benchmark) : true
      instance.hidden = !(matchesName && matchesMinOnDemandPrice && matchesMaxOnDemandPrice
        && matchesMinSpotPrice && matchesMaxSpotPrice && matchesMinCpu && matchesMaxCpu && matchesMinMemory
        && matchesMaxMemory && matchesNetwork && matchesTagsAnd && matchesTagsOr && matchesBenchmark);
      return instance
    });
    if (filter.benchmark) {
      rowsToDisplay
        .filter(row => !row.hidden)
        .forEach(instance => {
          const stats = instance.benchmarks.find(stat => stat.benchmarkId === benchmark && stat.series === series)!
          instance.Minimum = stats.min
          instance.Average = stats.avg
          instance.Median = stats.median
          instance.Maximum = stats.max
        })
      this.columns = this.defaultColumnsWithBenchmark
    } else {
      this.columns = this.defaultColumnsWithoutBenchmark
    }
  }

  compareSelectedItems(): void {
    this.router.navigate(["/instance/compare"],
      {queryParams: {instances: Array.from(this.selectedInstances).join(",")}});
  }

  onShowConsoleClick() {
    this.queryConsoleActive = !this.queryConsoleActive;
    if (!this.queryConsoleActive) {
      this.rows = this.defaultRows;
      this.applyFilters(this.filter)
      this.queryComponent.error = undefined
    }
  }

  onSort($event: SortEvent) {
    if ($event.direction === "" || $event.column === "") {
      $event.direction = "asc";
      $event.column = "Name";
    }
    this.rows = this.rows.sort((a, b) => {
      // @ts-ignore
      const res = this.compare(a[$event.column], b[$event.column]);
      return $event.direction === "asc" ? res : -res;
    });
  }

  onQueryExecution($event: { rows: { [p: string]: any }[]; columns: string[] }) {
    this.columns = $event.columns
    this.rows = $event.rows
    this.header.resetSort()
  }

  toggleComparison(object: { [index: string]: any }): boolean {
    const name = object["Name"] ?? object["name"]
    if (name && this.isInComparison(object)) {
      this.selectedInstances.delete(name);
      return false;
    } else if (this.selectedInstances.size < 3) {
      this.selectedInstances.add(name);
      return true;
    }
    return false;
  }

  isInComparison(row: { [index: string]: any }): boolean {
    const name = row['name'] ?? row['Name'] ?? row['NAME'];
    return this.selectedInstances.has(name);
  }

  trackById(index: number, row: any): number {
    return row['id']
  }

  countRows(): number {
    return this.rows.filter(row => !row['hidden']).length;
  }

  private getDefaultRows(instances: Instance[], statistics: BenchmarkStatistics[]) {
    const benchmarksMap = groupByToMap(statistics, x => x.instanceId)
    let counter = 0
    return instances.map(instance => {
      return {
        id: counter++,
        Name: instance.name,
        "On-Demand Price [$/h]": instance.onDemandPrice,
        "Spot Price [$/h]": instance.spotPrice,
        vCPUs: instance.vcpu,
        Memory: instance.memory,
        Network: instance.network,
        Tags: instance.tags,
        benchmarks: benchmarksMap.get(instance.id) ?? [],
        hidden: false
      } as InstanceDefaultRow
    })
      .sort((a, b) => a.Name.localeCompare(b.Name));
  }

  private updateFilters(benchmarks: BenchmarkDetails[]): void {
    const tagsSet = new Set<string>();
    const networksSet = new Set<string>();

    this.defaultRows.forEach(instance => {
      instance.Tags.forEach(tag => tagsSet.add(tag));
      networksSet.add(instance.Network);
    });

    this.allTags = Array.from(tagsSet);
    this.allNetworks = Array.from(networksSet);

    this.allBenchmarks = benchmarks.flatMap(benchmark => benchmark.seriesX.concat(benchmark.seriesY)
      .filter(series => series !== 'increasingValues')
      .map(series => {
        return {name: `${benchmark.name} - ${series}`, id: `${benchmark.id}-${series}`}
      }))
  }

  private compare(v1: string | number, v2: string | number) {
    return v1 < v2 ? -1 : v1 > v2 ? 1 : 0;
  }
}
