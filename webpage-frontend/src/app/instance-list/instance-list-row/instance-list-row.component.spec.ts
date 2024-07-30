import { ComponentFixture, TestBed } from "@angular/core/testing";

import { InstanceListRowComponent } from "./instance-list-row.component";

describe("InstanceListRowComponent", () => {
  let component: InstanceListRowComponent;
  let fixture: ComponentFixture<InstanceListRowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InstanceListRowComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InstanceListRowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
