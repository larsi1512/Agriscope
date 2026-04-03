import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

// Components
import { TopbarComponent } from '../topbar/topbar';
import { Sidebar } from '../sidebar/sidebar';
import { FieldGrid } from '../field-grid/field-grid';
import { WeatherWidget } from '../weather-widget/weather-widget';
import { Recommendations } from '../recommendations/recommendations';

// Services & Models
import { AuthService } from '../../services/auth-service/auth.service';
import { FarmService } from '../../services/farm-service/farm-service';
import { Farm } from '../../models/Farm';

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
  imports: [
    CommonModule,
    TopbarComponent,
    Sidebar,
    FieldGrid,
    WeatherWidget,
    Recommendations,
  ]
})
export class HomeComponent implements OnInit {
  selectedFarm: Farm | null = null;
  isLoading = true;

  constructor(
    public authService: AuthService,
    private router: Router,
    private toastr: ToastrService,
    private farmService: FarmService
  ) {}

  ngOnInit(): void {
    // Load farms with protection
    this.loadFarmsWithProtection();

    // Subscribe to selected farm observable
    this.farmService.selectedFarm$.subscribe((farm) => {
      this.selectedFarm = farm;
    });
  }

  /**
   * ⭐ Load farms with redirect protection
   */
  private loadFarmsWithProtection(): void {
    this.farmService.loadFarms().subscribe({
      next: (farms) => {
        console.log("Farms loaded:", farms);
        this.isLoading = false;

        // If no farms, redirect to add-farm
        if (farms.length === 0) {
          console.log('No farms found, redirecting to add-farm');

          this.router.navigate(['/new-farm'], {
            queryParams: { firstFarm: 'true' }
          });
          return;
        }

        // Farms exist, continue normally
        console.log(`Loaded ${farms.length} farm(s)`);
      },
      error: (error) => {
        console.error("Error loading farms:", error);
        this.isLoading = false;

        // Show error toast
        this.toastr.error(
          'Failed to load farms. Please try again.',
          'Error'
        );

        // If unauthorized, redirect to login
        if (error.status === 401 || error.status === 403) {
          this.router.navigate(['/login']);
        }
      }
    });
  }

  // Dev control methods
  logFarms(): void {
    console.log('Farms in memory:', this.farmService.getFarmsInMemory());
    console.log('Currently selected farm:', this.selectedFarm);
  }

  loadFarms(): void {
    console.log("Reloading farms...");
    this.isLoading = true;

    this.farmService.loadFarms().subscribe({
      next: (farms) => {
        console.log("Farms reloaded:", farms);
        this.isLoading = false;
        this.toastr.success(`Loaded ${farms.length} farm(s)`, 'Success');
      },
      error: (error) => {
        console.error("Error reloading farms:", error);
        this.isLoading = false;
        this.toastr.error('Failed to reload farms', 'Error');
      }
    });
  }
}
