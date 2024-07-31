import {ChangeDetectionStrategy, ChangeDetectorRef, Component, ViewChild} from "@angular/core";
import {InstanceDetailsService} from "./instance-explorer.service";
import {MonacoEditorComponent} from "./monaco-editor/monaco-editor.component";
import {MatSlideToggleChange} from "@angular/material/slide-toggle";

@Component({
  selector: "app-instance-explorer",
  template: `
    <div class="container mx-auto my-4 border rounded flex flex-row">
      <div class="p-2 w-1/2">
        <div class="flex flex-row justify-between items-center h-14">
          <mat-slide-toggle (change)="onToggleChange($event)">
            <p class="font-bold text-xl">Show partial results</p>
          </mat-slide-toggle>
          <div class="w-1/2 flex flex-row">
            <button class="m-1 bg-gray-800 text-white w-1/2 p-2 rounded disabled:bg-slate-500"
                    (click)="executeQuery()"
                    [disabled]="!isContentValid">
              Execute
            </button>
            <button class="m-1 bg-gray-800 text-white w-1/2 p-2 rounded disabled:bg-slate-500"
                    (click)="downloadJson()"
                    [disabled]="results.length == 0 || error">
              Download JSON
            </button>
          </div>
        </div>
        <div class="h-80-vh">
          <app-monaco-editor (isContentValid)="onContentValid($event)"/>
        </div>
      </div>
      <div class="flex flex-col p-2 w-1/2 ">
        <div class="h-14 flex items-center">
          <div class="text-2xl font-semibold">Results</div>
        </div>
        <app-json-viewer
          class="border rounded p-2 mb-2"
          *ngFor="let result of results"
          [inputJson]="result"
        />
        <div *ngIf="error" class="p-4 bg-red-100 border-2 rounded border-red-400 text-red-700">
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
  results: any[] = [];
  error: string | undefined;
  @ViewChild(MonacoEditorComponent) editor!: MonacoEditorComponent;

  constructor(private instanceDetailsService: InstanceDetailsService, private changeDetectorRef: ChangeDetectorRef) {
  }

  executeQuery() {
    const query = this.editor.code;
    const stagesAsJson = JSON.parse(query);
    const stagesAsStrings = stagesAsJson.map((stage: any) => JSON.stringify(stage));
    this.instanceDetailsService.executeQuery(stagesAsStrings, this.partialResults).subscribe(response => {
      this.results = response.results.map(result => JSON.parse(result));
      this.error = response.error;
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

  onToggleChange($event: MatSlideToggleChange) {
    this.partialResults = $event.checked;
  }
}
