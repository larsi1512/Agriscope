import { Component } from '@angular/core';
import {SignupDetail} from '../../dtos/signup';
import {SignupService} from '../../services/signup-service/signup.service';
import {Router} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Toast, ToastrService} from 'ngx-toastr';

@Component({
  selector: 'app-signup.ts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './signup.html',
  styleUrl: './signup.css'
})
export class Signup {
  errorMap: { [key: string]: string } = {};

  signupData: SignupDetail = {
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    password2: ''
  };

  showPassword = false;
  showPassword2 = false;

  constructor(private signupService: SignupService, private router: Router, private toastr: ToastrService) {
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  togglePassword2Visibility() {
    this.showPassword2 = !this.showPassword2;
  }


  //ngOnInit(): void {
  //  // This triggers immediately when the page loads#
  //  this.toastr.success('Hello! The toast is working.', 'System Test');
  //  this.toastr.error('Hello! The toast is working.', 'System Test');
  //}
  onSubmit() {
    this.errorMap = {};
    console.log("submit try");
    this.signupService.signupUser(this.signupData).subscribe({
      next: () => {
        this.toastr.success("Signup successful!");
        this.router.navigate(['/login']); // redirect to login after success
      },
      error: (err) => {
        if (err.error && err.error.errors) {
          console.log(err.error.errors)
          this.toastr.error(err.error.message)
          this.errorMap = err.error.errors;
        } else {
          this.toastr.error("Try again!","Error");
        }
      }
    });
  }
}
