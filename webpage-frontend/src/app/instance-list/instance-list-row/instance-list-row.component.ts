import {ChangeDetectionStrategy, Component, Input} from "@angular/core";
import {Instance} from "../instance.model";

@Component({
  selector: "app-instance-list-row",
  template: `
    <div class="flex flex-row border"
         (click)="toggleComparison()"
         [class.bg-gray-200]="isInComparison">
      <div class="py-2 px-4 my-1 w-1/6 text-blue-500 hover:underline"
           [routerLink]="['/instance', instance.name]">
        {{ instance.name }}
      </div>
      <div class="py-2 px-4 my-1 w-1/6">{{ instance.vcpu }} vCPUs</div>
      <div class="py-2 px-4 my-1 w-1/6">{{ instance.memory }} GiB</div>
      <div class="py-2 px-4 my-1 w-1/6">{{ instance.network }}</div>
      <div class="px-4 my-2 w-1/3">
        <span *ngFor="let tag of instance.otherTags"
              class="inline-block bg-gray-400 rounded-full px-3 py-1 text-sm font-semibold text-gray-800 mr-2 my-1">
          {{ tag }}
        </span>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InstanceListRowComponent {
  @Input({required: true}) instance!: Instance;
  @Input({required: true}) isInComparison!: boolean;
  @Input({required: true}) onToggleComparison!: (obj: Instance) => boolean;

  toggleComparison(): void {
    this.isInComparison = this.onToggleComparison(this.instance);
  }
}
