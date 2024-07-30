import { ComponentFixture, TestBed } from "@angular/core/testing";

import { InstanceListFilterComponent } from "./instance-list-filter.component";

describe("InstanceListFilterComponent", () => {
  let component: InstanceListFilterComponent;
  let fixture: ComponentFixture<InstanceListFilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InstanceListFilterComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InstanceListFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
