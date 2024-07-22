import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input} from '@angular/core';
import {Instance} from "../instance.model";

@Component({
  selector: 'app-instance-list-row',
  template: `
    <div class="flex flex-row border" (click)="toggleComparison()" [class.bg-gray-200]="isInComparison">
      <div class="py-2 px-4 w-1/6 text-blue-500 hover:underline"
           [routerLink]="['/instance', instance.name]">
        {{ instance.name }}
      </div>
      <div class="py-2 px-4 w-1/12">{{ instance.vCpu }}</div>
      <div class="py-2 px-4 w-1/6">{{ instance.memory }}</div>
      <div class="py-2 px-4 w-1/6">{{ instance.network }}</div>
      <div class="py-2 px-4 w-1/4">
        <span *ngFor="let tag of instance.otherTags"
              class="inline-block bg-gray-400 rounded-full px-3 py-1 text-sm font-semibold text-gray-800 mr-2 mb-2">
          {{ tag }}
        </span>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InstanceListRowComponent {
  @Input() instance: Instance = {
    id: "",
    name: "",
    vCpu: "",
    memory: "",
    network: "",
    otherTags: [],
  };
  @Input() isInComparison: boolean = true;
  @Input() onToggleComparison: (obj: Instance) => boolean = ()=> false;

  constructor(private changeDetectorRef: ChangeDetectorRef) {
  }

  toggleComparison(): void {
    this.isInComparison = this.onToggleComparison(this.instance);
    // this.changeDetectorRef.markForCheck()
  }

}
