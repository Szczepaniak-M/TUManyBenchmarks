import {ComponentFixture, TestBed} from "@angular/core/testing";
import {InstanceListComponent} from "./instance-list.component";
import {InstanceListService} from "./instance-list.service";
import {Router} from "@angular/router";
import {of} from "rxjs";
import {Instance} from "./instance.model";
import {Filter} from "./list-filter/list-filter.model";
import {SortEvent} from "./list-header/list-sort/list-sort.model";
import {By} from "@angular/platform-browser";
import {ListSortComponent} from "./list-header/list-sort/list-sort.component";
import {MockComponent} from "ng-mocks";
import {ListFilterComponent} from "./list-filter/list-filter.component";
import {ListRowComponent} from "./list-row/list-row.component";

describe("InstanceListComponent", () => {
  let component: InstanceListComponent;
  let fixture: ComponentFixture<InstanceListComponent>;
  let mockInstanceListService: { getInstances: any }
  let mockRouter: { navigate: any; };
  let mockInstances: Instance[];

  beforeEach(() => {
    mockInstances = [
      {id: "id1", name: "t2.micro", vcpu: 4, memory: 16, network: "Network1", tags: ["tag1", "tag2"], benchmarks: []},
      {id: "id2", name: "t2.nano", vcpu: 2, memory: 8, network: "Network1", tags: ["tag3"], benchmarks: []},
      {id: "id3", name: "t2.small", vcpu: 8, memory: 32, network: "Network2", tags: ["tag2"], benchmarks: []},
    ];

    mockInstanceListService = {
      getInstances: jasmine.createSpy("getInstances").and.returnValue(of(mockInstances))
    };
    mockRouter = {
      navigate: jasmine.createSpy("navigate").and.returnValue(Promise.resolve(true))
    };

    TestBed.configureTestingModule({
      declarations: [
        InstanceListComponent,
        MockComponent(ListFilterComponent),
        MockComponent(ListSortComponent),
        MockComponent(ListRowComponent)
      ],
      providers: [
        {provide: InstanceListService, useValue: mockInstanceListService},
        {provide: Router, useValue: mockRouter}
      ]
    });

    fixture = TestBed.createComponent(InstanceListComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize instances and filters on ngOnInit", () => {
    fixture.detectChanges()
    expect(component.rows).toEqual(mockInstances);
    expect(component.rows).toEqual(mockInstances);
    expect(component.allTags).toEqual(["tag1", "tag2", "tag3"]);
    expect(component.allNetworks).toEqual(["Network1", "Network2"]);
  });

  it("should filter instances based on filter criteria", () => {
    fixture.detectChanges()
    const filter: Filter = {name: "micro"};
    component.applyFilters(filter);
    expect(component.rows).toEqual([mockInstances[0]]);
  });

  it("should add and remove instances from comparison", () => {
    expect(component.selectedInstances).toEqual([]);
    component.toggleComparison(mockInstances[0]);
    expect(component.selectedInstances).toContain(mockInstances[0].name);
    component.toggleComparison(mockInstances[0]);
    expect(component.selectedInstances).not.toContain(mockInstances[0].name);
  });

  it("should navigate to instance details", () => {
    component.goToDetails(mockInstances[0]);
    expect(mockRouter.navigate).toHaveBeenCalledWith(["/instance", mockInstances[0].name]);
  });

  it("should navigate to comparison page with selected instances", () => {
    component.selectedInstances = [mockInstances[0].name, mockInstances[1].name];
    component.compareSelectedItems();
    expect(mockRouter.navigate).toHaveBeenCalledWith(["/instance/compare"], {
      queryParams: {instances: "t2.micro,t2.nano"}
    });
  });

  it("should correctly identify if an instance is in comparison", () => {
    component.selectedInstances = [mockInstances[0].name];
    expect(component.isInComparison(mockInstances[0].name)).toBeTrue();
    expect(component.isInComparison(mockInstances[1].name)).toBeFalse();
  });

  it("should sort instances based on the selected column and direction", () => {
    fixture.detectChanges()
    const sortEvent: SortEvent = {column: "memory", direction: "desc"};
    component.onSort(sortEvent);
    fixture.detectChanges()
    expect(component.rows[0].memory).toBeGreaterThanOrEqual(component.rows[1].memory);
  });

  it("should track instances by name", () => {
    const instance = mockInstances[0];
    expect(component.trackById(0, instance)).toBe(instance["id"]);
  });

  it("should reset sorting direction for other columns when sorting by a new column", () => {
    fixture.detectChanges()
    const sortComponents = fixture.debugElement.queryAll(By.directive(ListSortComponent));
    sortComponents.forEach(sortComponent => {
      spyOn(sortComponent.componentInstance, "resetDirection");
    });

    component.onSort({column: "memory", direction: "asc"});
    sortComponents.forEach(sortComponent => {
      if (sortComponent.componentInstance.column !== "memory") {
        expect(sortComponent.componentInstance.resetDirection).toHaveBeenCalled();
      }
    });
  });
});

