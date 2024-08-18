import {ComponentFixture, TestBed} from "@angular/core/testing";
import {By} from "@angular/platform-browser";
import {ListRowComponent} from "./list-row.component";
import {RouterModule} from "@angular/router";

describe("ListRowComponent", () => {
  let component: ListRowComponent;
  let fixture: ComponentFixture<ListRowComponent>;

  const testInstance = {
    id: "1",
    Name: "test-instance",
    vCPUs: 4,
    Memory: 16,
    Tags: ["tag1", "tag2"],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterModule.forRoot([]),
      ],
      declarations: [ListRowComponent]
    })

    fixture = TestBed.createComponent(ListRowComponent);
    component = fixture.componentInstance;
    component.row = testInstance;
    component.isInComparison = false;
    component.columns = ["Name", "vCPUs", "Memory", "Tags"];
    component.onToggleComparison = jasmine.createSpy("onToggleComparison").and.returnValue(true);
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should display values for each column", () => {
    const compiled = fixture.nativeElement as HTMLElement;

    const basicInformationDivs = compiled.querySelectorAll(".w-1\\/4");
    expect(basicInformationDivs[0].textContent).toContain("test-instance");
    expect(basicInformationDivs[1].textContent).toContain("4");
    expect(basicInformationDivs[2].textContent).toContain("16");

    const tagsSpans = compiled.querySelectorAll("span");
    expect(tagsSpans[0].textContent).toContain("tag1");
    expect(tagsSpans[1].textContent).toContain("tag2");
  });

  it("should apply routerLink to the Name field", () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const nameElement = compiled.querySelector(".text-blue-500");
    expect(nameElement?.getAttribute("ng-reflect-router-link")).toBe("/instance,test-instance");
  });

  it("should toggle comparison state on click", () => {
    expect(component.isInComparison).toBeFalse();

    const rowElement = fixture.debugElement.query(By.css(".flex-row")).nativeElement;
    rowElement.click();
    fixture.detectChanges();

    expect(component.onToggleComparison).toHaveBeenCalledWith(component.row);
    expect(component.isInComparison).toBeTrue();
  });

  it("should have bg-gray-200 class if isInComparison is true", () => {
    fixture.componentRef.setInput("isInComparison", true);
    fixture.detectChanges();

    const rowElement = fixture.debugElement.query(By.css(".flex.flex-row.border")).nativeElement;
    expect(rowElement.classList).toContain("bg-gray-200");
  });

  it("should not have bg-gray-200 class if isInComparison is false", () => {
    fixture.componentRef.setInput("isInComparison", false);
    fixture.detectChanges();

    const rowElement = fixture.debugElement.query(By.css(".flex.flex-row.border")).nativeElement;
    expect(rowElement.classList).not.toContain("bg-gray-200");
  });
});
