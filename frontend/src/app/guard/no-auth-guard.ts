import {CanActivateFn, Router} from '@angular/router';
import {Injectable} from '@angular/core';
import {AuthService} from '../services/auth-service/auth.service';

@Injectable({
  providedIn: 'root'
})
export class NoAuthGuard {
  constructor(private authService: AuthService,
              private router: Router) {}

  canActivate(): boolean {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/home']);
      return false;
    } else {
      return true;
    }
  }
}
