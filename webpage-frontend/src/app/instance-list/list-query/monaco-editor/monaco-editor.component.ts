import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

@Component({
  selector: "app-monaco-editor",
  template: `
    <ngx-monaco-editor style="height: 100%"
                       [(ngModel)]="query"
                       [options]="editorOptions"/>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MonacoEditorComponent {
  @Input() query: string = "";
  editorOptions = {
    theme: "vs-dark",
    language: "pgsql",
    automaticLayout: true,
    scrollBeyondLastLine: false,
    minimap: {
      enabled: false
    }
  };
}
