import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.css',
})
export class LandingPageComponent {
  constructor(private router: Router) {}

  navigateToLogin() {
    // Navigate to login page
    this.router.navigate(['/login']);
  }

  navigateToRegister() {
    // Navigate to register page
    this.router.navigate(['/signup']);
  }
}
