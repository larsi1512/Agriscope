import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { LandingPageComponent } from './landing-page';

describe('LandingPageComponent', () => {
  let component: LandingPageComponent;
  let fixture: ComponentFixture<LandingPageComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingPageComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(LandingPageComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should navigate to login when navigateToLogin is called', () => {
    const navigateSpy = spyOn(router, 'navigate');
    component.navigateToLogin();
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('should navigate to register when navigateToRegister is called', () => {
    const navigateSpy = spyOn(router, 'navigate');
    component.navigateToRegister();
    expect(navigateSpy).toHaveBeenCalledWith(['/register']);
  });
});
