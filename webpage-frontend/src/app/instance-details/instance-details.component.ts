import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {InstanceDetails} from "./instance-details.model";
import {InstanceDetailsService} from "./instance-details.service";

@Component({
  selector: 'app-instance-details',
  template: `
    <div *ngIf="instance" class="container mx-auto p-4">
      <h1 class="text-2xl font-bold mb-4">{{ instance.name }}</h1>

      <div class="mb-4">
        <h2 class="text-xl font-semibold">Instance Details</h2>
        <p><strong>vCPU:</strong> {{ instance.vcpu }}</p>
        <p><strong>Network:</strong> {{ instance.network }}</p>
        <p><strong>Memory:</strong> {{ instance.memory }}</p>
        <p><strong>Tags:</strong> {{ instance.otherTags.join(', ') }}</p>
      </div>

      <div class="mb-4">
        <h2 class="text-xl font-semibold">Benchmarks</h2>
        <div *ngFor="let benchmark of instance.benchmarks" class="mb-2">
          <h3 class="text-lg font-medium">{{ benchmark.name }}</h3>
          <p>{{ benchmark.description }}</p>
          <ul>
            <li *ngFor="let result of benchmark.results">
              <strong>{{ result.timestamp }}:</strong> {{ JSON.stringify(result.values) }}
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div *ngIf="!instance" class="container mx-auto p-4">
      <p>Instance details not found.</p>
    </div>

    <app-benchmark-plot/>
  `
})
export class InstanceDetailsComponent implements OnInit {
  instance: InstanceDetails = {
    id: '',
    name: '',
    vcpu: 0,
    memory: 0,
    network: '',
    otherTags: [],
    benchmarks: []
  };

  constructor(private route: ActivatedRoute, private instanceDetailsService: InstanceDetailsService) {
  }

  ngOnInit(): void {
    const instanceName = this.route.snapshot.paramMap.get('instance')!;
    this.instanceDetailsService.getInstanceDetails(instanceName).subscribe(data => {
      this.instance = data;
    });
  }

  protected readonly JSON = JSON;
}
