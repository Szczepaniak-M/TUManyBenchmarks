import {ComponentFixture, TestBed} from "@angular/core/testing";
import {ActivatedRoute} from "@angular/router";
import {InstanceDetailsService} from "../instance-details/instance-details.service";
import {of} from "rxjs";
import {InstanceDetails} from "../instance-details/instance-details.model";
import {NO_ERRORS_SCHEMA} from "@angular/core";
import {CompareInstancesComponent} from "./compare-instances.component";

describe("CompareInstancesComponent", () => {
  let component: CompareInstancesComponent;
  let fixture: ComponentFixture<CompareInstancesComponent>;
  let mockActivatedRoute;
  let mockInstanceDetailsService;
  let instanceDetails: InstanceDetails[];

  beforeEach(async () => {
    instanceDetails = [
      {id: "id1", name: "t2.micro", tags: ["value1"], benchmarks: []},
      {id: "id2", name: "t3.micro", tags: ["value2"], benchmarks: []}
    ];

    mockActivatedRoute = {
      snapshot: {
        queryParamMap: {
          get: () => "t2.micro,t3.micro"
        }
      }
    };

    mockInstanceDetailsService = {
      getInstanceDetails: (name: string) => {
        const instance = instanceDetails.find(instance => instance.name === name);
        return of(instance);
      }
    };

    await TestBed.configureTestingModule({
      declarations: [CompareInstancesComponent],
      providers: [
        {provide: ActivatedRoute, useValue: mockActivatedRoute},
        {provide: InstanceDetailsService, useValue: mockInstanceDetailsService}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(CompareInstancesComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize with instance details", () => {
    fixture.detectChanges();

    expect(component.instances.length).toBe(2);
    expect(component.instances[0].name).toBe("t2.micro");
    expect(component.instances[1].name).toBe("t3.micro");
  });
});
