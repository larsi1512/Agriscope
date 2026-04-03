import { Component } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import {ToastrService} from 'ngx-toastr';
import {Globals} from '../../global/globals';

@Component({
  selector: 'app-forgot-password',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPassword {
  resetEmail: string = '';

  constructor(private http: HttpClient, private toastr: ToastrService, private globals: Globals) {}

  onResetRequest() {
    const url = `${this.globals.backendUri}/users/password-reset?email=${this.resetEmail}`;

    this.http.get(url).subscribe({
      next: (response) => {
        this.toastr.success('If an account exists for ' + this.resetEmail + ', a reset link has been sent!', 'Success');
      },
      error: (err) => {
        console.error('Reset request failed', err);
        this.toastr.error('An error occurred. Please try again later.', 'Error');
      }
    });
  }
}
