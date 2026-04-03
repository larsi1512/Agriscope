import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AlertsNotification } from './alerts-notification';

describe('AlertsNotification', () => {
  let component: AlertsNotification;
  let fixture: ComponentFixture<AlertsNotification>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertsNotification]
    })
      .compileComponents();

    fixture = TestBed.createComponent(AlertsNotification);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
