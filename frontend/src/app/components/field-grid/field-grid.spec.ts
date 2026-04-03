import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FieldGrid } from './field-grid';

describe('FieldGrid', () => {
  let component: FieldGrid;
  let fixture: ComponentFixture<FieldGrid>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FieldGrid]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FieldGrid);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
