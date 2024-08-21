import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

@Component({
  selector: "app-list-row",
  template: `
    <div class="flex flex-row overflow-visible border"
         (click)="toggleComparison()"
         [class.bg-gray-200]="isInComparison">
      <div *ngFor="let field of columns" class="py-2 px-4 my-1 min-w-40 w-1/4 max-w-lg break-all">
        <div *ngIf="field.toLowerCase() === 'name'"
             class="text-blue-500 hover:underline"
             [routerLink]="['/instance', row[field]]">
          <p>{{ row[field] }}</p>
        </div>
        <div *ngIf="field.toLowerCase() === 'tags'">
          <span *ngFor="let tag of row[field]"
                class="inline-block bg-gray-400 rounded-full px-3 py-1 text-sm font-semibold text-gray-800 mr-2 my-1">
          {{ tag }}
        </span>
        </div>
        <div *ngIf="field.toLowerCase() !== 'name' && field.toLowerCase() !== 'tags'">
          {{ row[field] }}
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListRowComponent {
  @Input({required: true}) row!: {[index: string]:any};
  @Input({required: true}) columns!: string[];
  @Input({required: true}) isInComparison!: boolean;
  @Input({required: true}) onToggleComparison!: (obj: Object) => boolean;

  toggleComparison(): void {
    this.isInComparison = this.onToggleComparison(this.row);
  }
}
