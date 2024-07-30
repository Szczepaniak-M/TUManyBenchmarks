import {Component, Input, ViewEncapsulation} from "@angular/core";

@Component({
  selector: "app-json-viewer",
  template: `
    <ngx-json-viewer [json]="inputJson" [expanded]="false"></ngx-json-viewer>
  `,
  styleUrls: ["./json-viewer.component.css"]
})
export class JsonViewerComponent {
  @Input() inputJson!: string;


}
