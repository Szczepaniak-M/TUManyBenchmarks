import {Component, EventEmitter, Output} from "@angular/core";

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
  @Output() isContentValid = new EventEmitter<boolean>();
  editorOptions = {
    theme: "vs-dark",
    language: "json",
    automaticLayout: true,
    scrollBeyondLastLine: false
  };
  code: string = "[\n  \n]";
  editor: any;

  onEditorInit(editor: any) {
    this.editor = editor;
    this.editor.onDidChangeModelContent(() => {
      this.checkForErrors();
    });
  }

  checkForErrors() {
    try {
      const parsed = JSON.parse(this.code);
      this.isContentValid.emit(Array.isArray(parsed) && parsed.length > 0)
    } catch (e) {
      this.isContentValid.emit(false)
    }
  }

}
