import {Component} from '@angular/core';
import {InstanceDetailsService} from "./instance-explorer.service";

@Component({
  selector: 'app-instance-explorer',
  template: `
    <div class="container mx-auto p-4 flex flex-row">
      <div class="p-2 w-1/2">
        <app-monaco-editor/>
        <button (click)="executeQuery()">Execute</button>
      </div>

      <div class="flex flex-col p-2 w-1/2">
        <button (click)="downloadJson()">Download JSON</button>

        <app-json-viewer
          class="border p-2"
          *ngFor="let result of results"
          [inputJson]="result"
        />
        <div *ngIf="error"></div>
      </div>

    </div>
  `
})
export class InstanceExplorerComponent {
  results: any[] = [];
  partialResults: boolean = false;
  error: string | undefined

  constructor(private instanceDetailsService: InstanceDetailsService) {
  }

  executeQuery() {
    const stages = [""]
    this.instanceDetailsService.executeQuery(stages, this.partialResults).subscribe(response => {
      this.results = response.results
      this.error = response.error
      });

  }

  downloadJson() {
    const jsonStr = JSON.stringify(this.results[this.results.length - 1], null, 2);
    const blob = new Blob([jsonStr], {type: 'application/json'});
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = 'data.json';
    a.click();

    window.URL.revokeObjectURL(url);
    a.remove();
  }


}

