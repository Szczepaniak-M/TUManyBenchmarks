import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Instance} from "./instance.model";
import {InstanceListService} from "./instance-list.service";

@Component({
  selector: 'app-instance-list',
  template: `
    <div>
      <div>
        <button (click)="compareSelectedItems()" [disabled]="selectedInstances.length < 2">Compare Selected Items
        </button>
      </div>
      <table>
        <thead>
        <tr>
          <th>Name</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr *ngFor="let item of instances">
          <td (click)="goToDetails(item)">{{ item.name }}</td>
          <td>
            <button (click)="onSelectItem(item)">
              {{ selectedInstances.includes(item) ? 'Deselect' : 'Select' }}
            </button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>`
})
export class InstanceListComponent implements OnInit {
  instances: Instance[] = [];
  selectedInstances: Instance[] = [];

  constructor(private instanceListService: InstanceListService, private router: Router) {
  }

  ngOnInit(): void {
    this.instanceListService.getInstances().subscribe(response => {
      this.instances = response;
    });
  }

  onSelectItem(item: any): void {
    if (this.selectedInstances.includes(item)) {
      this.selectedInstances = this.selectedInstances.filter(i => i !== item);
    } else {
      if (this.selectedInstances.length < 3) {
        this.selectedInstances.push(item);
      }
    }
  }

  goToDetails(item: Instance): void {
    this.router.navigate(['/instance', item.name]).then();
  }

  compareSelectedItems(): void {
    const selectedNames = this.selectedInstances.map(item => item.name);
    this.router.navigate(['/instance/compare'],
      {queryParams: {instances: selectedNames.join(',')}})
      .then();
  }
}
