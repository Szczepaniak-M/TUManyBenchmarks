import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, QueryList, ViewChildren} from "@angular/core";
import {SortEvent} from "./list-sort/list-sort.model";
import {ListSortComponent} from "./list-sort/list-sort.component";

@Component({
  selector: "app-list-header",
  template: `
    <div class="flex flex-row items-center border bg-gray-100">
      <div *ngFor="let column of columns" class="py-2 px-4 text-left font-bold min-w-40 w-1/4 max-w-lg">
        <app-list-sort
          *ngIf="column !== 'Tags'"
          [column]="column"
          (sort)="onSort($event)">
        </app-list-sort>
        <div *ngIf="column === 'Tags'" class="flex items-center">
          <p class="text-left font-bold">Tags</p>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListHeaderComponent {
  @Input({required: true}) columns!: string[];
  @Output() sort = new EventEmitter<SortEvent>();
  @ViewChildren(ListSortComponent) headers!: QueryList<ListSortComponent>;

  onSort($event: SortEvent) {
    this.headers.forEach(header => {
        if (header.column !== $event.column)
          header.resetDirection();
      }
    );
    this.sort.emit($event);
  }
}
