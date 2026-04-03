import { Component, HostListener, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { Map } from '../map/map';
import { SoilType } from '../../models/SoilType';
import { FarmCreateDto } from '../../dtos/farm';
import { FarmService } from '../../services/farm-service/farm-service';
import { UserService } from '../../services/user-service/user-service';
import { TopbarComponent } from '../topbar/topbar';

@Component({
  selector: 'app-new-farm-component',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    Map,
    TopbarComponent
  ],
  templateUrl: './new-farm-component.html',
  styleUrl: './new-farm-component.css',
})
export class NewFarmComponent implements OnInit {
  @ViewChild(Map) mapComponent!: Map;

  farm: FarmCreateDto = new FarmCreateDto();
  farmName = '';
  errorMessage = '';
  selectedCoords: { lat: number; lng: number } | null = null;
  results: any[] = [];

  soilTypes = Object.entries(SoilType)
    .filter(([key, value]) => typeof value === 'number')
    .map(([key, value]) => ({ id: value as number, name: key }));

  selectedSoil: SoilType | null = null;

  isFirstFarm = false;
  isSubmitting = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private farmService: FarmService,
    private userService: UserService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.isFirstFarm = params['firstFarm'] === 'true';

      if (this.isFirstFarm) {
        console.log('First farm mode: Back button will be hidden');
      }
    });
  }

  onSearch(event: any) {
    const query = event.target.value;

    if (!query.trim()) {
      this.results = [];
      return;
    }

    fetch(`https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&countrycodes=at&q=${query}`)
      .then(res => res.json())
      .then(data => {
        this.results = data.map((item: any) => ({
          raw: item,
          short: this.formatAddress(item),
        }));
      });
  }

  formatAddress(item: any): string {
    const a = item.address;
    const street = a.road || a.pedestrian || a.cycleway || a.footway || "";
    const number = a.house_number || "";
    const city = a.city || a.town || a.village || a.municipality || "";
    const postcode = a.postcode || "";
    return `${postcode} ${street} ${number}, ${city}`.trim().replace(/^,|,$/g, "");
  }

  selectAddress(r: any) {
    this.results = [];
    const lat = Number(r.lat);
    const lng = Number(r.lon);
    this.selectedCoords = { lat, lng };
    this.farm.latitude = lat;
    this.farm.longitude = lng;
  }

  onLocationSelected(coords: { lat: number; lng: number }) {
    this.farm.latitude = coords.lat;
    this.farm.longitude = coords.lng;
    console.log('Location selected:', coords);
  }

  /**
   * Validate farm name
   */
  validateFarmName(): boolean {
    if (!this.farm.name || this.farm.name.trim() === '') {
      this.toastr.error('Farm name is required', 'Validation Error');
      return false;
    }

    if (this.farm.name.trim().length > 20) {
      this.toastr.error(
        'Farm name must be 20 characters or less',
        'Name Too Long'
      );
      return false;
    }

    return true;
  }

  /**
   * ⭐ Validate location
   */
  validateLocation(): boolean {
    if (!this.farm.latitude || !this.farm.longitude) {
      this.toastr.error(
        'Please select a location on the map',
        'Location Required'
      );
      return false;
    }

    const AUSTRIA_BOUNDS = {
      minLat: 46.3,
      maxLat: 49.1,
      minLng: 9.5,
      maxLng: 17.2
    };

    const lat = this.farm.latitude;
    const lng = this.farm.longitude;

    if (lat < AUSTRIA_BOUNDS.minLat || lat > AUSTRIA_BOUNDS.maxLat ||
      lng < AUSTRIA_BOUNDS.minLng || lng > AUSTRIA_BOUNDS.maxLng) {
      this.toastr.error(
        'Location must be within Austria',
        'Invalid Location'
      );
      return false;
    }

    return true;
  }

  /**
   * Validate soil type
   */
  validateSoilType(): boolean {
    if (!this.selectedSoil && this.selectedSoil !== 0) {
      this.toastr.error(
        'Please select a soil type',
        'Soil Type Required'
      );
      return false;
    }

    return true;
  }

  /**
   * Validate whole form
   */
  private validateForm(): boolean {
    let isValid = true;

    if (!this.validateFarmName()) isValid = false;
    if (!this.validateSoilType()) isValid = false;
    if (!this.validateLocation()) isValid = false;

    return isValid;
  }

  /**
   * Add new farm with validation
   */
  addNewFarm() {
    this.errorMessage = '';

    if (!this.validateForm()) {
      return;
    }

    if (this.selectedSoil !== null) {
      this.farm.soilType = this.selectedSoil;
    }

    if (this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;
    console.log('Creating farm:', this.farm);

    this.farmService.addNewFarm(this.farm).subscribe({
      next: (createdFarm) => {
        console.log('Farm created successfully:', createdFarm);

        this.toastr.success(
          `Farm "${createdFarm.name}" has been created!`,
          'Success'
        );

        setTimeout(() => {
          this.router.navigate(['/home']);
        }, 500);
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('Error creating farm:', err);

        if (err.error) {
          if (typeof err.error === 'object') {
            const errors = err.error;

            if (errors.name) {
              this.toastr.error(errors.name, 'Validation Error');
            }
            if (errors.latitude || errors.longitude) {
              this.toastr.error(
                errors.latitude || errors.longitude,
                'Location Error'
              );
            }
            if (errors.soilType) {
              this.toastr.error(errors.soilType, 'Soil Type Error');
            }
          } else if (typeof err.error === 'string') {
            this.toastr.error(err.error, 'Error');
          }
        } else if (err.status === 409) {
          this.toastr.error('A farm with this name already exists', 'Duplicate Farm');
        } else {
          this.toastr.error('Failed to create farm. Please try again.', 'Error');
        }
      }
    });
  }

  goBack() {
    if (this.isFirstFarm) {
      this.toastr.info(
        'Please create your first farm before continuing',
        'Farm Required'
      );
      return;
    }

    this.router.navigate(['/home']);
  }

  onSubmit() {
    // Handled by addNewFarm()
  }

  isMenuOpen = false;

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.menu-wrapper')) {
      this.isMenuOpen = false;
    }
  }
}
