import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {InstanceDetails} from "../instance-details/instance-details.model";
import {InstanceDetailsService} from "../instance-details/instance-details.service";

@Component({
  selector: 'app-compare-instances',
  template: `
    <div *ngIf="instances.length">
      <h2>Comparison of Selected Items</h2>
      <table>
        <thead>
        <tr>
          <th>Property</th>
          <th *ngFor="let item of instances">{{ item.name }}</th>
        </tr>
        </thead>
        <tbody>>
        </tbody>
      </table>
    </div>`
})
export class CompareInstancesComponent implements OnInit {
  instances: InstanceDetails[] = [];

  constructor(private route: ActivatedRoute, private instanceDetailsService: InstanceDetailsService) {
  }

  ngOnInit(): void {
    const names = this.route.snapshot.queryParamMap.get('instances')!.split(',');
    names.forEach(name => {
      this.instanceDetailsService.getInstanceDetails(name).subscribe(data => {
        this.instances.push(data);
      });
    });
  }
}
