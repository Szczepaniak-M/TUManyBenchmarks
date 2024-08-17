import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output} from "@angular/core";
import {SortDirection, SortEvent} from "./list-sort.model";


@Component({
  selector: "app-list-sort",
  template: `
    <div class="flex justify-between" (click)="rotate()">
      <div class="flex items-center">
        <p class="text-left font-bold">{{ column }}</p>
      </div>
      <div class="text-center center items-center">
        <div class="triangle-up mb-1" [style.opacity]="direction == 'asc' ? '0.6' : '0.125'"></div>
        <div class="triangle-down" [style.opacity]="direction == 'desc' ? '0.6' : '0.125'"></div>
      </div>
    </div>
  `,
  styleUrls: ['./list-sort.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListSortComponent {
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
