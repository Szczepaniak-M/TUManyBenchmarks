import {Component, OnInit} from "@angular/core";
import {ActivatedRoute} from "@angular/router";
import {InstanceDetailsService} from "./instance-details.service";
import {Instance} from "../instance-list/instance.model";

@Component({
  selector: "app-instance-details",
  template: `
    <div *ngIf="instance" class="container mx-auto p-4 my-2 border-2 rounded">
      <h1 class="text-3xl font-bold mb-2">{{ instance.name }}</h1>
      <div class="mb-2">
        <h2 class="text-2xl font-semibold">Instance Details</h2>
        <div class="text-gray-700">
          <p><strong class="text-black">On-Demand Price: </strong>\${{ instance.onDemandPrice }} hourly</p>
          <p><strong class="text-black">Spot Price: </strong>\${{ instance.spotPrice }} hourly</p>
          <p><strong class="text-black">vCPU: </strong>{{ instance.vcpu }}</p>
          <p><strong class="text-black">Network: </strong>{{ instance.network }}</p>
          <p><strong class="text-black">Memory: </strong>{{ instance.memory }} GiB</p>
          <p><strong class="text-black">Tags: </strong>
            <span *ngFor="let tag of instance.tags"
                  class="inline-block bg-gray-400 rounded-full px-3 py-1 text-sm font-semibold text-gray-800 mr-2 my-1">
                {{ tag }}
            </span>
          </p>
        </div>
      </div>

      <div class="mb-4">
        <h2 class="text-2xl font-semibold">Benchmarks</h2>
        <div *ngFor="let benchmark of instance.benchmarks"
             class="mb-2 border-2 rounded p-2 text-gray-700">
          <h3 class="text-lg font-medium">{{ benchmark.name }}</h3>
          <p class="text font-medium">{{ benchmark.description }}</p>
          <div *ngFor="let plot of benchmark.plots" class="mb-2">
            <app-benchmark-plot [benchmarkResults]="[benchmark.results]" [plot]="plot"/>
          </div>
        </div>
      </div>

    </div>
    <div *ngIf="!instance" class="container mx-auto p-4">
      <p>Instance details not found.</p>
    </div>
  `
})
export class InstanceDetailsComponent implements OnInit {
  instance: Instance = {
    id: "",
    name: "",
    onDemandPrice: 0,
    spotPrice: 0,
    vcpu: 0,
    memory: 0,
    network: "",
    tags: [],
    benchmarks: []
  };

  constructor(private route: ActivatedRoute, private instanceDetailsService: InstanceDetailsService) {
  }

  ngOnInit(): void {
    const instanceName = this.route.snapshot.paramMap.get("instance")!;
    this.instanceDetailsService.getInstanceDetails(instanceName).subscribe(data => {
      this.instance = data;
    });
  }
}
