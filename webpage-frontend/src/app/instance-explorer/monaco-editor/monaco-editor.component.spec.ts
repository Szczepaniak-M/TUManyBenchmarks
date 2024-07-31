import {MonacoEditorComponent} from "./monaco-editor.component";
import {ComponentFixture, TestBed} from "@angular/core/testing";
import {Component, EventEmitter, Input, Output} from "@angular/core";


@Component({
  selector: "ngx-monaco-editor",
  template: "<div></div>"
})
class MockNgxMonacoEditorComponent {
  @Input() style: any;
  @Input() ngModel: any;
  @Input() options: any;
  @Output() ngModelChange = new EventEmitter();
  @Output() onInit = new EventEmitter();
}


describe("MonacoEditorComponent", () => {
  let component: MonacoEditorComponent;
  let fixture: ComponentFixture<MonacoEditorComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MonacoEditorComponent, MockNgxMonacoEditorComponent]
    });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(MonacoEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize the editor and set up content change listener", () => {
    const mockEditorInstance = {
      onDidChangeModelContent: jasmine.createSpy("onDidChangeModelContent")
    };

    component.onEditorInit(mockEditorInstance);
    expect(component.editor).toBe(mockEditorInstance);
    expect(mockEditorInstance.onDidChangeModelContent).toHaveBeenCalled();
  });

  it("should validate JSON content and emit isContentValid equals true when content is array of objects", () => {
    spyOn(component.isContentValid, "emit");
    component.code = '[{"key": "value"}]';
    component.checkForErrors();
    expect(component.isContentValid.emit).toHaveBeenCalledWith(true);
  });

  it("should validate JSON content and emit isContentValid equals false when content is invalid JSON", () => {
    spyOn(component.isContentValid, "emit");
    component.code = '[{"key": "value"]';
    component.checkForErrors();
    expect(component.isContentValid.emit).toHaveBeenCalledWith(false);
  });

  it("should validate JSON content and emit isContentValid equals false when content is empty array", () => {
    spyOn(component.isContentValid, "emit");
    component.code = "[]";
    component.checkForErrors();
    expect(component.isContentValid.emit).toHaveBeenCalledWith(false);
  });

  it("should update the code model and validate on content change", () => {
    spyOn(component, "checkForErrors");
    const mockEditorInstance = {
      onDidChangeModelContent: (callback: Function) => {
        component.code = "[{'new': 'content'}]";
        callback();
      }
    };

    component.onEditorInit(mockEditorInstance);
    fixture.detectChanges();

    expect(component.checkForErrors).toHaveBeenCalled();
    expect(component.code).toBe("[{'new': 'content'}]");
  });
});
