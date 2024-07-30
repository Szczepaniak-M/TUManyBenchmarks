import {ComponentFixture, TestBed} from "@angular/core/testing";
import {InstanceDetailsComponent} from "./instance-details.component";
import {ActivatedRoute} from "@angular/router";
import {InstanceDetailsService} from "./instance-details.service";
import {of} from "rxjs";
import {InstanceDetails} from "./instance-details.model";
import {NO_ERRORS_SCHEMA} from "@angular/core";

describe("InstanceDetailsComponent", () => {
  let component: InstanceDetailsComponent;
  let fixture: ComponentFixture<InstanceDetailsComponent>;
  let mockActivatedRoute;
  let mockInstanceDetailsService;
  let instanceDetail: InstanceDetails;

  beforeEach(async () => {
    instanceDetail = {id: "id1", name: "t2.micro", tags: ["tag1", "tag2"], benchmarks: []};

    mockActivatedRoute = {
      snapshot: {
        paramMap: {
          get: () => "t2.micro"
        }
      }
    };

    mockInstanceDetailsService = {
      getInstanceDetails: (name: string) => of(instanceDetail)
    };

    await TestBed.configureTestingModule({
      declarations: [InstanceDetailsComponent],
      providers: [
        {provide: ActivatedRoute, useValue: mockActivatedRoute},
        {provide: InstanceDetailsService, useValue: mockInstanceDetailsService}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(InstanceDetailsComponent);
    component = fixture.componentInstance;
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should initialize with instance details", () => {
    fixture.detectChanges();

    expect(component.instance).toEqual(instanceDetail);
    expect(component.instance.name).toBe("t2.micro");
    expect(component.instance.tags).toEqual(["tag1", "tag2"]);
  });
});
