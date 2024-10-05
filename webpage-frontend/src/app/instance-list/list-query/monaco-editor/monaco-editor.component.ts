import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";
import {environment} from "../../../../environments/environment";


@Component({
  selector: "app-monaco-editor",
  template: `
    <ngx-monaco-editor style="height: 100%"
                       [(ngModel)]="query"
                       (onInit)="onInit($event)"
                       [options]="editorOptions"/>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MonacoEditorComponent {
  @Input() query: string = "";
  @Output() executeQuery = new EventEmitter<Boolean>();
  editorOptions = {
    theme: "vs-dark",
    language: "pgsql",
    automaticLayout: true,
    scrollBeyondLastLine: false,
    minimap: {
      enabled: false
    }
  };

  onInit(editor: any) {
    // Tests aren't loaded with editor.addCommand()
    // because Monaco Editor loads CSS and creating bundle fails
    // Work-around is to include editor.addCommand() only in build for production
    environment.addCommandMonacoEditor(editor, this.executeQuery)
  }
}
