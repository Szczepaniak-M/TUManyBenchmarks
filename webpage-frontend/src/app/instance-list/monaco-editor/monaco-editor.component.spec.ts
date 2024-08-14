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
});
