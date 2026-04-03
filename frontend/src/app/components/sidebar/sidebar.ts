import {Component, EventEmitter, Input, Output} from '@angular/core';
import { CommonModule } from '@angular/common'; // Import CommonModule
import { Router } from '@angular/router';
import {UserService} from '../../services/user-service/user-service';
import { Farm } from '../../models/Farm';
import { FarmService } from '../../services/farm-service/farm-service';

@Component({
  selector: 'app-sidebar',
  imports: [CommonModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar {
  displayName: string = 'Loading...';
  farms: Farm[] = [];
  selectedFarm: Farm | null = null;
  profilePicture: string | null = null;

  constructor( private router: Router, private userService: UserService, private farmService: FarmService) { }

  ngOnInit() {
    this.userService.getProfile().subscribe({
      next: (user) => {
        this.displayName = `${user.firstName} ${user.lastName}`;
        if (user.profilePicture) {
          this.profilePicture = `data:image/jpeg;base64,${user.profilePicture}`;
        } else {
          this.profilePicture = 'assets/icons/no_profile_photo.svg';
        }
      },
      error: (err) => {
        console.error('Failed to fetch user name', err);
        this.displayName = 'Guest';
      }
    });

    this.farmService.farms$.subscribe((farms) => {
      this.farms = farms;
      this.selectedFarm = this.farmService.getSelectedFarm();
    });
  }

  @Input() showDevControls: boolean = false;
  @Output() logFarmsEvent = new EventEmitter<void>();
  @Output() loadFarmsEvent = new EventEmitter<void>();

  addNewFarm() {
    this.router.navigate(["new-farm"])
  }

  gotoSeeds() {
    this.router.navigate(["seeds"])
  }

  gotoHome() {
    this.router.navigate(["home"])
  }

  goToFeedback() {
    this.router.navigate(["feedback"])
  }

  selectFarm(farm: Farm) {
    this.selectedFarm = farm;
    this.farmService.selectFarm(farm);
    this.router.navigate(['home']);
  }

  onLogFarms(): void {
    this.logFarmsEvent.emit();
  }

  onLoadFarms(): void {
    this.loadFarmsEvent.emit();
  }

  gotoStatistic() {
    this.router.navigate(["statistic"])
  }
}
