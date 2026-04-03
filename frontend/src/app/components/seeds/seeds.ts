import { Component, HostListener, OnInit} from '@angular/core';

import { Sidebar } from '../sidebar/sidebar';
import {CommonModule} from '@angular/common';
import { Router } from '@angular/router';
import { SeedService } from '../../services/seed-service/seed-service';
import { Seed } from '../../models/Seed';
import {TopbarComponent} from '../topbar/topbar';

@Component({
  selector: 'app-seeds',
  standalone: true,
  imports: [
    Sidebar,
    CommonModule,
    TopbarComponent
  ],
  templateUrl: './seeds.html',
  styleUrl: './seeds.css',
})
export class SeedsComponent implements OnInit {

  isMenuOpen = false;
  constructor( private router: Router, private seedService : SeedService) { }

  seeds : Seed[] = [];
  ngOnInit(): void {
    this.seedService.getAll().subscribe(
      data => {
        if (data) {
          console.log('=== RAW DATA FROM BACKEND ===');
          console.log('Full response:', data);
          console.log('Number of seeds:', data.length);

          if (data.length > 0) {
            console.log('=== FIRST SEED DETAILS ===');
            console.log('Full first seed object:', data[0]);
            console.log('Keys in first seed:', Object.keys(data[0]));
            console.log('daysToReady value:', data[0].daysToReady);
            console.log('Type of daysToReady:', typeof data[0].daysToReady);
          }

          this.seeds = data;
        }
      },
      error => {
        console.error('Error fetching seeds:', error);
      }
    );
  }

  getSeedIcon(seedType: string): string {
  switch(seedType) {
    case 'CORN': return '/assets/icons/corn.svg';
    case 'WHEAT': return '/assets/icons/wheat.svg';
    case 'BARLEY': return '/assets/icons/barely.svg';
    case 'PUMPKIN': return '/assets/icons/pumpkin.svg';
    case 'BLACK_GRAPES': return '/assets/icons/grape.svg';
    case 'WHITE_GRAPES': return '/assets/icons/white_grape.svg';
    default: return '/assets/icons/default.svg';
  }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.menu-wrapper')) {
      this.isMenuOpen = false;
    }
  }

  selectedSeed: Seed | null = null;

  formatSeedType(seedType: string): string {
    // Convert "CORN" to "Corn", "BLACK_GRAPES" to "Black Grapes"
    return seedType
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  }

  openDetailsModal(seed: Seed): void {
    this.selectedSeed = seed;
  }

  closeDetailsModal(): void {
    this.selectedSeed = null;
  }
}
