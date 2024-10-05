import {MonacoEditorComponent} from "./monaco-editor.component";
import {ComponentFixture, TestBed} from "@angular/core/testing";
import {Component, EventEmitter, Input, Output} from "@angular/core";


@Component({
  selector: "ngx-monaco-editor",
  template: "<div></div>"
})
class MockNgxMonacoEditorComponent {
  @Input() ngModel: any;
  @Input() options: any;
  @Output() onInit = new EventEmitter();
}


describe("MonacoEditorComponent", () => {
  let component: MonacoEditorComponent;
  let fixture: ComponentFixture<MonacoEditorComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MonacoEditorComponent, MockNgxMonacoEditorComponent]
    });

    fixture = TestBed.createComponent(MonacoEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
