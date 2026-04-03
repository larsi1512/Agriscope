import { Component, HostListener, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth-service/auth.service';
import { ToastrService } from 'ngx-toastr';
import { AlertsNotification } from '../alerts-notification/alerts-notification';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, AlertsNotification],
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
})
export class TopbarComponent {
  // Inputs for customization
  @Input() showFarmTitle: boolean = false;
  @Input() farmName: string | null = null;

  isMenuOpen = false;

  constructor(
    private router: Router,
    private authService: AuthService,
    private toastr: ToastrService
  ) {}

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  goToProfile() {
    this.isMenuOpen = false;
    this.router.navigate(['/profile']);
  }

  goToHome() {
    this.isMenuOpen = false;
    this.router.navigate(['/home']);
  }

  logout() {
    this.authService.logoutUser();
    this.isMenuOpen = false;
    this.toastr.success('Signed out!', 'Success');
    this.router.navigate(['/login']);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.menu-wrapper')) {
      this.isMenuOpen = false;
    }
  }
}
