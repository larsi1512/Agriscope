import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import {ResetPasswordDto} from '../../dtos/user';
import {Globals} from '../../global/globals';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './reset-password.html',
  styleUrls: ['./reset-password.css']
})
export class ResetPassword implements OnInit {
  errorMap: { [key: string]: string } = {};

  resetData: ResetPasswordDto = {
    password: '',
    confirmPassword: ''
  };

  token: string | null = null;
  showPassword = false;
  showPassword2 = false;

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
    private toastr: ToastrService,
    private globals: Globals
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');

    if (!this.token) {
      this.toastr.error("Invalid or missing reset token. Please request a new link.");
      this.router.navigate(['/forgot-password']);
    }
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  togglePassword2() {
    this.showPassword2 = !this.showPassword2;
  }

  onResetPassword() {
    this.errorMap = {};

    const url = `${this.globals.backendUri}/users/password-reset?token=${this.token}`;

    this.http.post(url, this.resetData, { responseType: 'text' }).subscribe({
      next: (response) => {
        this.toastr.success("Password updated successfully!");
        this.router.navigate(['/login']);
      },
      error: (err) => {
        try {
          const errorBody = JSON.parse(err.error);
          this.errorMap = errorBody.errors || {};

          if (this.errorMap['token']) {
            this.toastr.error("Your reset link has expired. Redirecting...");
            setTimeout(() => this.router.navigate(['/forgot-password']), 3000);
            return;
          }

          if (errorBody.message) {
            this.toastr.error(errorBody.message);
          }
        } catch (e) {
          this.toastr.error(err.error || "An unexpected error occurred.");
        }
      }
    });
  }
}
