import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../services/auth-service/auth.service';
import { FarmService } from '../../services/farm-service/farm-service';
import { AuthRequest } from '../../dtos/auth-request';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  email = '';
  password = '';
  loginFailed = false;
  showPassword = false;
  isLoading = false;

  constructor(
    private auth: AuthService,
    private farmService: FarmService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  onSubmit() {
    this.loginFailed = false;
    this.isLoading = true;

    this.auth.loginUser(new AuthRequest(this.email, this.password))
      .subscribe({
        next: () => {
          this.toastr.success('Successfully signed in!', 'Welcome');

          // Check if user has farms before routing
          this.checkFarmsAndRoute();
        },
        error: err => {
          this.isLoading = false;  //Stop loading
          this.loginFailed = true;
          this.toastr.error('Please check your credentials.', 'Login failed');
        }
      });
  }

  /**
   * Check farms and route accordingly
   */
  private checkFarmsAndRoute(): void {
    this.farmService.checkHasFarms().subscribe({
      next: (response) => {
        this.isLoading = false;

        if (response.hasFarms) {
          // User has farms → Go to homepage
          console.log(`User has ${response.farmCount} farm(s), routing to home`);
          this.router.navigate(['/home']);
        } else {
          // User has no farms → Force Add New Farm
          console.log('User has no farms, routing to add-farm');
          this.router.navigate(['/new-farm'], {
            queryParams: { firstFarm: 'true' }
          });
        }
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error checking farms:', err);

        // Fallback: try to go to home anyway
        this.router.navigate(['/home']);
      }
    });
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }
}
