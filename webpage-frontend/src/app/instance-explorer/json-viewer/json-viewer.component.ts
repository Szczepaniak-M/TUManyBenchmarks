import {Component, Input} from "@angular/core";

@Component({
  selector: "app-json-viewer",
  template: `
    <ngx-json-viewer [json]="inputJson" [expanded]="false"></ngx-json-viewer>
  `
})
export class JsonViewerComponent {
  @Input() inputJson!: string;
}
