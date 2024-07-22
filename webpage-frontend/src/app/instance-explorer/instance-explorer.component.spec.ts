import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InstanceExplorerComponent } from './instance-explorer.component';

describe('InstanceExplorerComponent', () => {
  let component: InstanceExplorerComponent;
  let fixture: ComponentFixture<InstanceExplorerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [InstanceExplorerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InstanceExplorerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
