import { ComponentFixture, TestBed } from "@angular/core/testing";

import { CompareInstancesBenchmarkComponent } from "./compare-instances-benchmark.component";

describe("CompareInstancesBenchmarkComponent", () => {
  let component: CompareInstancesBenchmarkComponent;
  let fixture: ComponentFixture<CompareInstancesBenchmarkComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CompareInstancesBenchmarkComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CompareInstancesBenchmarkComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
