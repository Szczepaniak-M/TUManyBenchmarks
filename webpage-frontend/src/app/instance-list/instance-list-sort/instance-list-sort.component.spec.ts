import {ComponentFixture, TestBed} from "@angular/core/testing";
import {By} from "@angular/platform-browser";
import {InstanceListSortComponent} from "./instance-list-sort.component";
import {ChangeDetectorRef} from "@angular/core";

describe("InstanceListSortComponent", () => {
  let component: InstanceListSortComponent;
  let fixture: ComponentFixture<InstanceListSortComponent>;

  beforeEach(() => {

    TestBed.configureTestingModule({
      declarations: [InstanceListSortComponent],
    })

    fixture = TestBed.createComponent(InstanceListSortComponent);
    component = fixture.componentInstance;
    component.column = "Name";
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should emit sort event with 'asc' direction on first click", () => {
    spyOn(component.sort, "emit");
    component.rotate();
    expect(component.direction).toBe("asc");
    expect(component.sort.emit).toHaveBeenCalledWith({column: "Name", direction: "asc"});
  });

  it("should emit sort event with 'desc' direction on second click", () => {
    component.direction = "asc";
    spyOn(component.sort, "emit");
    component.rotate();
    expect(component.direction).toBe("desc");
    expect(component.sort.emit).toHaveBeenCalledWith({column: "Name", direction: "desc"});
  });

  it("should emit sort event with '' direction on third click", () => {
    component.direction = "desc";
    spyOn(component.sort, "emit");
    component.rotate();
    expect(component.direction).toBe("");
    expect(component.sort.emit).toHaveBeenCalledWith({column: "Name", direction: ""});
  });

  it("should reset direction and call markForCheck on resetDirection", () => {
    component.direction = "asc";
    component.resetDirection();
    expect(component.direction).toBe("");
  });

  it("should update SVG opacity based on direction", () => {
    const svgAsc = fixture.debugElement.queryAll(By.css("path"))[0].nativeElement;
    const svgDesc = fixture.debugElement.queryAll(By.css("path"))[1].nativeElement;

    expect(svgAsc.getAttribute("opacity")).toBe("0.125");
    expect(svgDesc.getAttribute("opacity")).toBe("0.125");

    component.rotate()
    fixture.componentRef.injector.get(ChangeDetectorRef).detectChanges();
    expect(svgAsc.getAttribute("opacity")).toBe("0.6");
    expect(svgDesc.getAttribute("opacity")).toBe("0.125");

    component.rotate()
    fixture.componentRef.injector.get(ChangeDetectorRef).detectChanges();
    expect(svgAsc.getAttribute("opacity")).toBe("0.125");
    expect(svgDesc.getAttribute("opacity")).toBe("0.6");
  });
});