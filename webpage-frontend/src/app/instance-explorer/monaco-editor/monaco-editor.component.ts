import { Component } from '@angular/core';

@Component({
  selector: 'app-monaco-editor',
  template: `
      <ngx-monaco-editor [options]="editorOptions" [(ngModel)]="code"/>
  `
})
export class MonacoEditorComponent {
  editorOptions = {theme: 'vs-dark', language: 'json', automaticLayout: true};
  code: string= '[]';
}
