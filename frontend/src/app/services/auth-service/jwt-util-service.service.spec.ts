import { TestBed } from '@angular/core/testing';

import { JwtUtilServiceService } from './jwt-util-service.service';

describe('JwtUtilServiceService', () => {
  let service: JwtUtilServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(JwtUtilServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
