import { Component, EventEmitter, OnInit, Output, Input, SimpleChanges } from '@angular/core';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { ToastrService } from 'ngx-toastr';
import * as L from 'leaflet';
import 'leaflet-control-geocoder';

/**
 * ⭐ Austria geographic bounds
 */
const AUSTRIA_BOUNDS = {
  minLat: 46.3,
  maxLat: 49.1,
  minLng: 9.5,
  maxLng: 17.2
};

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [HttpClientModule],
  templateUrl: './map.html',
  styleUrl: './map.css',
})
export class Map implements OnInit {
  private map: any;
  private redIcon: any;
  private currentMarker: any;

  @Output() locationSelected = new EventEmitter<{ lat: number; lng: number }>();
  @Input() externalCoords: { lat: number; lng: number } | null = null;

  constructor(
    private http: HttpClient,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.initMap();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['externalCoords'] && this.externalCoords && this.map) {
      const { lat, lng } = this.externalCoords;

      // Check if coordinates are in Austria
      if (this.isInAustria(lat, lng)) {
        this.addMarker({ lat, lng });
        this.findLocationName(lat, lng);
        this.map.setView([lat, lng], 15);
      } else {
        console.warn('External coordinates outside Austria:', lat, lng);
        this.toastr.warning(
          'The provided location is outside Austria',
          'Invalid Location'
        );
      }
    }
  }

  private initMap(): void {
    this.map = L.map('map', {
      center: [47.61, 13.5],  // Center of Austria
      zoom: 7,
      minZoom: 7,
      maxZoom: 19
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    // Restrict view to Austria only
    const strictBounds = L.latLngBounds(
      L.latLng(46.2, 9.4),
      L.latLng(49.2, 17.3)
    );

    this.map.setMaxBounds(strictBounds);
    this.map.on('drag', () => {
      this.map.panInsideBounds(strictBounds, { animate: false });
    });

    this.redIcon = L.icon({
      iconUrl: 'https://maps.google.com/mapfiles/ms/icons/red-dot.png',
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -32]
    });

    // Draw Austria bounds
    this.drawAustriaBounds();

    this.map.on('click', (e: any) => {
      const latLng = e.latlng;
      const lat = latLng.lat;
      const lng = latLng.lng;

      // Validate Austria bounds
      if (!this.isInAustria(lat, lng)) {
        console.warn('Location outside Austria:', lat, lng);

        this.toastr.error(
          'Please select a location within Austria',
          'Invalid Location',
          {
            timeOut: 4000,
            progressBar: true
          }
        );

        return;
      }

      // Valid location
      this.addMarker(latLng);
      this.findLocationName(lat, lng);
      this.locationSelected.emit({ lat, lng });

      this.toastr.success(
        `Location selected: ${lat.toFixed(4)}, ${lng.toFixed(4)}`,
        'Location Set',
        { timeOut: 2000 }
      );
    });

    // Geocoder with Austria restriction
    const geocoder = (L.Control as any).geocoder({
      defaultMarkGeocode: false,
      geocoder: new ((L.Control as any).Geocoder.nominatim)({
        geocodingQueryParams: {
          countrycodes: 'at',
          bounded: 1,
          viewbox: '9.5,46.4,17.2,49.0'
        }
      })
    })
      .on('markgeocode', (result: any) => {
        const latlng = result.geocode.center;

        if (!this.isInAustria(latlng.lat, latlng.lng)) {
          this.toastr.error(
            'Please select a location within Austria',
            'Invalid Location'
          );
          return;
        }

        this.map.setView(latlng, 13);
        this.addMarker(latlng, result.geocode.name);
      })
      .addTo(this.map);
  }

  /**
   * Draw Austria bounds rectangle on map
   */
  private drawAustriaBounds(): void {
    const austriaBounds = L.latLngBounds(
      L.latLng(AUSTRIA_BOUNDS.minLat, AUSTRIA_BOUNDS.minLng),
      L.latLng(AUSTRIA_BOUNDS.maxLat, AUSTRIA_BOUNDS.maxLng)
    );

    L.rectangle(austriaBounds, {
      color: '#52A722',
      weight: 2,
      fillColor: '#52A722',
      fillOpacity: 0.05,
      interactive: false
    }).addTo(this.map);

    L.marker([AUSTRIA_BOUNDS.maxLat - 0.2, AUSTRIA_BOUNDS.minLng + 0.3], {
      icon: L.divIcon({
        className: 'austria-label',
        html: '<div style="color: #52A722; padding: 3px 6px; font-weight: 600; white-space: nowrap; font-size: 11px; text-shadow: 1px 1px 2px white, -1px -1px 2px white, 1px -1px 2px white, -1px 1px 2px white;">🇦🇹 Austria - Valid Area</div>',
        iconSize: [120, 20]
      }),
      interactive: false
    }).addTo(this.map);
  }

  /**
   * Check if coordinates are within Austria
   */
  private isInAustria(lat: number, lng: number): boolean {
    return lat >= AUSTRIA_BOUNDS.minLat &&
      lat <= AUSTRIA_BOUNDS.maxLat &&
      lng >= AUSTRIA_BOUNDS.minLng &&
      lng <= AUSTRIA_BOUNDS.maxLng;
  }

  /**
   * Add current location marker
   */
  public addCurrentLocationMarker(position: { lat: number; lng: number }): void {
    const latLng = L.latLng(position.lat, position.lng);

    // ⭐ VALIDATE: Check if current location is in Austria
    if (!this.isInAustria(position.lat, position.lng)) {
      this.toastr.warning(
        'Your current location is outside Austria',
        'Location Outside Austria'
      );
      return;
    }

    if (this.currentMarker) {
      this.map.removeLayer(this.currentMarker);
    }

    this.currentMarker = L.marker(latLng, { icon: this.redIcon }).addTo(this.map);
    this.currentMarker.bindPopup('Your Current Location').openPopup();

    this.locationSelected.emit(position);
  }

  /**
   * Add marker on map
   */
  private addMarker(latlng: any, popupText?: string): void {
    // Remove existing marker if it exists
    if (this.currentMarker) {
      this.map.removeLayer(this.currentMarker);
    }

    this.currentMarker = L.marker(latlng, { icon: this.redIcon }).addTo(this.map);
    if (popupText) {
      this.currentMarker.bindPopup(popupText).openPopup();
    } else {
      this.findLocationName(latlng.lat.toFixed(4), latlng.lng.toFixed(4));
    }
  }

  /**
   * Find location name using reverse geocoding
   */
  private findLocationName(lat: number, lng: number): void {
    const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json`;

    this.http.get(url).subscribe(
      (data: any) => {
        const locationName = data.display_name;
        if (locationName) {
          L.popup()
            .setLatLng([lat, lng])
            .setContent(`Location: ${locationName}`)
            .openOn(this.map);
        }
      },
      (error) => {
        console.error('Error fetching location name', error);
      }
    );
  }

  /**
   * Public method to validate coordinates
   */
  public validateCoordinates(lat: number, lng: number): boolean {
    return this.isInAustria(lat, lng);
  }
}
