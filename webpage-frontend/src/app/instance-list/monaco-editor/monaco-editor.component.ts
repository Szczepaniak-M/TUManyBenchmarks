import {Component} from "@angular/core";

@Component({
  selector: "app-monaco-editor",
  template: `
    <ngx-monaco-editor style="height: 100%"
                       [(ngModel)]="code"
                       [options]="editorOptions"
                       (onInit)="onEditorInit($event)"/>
  `
})
export class MonacoEditorComponent {
  editorOptions = {
    theme: "vs-dark",
    language: "pgsql",
    automaticLayout: true,
    scrollBeyondLastLine: false
  };
  code: string = "[\n  \n]";
  editor: any;

  onEditorInit(editor: any) {
    this.editor = editor;
  }
}
