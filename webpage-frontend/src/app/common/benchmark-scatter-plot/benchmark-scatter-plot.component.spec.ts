import { ComponentFixture, TestBed } from "@angular/core/testing";

import { ScatterBenchmarkPlotComponent } from "./benchmark-scatter-plot.component";

describe("BenchmarkPlotComponent", () => {
  let component: ScatterBenchmarkPlotComponent;
  let fixture: ComponentFixture<ScatterBenchmarkPlotComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ScatterBenchmarkPlotComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ScatterBenchmarkPlotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
