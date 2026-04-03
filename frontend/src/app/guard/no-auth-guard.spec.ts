import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { NoAuthGuard } from './no-auth-guard';

describe('noAuthGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => new NoAuthGuard(null as any, null as any).canActivate());

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
