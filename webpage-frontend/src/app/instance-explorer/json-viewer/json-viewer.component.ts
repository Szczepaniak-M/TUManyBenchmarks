import { Component } from '@angular/core';

@Component({
  selector: 'app-json-viewer',
  template: `
    <button (click)="downloadJson()">Download JSON</button>
    <ngx-json-viewer [json]="myJsonData"></ngx-json-viewer>
  `
})
export class JsonViewerComponent {

  downloadJson() {
    const jsonStr = JSON.stringify(this.myJsonData, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = 'data.json';
    a.click();

    window.URL.revokeObjectURL(url);
    a.remove();
  }

  myJsonData = [
    {
      "id": "6655f30f89b9d454b1472a4d",
      "name": "x1e.8xlarge",
      "tags": [
        "32 vCPUs",
        "976 GiB Memory",
        "x86_64",
        "ssd",
        "Up to 10 Gigabit Network",
        "Hypervisor Xen"
      ]
    },
    {
      "id": "6655f30f89b9d454b1472a50",
      "name": "m7gd.xlarge",
      "tags": [
        "4 vCPUs",
        "16 GiB Memory",
        "arm64",
        "ssd",
        "Up to 12.5 Gigabit Network",
        "Hypervisor Nitro"
      ]
    }]
}
