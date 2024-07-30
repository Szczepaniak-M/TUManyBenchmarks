import {ChangeDetectionStrategy, ChangeDetectorRef, Component, ViewChild} from "@angular/core";
import {InstanceDetailsService} from "./instance-explorer.service";
import {MonacoEditorComponent} from "./monaco-editor/monaco-editor.component";
import {MatSlideToggleChange} from "@angular/material/slide-toggle";

@Component({
  selector: "app-instance-explorer",
  template: `
    <div class="container mx-auto my-4 border rounded flex flex-row">
      <div class="p-2 w-1/2 h-85-vh">
        <app-monaco-editor (isContentValid)="onContentValid($event)"/>

      </div>

      <div class="flex flex-col p-2 w-1/2 ">
        <div class="flex flex-row">
          <mat-slide-toggle labelPosition="before" (change)="onToogleChange($event)">
            <p class="font-bold">Show partial results</p>
          </mat-slide-toggle>
          <button (click)="executeQuery()"
                  [disabled]="!isContentValid"
                  class="m-1 bg-gray-800 text-white w-1/4 p-2 rounded disabled:bg-slate-500">
            Execute
          </button>
          <button class="m-1 bg-gray-800 text-white w-1/4 p-2 rounded disabled:bg-slate-500"
                  (click)="downloadJson()"
                  [disabled]="results.length == 0">
            Download JSON
          </button>
        </div>
        <h1>Results</h1>
        <app-json-viewer
          class="border p-2 mt-2"
          *ngFor="let result of results"
          [inputJson]="result"
        />
        <div *ngIf="error" class="p-4 mt-2 bg-red-100 border rounded border-red-400 text-red-700">
          {{ error }}
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush

})
export class InstanceExplorerComponent {
  isContentValid: boolean = false;
  partialResults: boolean = false;
  results: any[] = [{"a":"a"}];
  error: string | undefined = "lorem ipsum"
  @ViewChild(MonacoEditorComponent) editor!: MonacoEditorComponent;

  constructor(private instanceDetailsService: InstanceDetailsService, private changeDetectorRef: ChangeDetectorRef) {
  }

  executeQuery() {
    const query = this.editor.code
    const stagesAsJson = JSON.parse(query);
    const stagesAsStrings = stagesAsJson.map((stage: any) => JSON.stringify(stage));
    this.instanceDetailsService.executeQuery(stagesAsStrings, this.partialResults).subscribe(response => {
      this.results = response.results.map(result => JSON.parse(result))
      this.error = response.error
      this.changeDetectorRef.markForCheck();
    });
  }

  downloadJson() {
    const jsonStr = JSON.stringify(this.results[this.results.length - 1], null, 2);
    const blob = new Blob([jsonStr], {type: "application/json"});
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement("a");
    a.href = url;
    a.download = "data.json";
    a.click();

    window.URL.revokeObjectURL(url);
    a.remove();
  }


  onContentValid($event: boolean) {
    this.isContentValid = $event;
    this.changeDetectorRef.markForCheck();
  }

  onToogleChange($event: MatSlideToggleChange) {
    this.partialResults = $event.checked
  }
}

// [
//   { "$match": { "name": "t2.micro" } },
//   { "$project": { "name": 1 } }
// ]

