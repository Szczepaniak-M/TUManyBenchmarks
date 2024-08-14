import {ComponentFixture, TestBed} from "@angular/core/testing";
import {By} from "@angular/platform-browser";
import {InstanceListRowComponent} from "./instance-list-row.component";
import {Instance} from "../instance.model";
import {RouterModule} from "@angular/router";

describe("InstanceListRowComponent", () => {
  let component: InstanceListRowComponent;
  let fixture: ComponentFixture<InstanceListRowComponent>;

  const testInstance: Instance = {
    id: "1",
    name: "test-instance",
    vCpu: 4,
    memory: 16,
    network: "test-network",
    tags: ["tag1", "tag2"],
    benchmarks: []
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterModule.forRoot([]),
      ],
      declarations: [InstanceListRowComponent]
    })

    fixture = TestBed.createComponent(InstanceListRowComponent);
    component = fixture.componentInstance;
    component.instance = testInstance;
    component.isInComparison = false;
    component.onToggleComparison = jasmine.createSpy("onToggleComparison").and.returnValue(true);
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should display instance details", () => {
    const compiled = fixture.nativeElement as HTMLElement;

    const basicInformationDivs = compiled.querySelectorAll(".w-1\\/6");
    expect(basicInformationDivs[0].textContent).toContain("test-instance");
    expect(basicInformationDivs[1].textContent).toContain("4 vCPUs");
    expect(basicInformationDivs[2].textContent).toContain("16 GiB");
    expect(basicInformationDivs[3].textContent).toContain("test-network");

    const tagsSpans = compiled.querySelectorAll("span");
    expect(tagsSpans[0].textContent).toContain("tag1");
    expect(tagsSpans[1].textContent).toContain("tag2");
  });

  it("should call onToggleComparison and update isInComparison on row click", () => {
    const rowElement = fixture.debugElement.query(By.css(".flex.flex-row.border")).nativeElement;
    rowElement.click();
    fixture.detectChanges();

    expect(component.onToggleComparison).toHaveBeenCalledWith(testInstance);
    expect(component.isInComparison).toBe(true);
  });

  it("should have bg-gray-200 class if isInComparison is true", () => {
    fixture.componentRef.setInput("isInComparison", true)
    fixture.detectChanges();

    const rowElement = fixture.debugElement.query(By.css(".flex.flex-row.border")).nativeElement;
    expect(rowElement.classList).toContain("bg-gray-200");
  });

  it("should not have bg-gray-200 class if isInComparison is false", () => {
    fixture.componentRef.setInput("isInComparison", false)
    fixture.detectChanges();

    const rowElement = fixture.debugElement.query(By.css(".flex.flex-row.border")).nativeElement;
    expect(rowElement.classList).not.toContain("bg-gray-200");
  });
});
