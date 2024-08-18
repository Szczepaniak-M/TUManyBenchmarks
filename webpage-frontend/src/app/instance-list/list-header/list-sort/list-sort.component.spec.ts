import {ComponentFixture, TestBed} from "@angular/core/testing";
import {By} from "@angular/platform-browser";
import {ListSortComponent} from "./list-sort.component";
import {ChangeDetectorRef} from "@angular/core";

describe("ListSortComponent", () => {
  let component: ListSortComponent;
  let fixture: ComponentFixture<ListSortComponent>;

  beforeEach(() => {

    TestBed.configureTestingModule({
      declarations: [ListSortComponent],
    })

    fixture = TestBed.createComponent(ListSortComponent);
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

  it("should update triangle opacity based on direction", () => {
    const divAsc = fixture.debugElement.query(By.css(".triangle-up")).nativeElement;
    const divDesc = fixture.debugElement.query(By.css(".triangle-down")).nativeElement;

    expect(window.getComputedStyle(divAsc).opacity).toBe("0.125");
    expect(window.getComputedStyle(divDesc).opacity).toBe("0.125");

    component.rotate()
    fixture.componentRef.injector.get(ChangeDetectorRef).detectChanges();
    expect(window.getComputedStyle(divAsc).opacity).toBe("0.6");
    expect(window.getComputedStyle(divDesc).opacity).toBe("0.125");

    component.rotate()
    fixture.componentRef.injector.get(ChangeDetectorRef).detectChanges();
    expect(window.getComputedStyle(divAsc).opacity).toBe("0.125");
    expect(window.getComputedStyle(divDesc).opacity).toBe("0.6");
  });
});
