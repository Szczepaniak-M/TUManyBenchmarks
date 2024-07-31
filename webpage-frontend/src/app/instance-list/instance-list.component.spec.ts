import {ComponentFixture, TestBed} from "@angular/core/testing";
import {InstanceListComponent} from "./instance-list.component";
import {InstanceListService} from "./instance-list.service";
import {Router} from "@angular/router";
import {of} from "rxjs";
import {Instance} from "./instance.model";
import {Filter} from "./instance-list-filter/instance-list-filter.model";
import {SortEvent} from "./instance-list-sort/instance-list-sort.model";
import {By} from "@angular/platform-browser";
import {InstanceListSortComponent} from "./instance-list-sort/instance-list-sort.component";
import {MockComponent} from "ng-mocks";
import {InstanceListFilterComponent} from "./instance-list-filter/instance-list-filter.component";
import {InstanceListRowComponent} from "./instance-list-row/instance-list-row.component";

describe("InstanceListComponent", () => {
  let component: InstanceListComponent;
  let fixture: ComponentFixture<InstanceListComponent>;
  let mockInstanceListService: { getInstances: any }
  let mockRouter: { navigate: any; };
  let mockInstances: Instance[];

  beforeEach(() => {
    mockInstances = [
      {id: "id1", name: "t2.micro", vcpu: 4, memory: 16, network: "Network1", otherTags: ["tag1", "tag2"]},
      {id: "id2", name: "t2.nano", vcpu: 2, memory: 8, network: "Network1", otherTags: ["tag3"]},
      {id: "id3", name: "t2.small", vcpu: 8, memory: 32, network: "Network2", otherTags: ["tag2"]},
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
        MockComponent(InstanceListFilterComponent),
        MockComponent(InstanceListSortComponent),
        MockComponent(InstanceListRowComponent)
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
    expect(component.instances).toEqual(mockInstances);
    expect(component.displayedInstances).toEqual(mockInstances);
    expect(component.allTags).toEqual(["tag1", "tag2", "tag3"]);
    expect(component.allNetworks).toEqual(["Network1", "Network2"]);
  });

  it("should filter instances based on filter criteria", () => {
    fixture.detectChanges()
    const filter: Filter = {name: "micro"};
    component.applyFilters(filter);
    expect(component.displayedInstances).toEqual([mockInstances[0]]);
  });

  it("should add and remove instances from comparison", () => {
    expect(component.selectedInstances).toEqual([]);
    component.toggleComparison(mockInstances[0]);
    expect(component.selectedInstances).toContain(mockInstances[0]);
    component.toggleComparison(mockInstances[0]);
    expect(component.selectedInstances).not.toContain(mockInstances[0]);
  });

  it("should navigate to instance details", () => {
    component.goToDetails(mockInstances[0]);
    expect(mockRouter.navigate).toHaveBeenCalledWith(["/instance", mockInstances[0].name]);
  });

  it("should navigate to comparison page with selected instances", () => {
    component.selectedInstances = [mockInstances[0], mockInstances[1]];
    component.compareSelectedItems();
    expect(mockRouter.navigate).toHaveBeenCalledWith(["/instance/compare"], {
      queryParams: {instances: "t2.micro,t2.nano"}
    });
  });

  it("should correctly identify if an instance is in comparison", () => {
    component.selectedInstances = [mockInstances[0]];
    expect(component.isInComparison(mockInstances[0])).toBeTrue();
    expect(component.isInComparison(mockInstances[1])).toBeFalse();
  });

  it("should sort instances based on the selected column and direction", () => {
    fixture.detectChanges()
    const sortEvent: SortEvent = {column: "memory", direction: "desc"};
    component.onSort(sortEvent);
    fixture.detectChanges()
    expect(component.displayedInstances[0].memory).toBeGreaterThanOrEqual(component.displayedInstances[1].memory);
  });

  it("should track instances by name", () => {
    const instance = mockInstances[0];
    expect(component.trackByName(0, instance)).toBe(instance.name);
  });

  it("should reset sorting direction for other columns when sorting by a new column", () => {
    fixture.detectChanges()
    const sortComponents = fixture.debugElement.queryAll(By.directive(InstanceListSortComponent));
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

