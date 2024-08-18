import {ComponentFixture, TestBed} from "@angular/core/testing";
import {ListHeaderComponent} from "./list-header.component";
import {By} from "@angular/platform-browser";
import {SortEvent} from "./list-sort/list-sort.model";
import {MockComponent} from "ng-mocks";
import {ListSortComponent} from "./list-sort/list-sort.component";

describe("ListHeaderComponent", () => {
  let component: ListHeaderComponent;
  let fixture: ComponentFixture<ListHeaderComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [
        ListHeaderComponent,
        MockComponent(ListSortComponent)
      ],
    })

    fixture = TestBed.createComponent(ListHeaderComponent);
    component = fixture.componentInstance;
    component.columns = ["Name", "Memory", "Tags"];
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should render correct number of columns", () => {
    const columnElements = fixture.debugElement.queryAll(By.css(".min-w-40"));
    expect(columnElements.length).toBe(3);
  });

  it("should render the Tags column without a sort component", () => {
    const tagsColumn = fixture.debugElement.query(By.css(".min-w-40:last-child"));
    expect(tagsColumn.nativeElement.textContent.trim()).toContain("Tags");
    const sortComponent = tagsColumn.query(By.directive(ListSortComponent));
    expect(sortComponent).toBeNull();
  });

  it("should emit sort event when a column is sorted", () => {
    spyOn(component.sort, "emit");

    const sortComponent = fixture.debugElement.query(By.directive(ListSortComponent));
    const sortEvent: SortEvent = {column: "Name", direction: "asc"};
    sortComponent.triggerEventHandler("sort", sortEvent);

    expect(component.sort.emit).toHaveBeenCalledWith(sortEvent);
  });

  it("should reset direction on other columns when a column is sorted", () => {
    const sortComponents = fixture.debugElement.queryAll(By.directive(ListSortComponent));
    const nameSortComponent = sortComponents[0].componentInstance as ListSortComponent;
    const memorySortComponent = sortComponents[1].componentInstance as ListSortComponent;

    spyOn(nameSortComponent, "resetDirection");
    spyOn(memorySortComponent, "resetDirection");

    const sortEvent: SortEvent = {column: "Name", direction: "asc"};
    nameSortComponent.sort.emit(sortEvent);

    expect(nameSortComponent.resetDirection).not.toHaveBeenCalled();
    expect(memorySortComponent.resetDirection).toHaveBeenCalled();
  });
});
