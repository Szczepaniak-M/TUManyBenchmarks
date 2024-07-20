import {ComponentFixture, TestBed} from '@angular/core/testing';
import {InstanceListComponent} from './instance-list.component';
import {InstanceListService} from './instance-list.service';
import {Router} from '@angular/router';
import {of} from 'rxjs';
import {Instance} from './instance.model';
import {NO_ERRORS_SCHEMA} from '@angular/core';

describe('InstanceListComponent', () => {
  let component: InstanceListComponent;
  let fixture: ComponentFixture<InstanceListComponent>;
  let mockInstanceListService;
  let mockRouter: { navigate: any; };
  let instances: Instance[];

  beforeEach(async () => {
    instances = [
      {id: 'id1', name: 't2.micro', tags: ['value1']},
      {id: 'id2', name: 't3.micro', tags: ['value2']},
      {id: 'id3', name: 't2.small', tags: ['value2']},
      {id: 'id4', name: 't3.small', tags: ['value2']}
    ];

    mockInstanceListService = {
      getInstances: () => of(instances)
    };

    mockRouter = {
      navigate: jasmine.createSpy('navigate').and.returnValue(Promise.resolve(true))
    };

    await TestBed.configureTestingModule({
      declarations: [InstanceListComponent],
      providers: [
        {provide: InstanceListService, useValue: mockInstanceListService},
        {provide: Router, useValue: mockRouter}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(InstanceListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with instances', () => {
    fixture.detectChanges();
    expect(component.instances.length).toBe(4);
    expect(component.instances).toEqual(instances);
  });

  it('should select and deselect instances', () => {
    fixture.detectChanges();

    component.onSelectItem(instances[0]);
    expect(component.selectedInstances).toContain(instances[0]);

    component.onSelectItem(instances[0]);
    expect(component.selectedInstances).not.toContain(instances[0]);
  });

  it('should not select more than 3 instances', () => {
    fixture.detectChanges();

    component.onSelectItem(instances[0]);
    component.onSelectItem(instances[1]);
    component.onSelectItem(instances[2]);
    expect(component.selectedInstances.length).toBe(3);

    component.onSelectItem(instances[3]);
    expect(component.selectedInstances.length).toBe(3);

    component.onSelectItem(instances[0]);
    component.onSelectItem(instances[3]);
    expect(component.selectedInstances.length).toBe(3);
  });

  it('should navigate to details on goToDetails', () => {
    fixture.detectChanges();

    component.goToDetails(instances[0]);
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/instance', 't2.micro']);
  });

  it('should navigate to compare selected items', () => {
    fixture.detectChanges();

    component.onSelectItem(instances[0]);
    component.onSelectItem(instances[1]);

    component.compareSelectedItems();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/instance/compare'], {
      queryParams: {instances: 't2.micro,t3.micro'}
    });
  });

  it('should disable compare button if less than 2 items selected', () => {
    fixture.detectChanges();

    let compareButton = fixture.nativeElement.querySelector('button');
    expect(compareButton.disabled).toBeTrue();

    component.onSelectItem(instances[0]);
    fixture.detectChanges();
    expect(compareButton.disabled).toBeTrue();

    component.onSelectItem(instances[1]);
    fixture.detectChanges();
    expect(compareButton.disabled).toBeFalse();
  });
});
