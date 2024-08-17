import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild
} from "@angular/core";
import {ListQueryService} from "./list-query.service";
import {Filter} from "../list-filter/list-filter.model";
import {MonacoEditorComponent} from "./monaco-editor/monaco-editor.component";

@Component({
  selector: "app-list-query",
  template: `
    <div class="p-2 border border-gray-300 rounded flex flex-col">
      <div class="flex flex-row">
        <button class="m-1 bg-gray-800 text-white w-1/2 p-2 rounded disabled:bg-slate-500"
                (click)="executeQuery()">
          Execute
        </button>
        <button class="m-1 bg-gray-800 text-white w-1/2 p-2 rounded disabled:bg-slate-500"
                (click)="downloadCsv()"
                [disabled]="error || isRowsEmpty()">
          Download CSV
        </button>
        <button class=" m-1 bg-gray-800 text-white w-1/2 p-2 rounded disabled:bg-slate-500"
                (click)="onRedirectToComparison()"
                [disabled]="selectedInstances < 2">
          <p>Compare ({{ selectedInstances }} / 3)</p>
        </button>
      </div>
      <div class="h-60">
        <app-monaco-editor [query]="query"/>
      </div>
      <div *ngIf="error" class="p-4 bg-red-100 border-2 rounded border-red-400 text-red-700">
        <pre *ngFor="let line of error">{{ line }}</pre>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListQueryComponent implements OnChanges {
  @Input() filter: Filter = {};
  @Input({required: true}) selectedInstances!: number;
  @Input() rows: { [index: string]: any }[] = [];
  @Input() columns: string[] = [];
  @Output() queryResult = new EventEmitter<{ rows: { [index: string]: any }[], columns: string[] }>();
  @Output() redirectToComparison = new EventEmitter<void>();
  @ViewChild(MonacoEditorComponent) editor!: MonacoEditorComponent;

  query: string = "";
  error: string[] | undefined;

  constructor(private listQueryService: ListQueryService,
              private changeDetectorRef: ChangeDetectorRef) {
    this.query = this.listQueryService.transformFilterToQuery({});
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes["filter"]) {
      this.query = this.listQueryService.transformFilterToQuery(this.filter);
    }
  }

  onRedirectToComparison(): void {
    this.redirectToComparison.emit();
  }

  executeQuery() {
    this.query = this.editor.query;
    this.listQueryService.executeQuery(this.query)
      .then(response => {
        if (response.error) {
          this.error = response.error;
        } else {
          this.error = undefined;
          this.queryResult.emit(response);
        }
        this.changeDetectorRef.detectChanges();
      });
  }

  downloadCsv() {
    const csvStr = this.jsonToCsv(this.rows, this.columns);
    const blob = new Blob([csvStr], {type: "text/csv"});
    window.URL.createObjectURL(blob);
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = "data.csv";
    a.click();

    window.URL.revokeObjectURL(url);
    a.remove();
  }

  isRowsEmpty(): boolean {
    if (this.rows.length != 0) {
      return this.rows.filter(row => !this.isRowHidden(row, this.columns)).length == 0;
    }
    return true
  }

  isRowHidden(row: { [index: string]: any }, columns: string[]) {
    return 'hidden' in row && !('hidden' in columns) && row["hidden"] == true;
  }

  private jsonToCsv(jsonData: { [index: string]: any }[], columns: string[]) {
    let csv = "";
    csv += columns.join(";") + "\n";
    jsonData.filter(row => !this.isRowHidden(row, columns))
      .forEach(obj => {
        const values = columns.map(header => obj[header]);
        csv += values.join(";") + "\n";
      });
    return csv;
  }
}
