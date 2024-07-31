import {ComponentFixture, TestBed} from "@angular/core/testing";
import {By} from "@angular/platform-browser";
import {InstanceListFilterComponent} from "./instance-list-filter.component";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {FormsModule} from "@angular/forms";
import {MatFormField, MatLabel} from "@angular/material/form-field";
import {MatInput} from "@angular/material/input";
import {MatOption, MatSelect} from "@angular/material/select";
import {MockComponent, MockDirective, MockModule} from "ng-mocks";

describe("InstanceListFilterComponent", () => {
  let component: InstanceListFilterComponent;
  let fixture: ComponentFixture<InstanceListFilterComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        MockComponent(MatFormField),
        MockComponent(MatSelect),
        MockComponent(MatOption),
        MockDirective(MatLabel),
        MockDirective(MatInput),
        MockModule(FormsModule),
        MockModule(BrowserAnimationsModule)
      ],
      declarations: [InstanceListFilterComponent]
    })

    fixture = TestBed.createComponent(InstanceListFilterComponent);
    component = fixture.componentInstance;
    component.allNetworks = ["Network1", "Network2"];
    component.allTags = ["Tag1", "Tag2"];
    component.selectedInstances = 1;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should bind input properties", () => {
    expect(component.allNetworks).toEqual(["Network1", "Network2"]);
    expect(component.allTags).toEqual(["Tag1", "Tag2"]);
    expect(component.selectedInstances).toBe(1);
  });

  it("should emit filterChange event on filter change", () => {
    spyOn(component.filterChange, "emit");
    component.filter.name = "TestName";
    component.onFilterChange();
    expect(component.filterChange.emit).toHaveBeenCalledWith(component.filter);
  });

  it("should emit redirectToComparison event on button click", () => {
    spyOn(component.redirectToComparison, "emit");
    fixture.componentRef.setInput("selectedInstances", 2)
    fixture.detectChanges();
    const button = fixture.debugElement.query(By.css("button")).nativeElement;
    button.click();
    expect(component.redirectToComparison.emit).toHaveBeenCalled();
  });

  it("should disable the compare button when selectedInstances < 2", () => {
    fixture.componentRef.setInput("selectedInstances", 1)
    fixture.detectChanges();
    const button = fixture.debugElement.query(By.css("button")).nativeElement;
    expect(button.disabled).toBeTruthy();
  });

  it("should enable the compare button when selectedInstances >= 2", () => {
    fixture.componentRef.setInput("selectedInstances", 2)
    fixture.detectChanges();
    const button = fixture.debugElement.query(By.css("button")).nativeElement;
    expect(button.disabled).toBeFalsy();
  });
});
