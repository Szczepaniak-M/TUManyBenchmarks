// import {ComponentFixture, TestBed} from "@angular/core/testing";
// import {InstanceExplorerComponent} from "./instance-explorer.component";
// import {InstanceDetailsService} from "./instance-explorer.service";
// import {MonacoEditorComponent} from "../instance-list/monaco-editor/monaco-editor.component";
// import {MatSlideToggle, MatSlideToggleChange} from "@angular/material/slide-toggle";
// import {of} from "rxjs";
// import {MockComponent} from "ng-mocks";
//
// describe("InstanceExplorerComponent", () => {
//   let component: InstanceExplorerComponent;
//   let fixture: ComponentFixture<InstanceExplorerComponent>;
//   let mockInstanceDetailsService: { executeQuery: any }
//
//   beforeEach(() => {
//     const mockResponse = {
//       results: ['{"key":"value"}'],
//       error: undefined
//     };
//     mockInstanceDetailsService = {
//       executeQuery: jasmine.createSpy("executeQuery").and.returnValue(of(mockResponse))
//     };
//
//     TestBed.configureTestingModule({
//       declarations: [
//         InstanceExplorerComponent,
//         MockComponent(MatSlideToggle),
//         MockComponent(MonacoEditorComponent)
//       ],
//       providers: [
//         {provide: InstanceDetailsService, useValue: mockInstanceDetailsService}
//       ]
//     });
//
//     fixture = TestBed.createComponent(InstanceExplorerComponent);
//     component = fixture.componentInstance;
//     fixture.detectChanges();
//   });
//
//   it("should create", () => {
//     expect(component).toBeTruthy();
//   });
//
//   it("should handle content validation changes", () => {
//     component.onContentValid(true);
//     expect(component.isContentValid).toBe(true);
//
//     component.onContentValid(false);
//     expect(component.isContentValid).toBe(false);
//   });
//
//   it("should handle toggle change", () => {
//     const event = {checked: true} as MatSlideToggleChange;
//     component.onToggleChange(event);
//     expect(component.partialResults).toBe(true);
//
//     event.checked = false;
//     component.onToggleChange(event);
//     expect(component.partialResults).toBe(false);
//   });
//
//   it("should execute query and update results", () => {
//     component.editor = {code: '[{"key":"value"}]'} as MonacoEditorComponent;
//     component.executeQuery();
//
//     expect(mockInstanceDetailsService.executeQuery).toHaveBeenCalledWith(['{"key":"value"}'], component.partialResults);
//     expect(component.results).toEqual([{key: "value"}]);
//     expect(component.error).toBeUndefined();
//   });
//
//   it("should download JSON when downloadJson is called", () => {
//     const jsonBlob = new Blob([JSON.stringify({key: "value"}, null, 2)], {type: "application/json"});
//     const blobUrl = window.URL.createObjectURL(jsonBlob);
//
//     component.results = [{key: "value"}];
//     spyOn(window.URL, "createObjectURL").and.returnValue(blobUrl);
//     const revokeObjectURLSpy = spyOn(window.URL, "revokeObjectURL");
//     const createElementSpy = spyOn(document, "createElement").and.callThrough();
//
//     component.downloadJson();
//
//     expect(window.URL.createObjectURL).toHaveBeenCalledWith(jsonBlob);
//     expect(revokeObjectURLSpy).toHaveBeenCalledWith(blobUrl);
//     expect(createElementSpy).toHaveBeenCalledWith("a");
//   });
// });
//
