// import {ComponentFixture, TestBed} from "@angular/core/testing";
//
// import {JsonViewerComponent} from "./json-viewer.component";
// import {By} from "@angular/platform-browser";
// import {MockModule} from "ng-mocks";
// import {NgxJsonViewerModule} from "ngx-json-viewer";
//
// describe("JsonViewerComponent", () => {
//   let component: JsonViewerComponent;
//   let fixture: ComponentFixture<JsonViewerComponent>;
//
//   beforeEach(() => {
//     TestBed.configureTestingModule({
//       declarations: [JsonViewerComponent],
//       imports: [MockModule(NgxJsonViewerModule)]
//     })
//
//     fixture = TestBed.createComponent(JsonViewerComponent);
//     component = fixture.componentInstance;
//     fixture.detectChanges();
//   });
//
//   it("should create", () => {
//     expect(component).toBeTruthy();
//   });
//
//   it("should pass inputJson to ngx-json-viewer", () => {
//     const mockJson = '{"key": "value"}';
//     component.inputJson = mockJson;
//     fixture.detectChanges();
//
//     const jsonViewer = fixture.debugElement.query(By.css("ngx-json-viewer")).componentInstance;
//     expect(jsonViewer.json).toBe(mockJson);
//     expect(jsonViewer.expanded).toBe(false);
//   });
// });
//
