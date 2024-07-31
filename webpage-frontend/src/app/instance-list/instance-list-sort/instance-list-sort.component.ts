import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from "@angular/core";
import {SortDirection, SortEvent} from "./instance-list-sort.model";


@Component({
  selector: "app-instance-list-sort",
  template: `
    <div class="flex justify-between" (click)="rotate()">
      <div class="flex items-center">
        <p class="text-left font-bold">{{ column }}</p>
      </div>
      <div class="text-center center items-center">
        <svg viewBox="6 8 12 1" width="12px" height="12px" focusable="false" aria-hidden="true">
          <path fill="#212529" d="M7 10l5 5 5-5z" transform="rotate(180, 12, 12)"
                [attr.opacity]="direction == 'asc' ? '0.6' : '0.125'"/>
        </svg>
        <svg viewBox="6 8 12 12" width="12px" height="12px" focusable="false" aria-hidden="true">
          <path fill="#212529" d="M7 10l5 5 5-5z"
                [attr.opacity]="direction == 'desc' ? '0.6' : '0.125'"/>
        </svg>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class InstanceListSortComponent {
  @Input({required: true}) column!: string;
  @Output() sort = new EventEmitter<SortEvent>();
  direction: SortDirection = "";

  private rotateMap: { [key: string]: SortDirection } = {"asc": "desc", "desc": "", "": "asc"};

  constructor(private changeDetectorRef: ChangeDetectorRef) {
  }

  rotate() {
    this.direction = this.rotateMap[this.direction];
    this.sort.emit({column: this.column, direction: this.direction});
  }

  resetDirection() {
    this.direction = "";
    this.changeDetectorRef.markForCheck();
  }
}
