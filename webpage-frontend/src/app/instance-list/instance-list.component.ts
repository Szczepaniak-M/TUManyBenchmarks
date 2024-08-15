import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {BenchmarkDetails, BenchmarkStatistics, Instance, InstanceDefaultRow} from "./instance.model";
import {InstanceListService} from "./instance-list.service";
import {Filter} from "./list-filter/list-filter.model";
import {SortEvent} from "./list-sort/list-sort.model";
import {QueryProcessorService} from "./query-processor/query-processor.service";
import {forkJoin} from "rxjs";
import {groupByToMap, removeUnnecessaryTags} from "../common/instance/instance.utils";

@Component({
  selector: "app-instance-list",
  template: `
    <div class="container mx-auto my-2 border-2 rounded">
      <app-list-filter
        [allTags]="allTags"
        [allNetworks]="allNetworks"
        [allBenchmarks]="allBenchmarks"
        [selectedInstances]="selectedInstances.length"
        (filterChange)="applyFilters($event)"
        (redirectToComparison)="compareSelectedItems()">
      </app-list-filter>

      <div class="h-60">
        <app-monaco-editor></app-monaco-editor>
      </div>

      <div class="flex flex-col overflow-x-auto">
        <div class="w-auto min-w-max">
          <app-list-header
            [columns]="columns"
            (sort)="onSort($event)"/>
          <app-list-row
            *ngFor="let row of rows; trackBy trackById"
            [hidden]="row['hidden']"
            [row]="row"
            [columns]="columns"
            [isInComparison]="isInComparison(row['name'])"
            [onToggleComparison]="toggleComparison.bind(this)"
          />
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InstanceListComponent implements OnInit {
  defaultRows: InstanceDefaultRow[] = [];
  rows: { [index: string]: any }[] = [];
  selectedInstances: string[] = [];
  allTags: string[] = [];
  allNetworks: string[] = [];
  allBenchmarks: { name: string, id: string }[] = [];
  private defaultColumnsWithoutBenchmark = ["Name", "vCPUs", "Memory", "Network", "Tags"]
  columns: string[] = this.defaultColumnsWithoutBenchmark
  private defaultColumnsWithBenchmark = ["Name", "vCPUs", "Memory", "Network", "Minimum", "Average", "Median", "Maximum", "Tags"]

  constructor(private instanceListService: InstanceListService,
              private router: Router,
              private changeDetectorRef: ChangeDetectorRef,
              private queryProcessorService: QueryProcessorService) {
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

      this.queryProcessorService.initializeDuckDB()
        .then(() => this.queryProcessorService.loadDatabase())
    })
  }

  applyFilters(filter: Filter): void {
    const benchmarkSplit = filter.benchmark?.split("-")
    const benchmark = benchmarkSplit ? benchmarkSplit[0] : null;
    const series = benchmarkSplit ? benchmarkSplit[1] : null;
    const rowsToDisplay = this.defaultRows.map(instance => {
      const matchesName = filter.name ? instance.Name.toLowerCase().includes(filter.name.toLowerCase()) : true;
      const matchesMinCpu = filter.minCpu ? instance.vCPUs >= filter.minCpu : true;
      const matchesMaxCpu = filter.maxCpu ? instance.vCPUs <= filter.maxCpu : true;
      const matchesMinMemory = filter.minMemory ? instance.Memory >= filter.minMemory : true;
      const matchesMaxMemory = filter.maxMemory ? instance.Memory <= filter.maxMemory : true;
      const matchesNetwork = filter.network && filter.network.length ? filter.network.includes(instance.Network) : true;
      const matchesTags = filter.tags && filter.tags.length ? filter.tags.every(tag => instance.Tags.includes(tag)) : true;
      const matchesBenchmark = !!benchmark ? instance.benchmarks.map(stat => stat.benchmarkId).includes(benchmark) : true
      instance.hidden = !(matchesName && matchesMinCpu && matchesMaxCpu && matchesMinMemory && matchesMaxMemory && matchesNetwork && matchesTags && matchesBenchmark);
      return instance
    });
    if (filter.benchmark) {
      rowsToDisplay
        .filter(row => !row.hidden)
        .forEach(instance => {
          const stats = instance.benchmarks.find(stat => stat.benchmarkId === benchmark && stat.series === series)!
          instance.Minimum = Number(stats.min.toPrecision(5))
          instance.Average = Number(stats.avg.toPrecision(5))
          instance.Median = Number(stats.median.toPrecision(5))
          instance.Maximum = Number(stats.max.toPrecision(5))
        })
      this.columns = this.defaultColumnsWithBenchmark
    } else {
      this.columns = this.defaultColumnsWithoutBenchmark
    }
  }

  toggleComparison(object: { [index: string]: any }): boolean {
    const name = object["Name"] ?? object["name"]
    if (name && this.isInComparison(name)) {
      this.selectedInstances = this.selectedInstances.filter(item => item !== name);
      return false;
    } else if (this.selectedInstances.length < 3) {
      this.selectedInstances.push(name);
      return true;
    }
    return false;
  }

  goToDetails(instance: Instance): void {
    this.router.navigate(["/instance", instance.name]);
  }

  compareSelectedItems(): void {
    this.router.navigate(["/instance/compare"],
      {queryParams: {instances: this.selectedInstances.join(",")}});
  }

  isInComparison(name: string): boolean {
    return this.selectedInstances.includes(name);
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

  trackById(index: number, row: any): number {
    return row['id']
  }

  private getDefaultRows(instances: Instance[], statistics: BenchmarkStatistics[]) {
    const benchmarksMap = groupByToMap(statistics, x => x.instanceId)
    let counter = 0
    return instances.map(instance => removeUnnecessaryTags(instance))
      .map(instance => {
        return {
          id: counter++,
          Name: instance.name,
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
