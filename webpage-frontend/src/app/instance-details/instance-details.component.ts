import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {InstanceDetails} from "./instance-details.model";
import {InstanceDetailsService} from "./instance-details.service";

@Component({
  selector: 'app-instance-details',
  template: `
    <div *ngIf="instance">
      <h2>{{ instance.name }}</h2>
      <p>{{ instance.tags }}</p>
    </div>`,
})
export class InstanceDetailsComponent implements OnInit {
  instance: InstanceDetails = {
    id: '',
    name: '',
    tags: [],
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
}
