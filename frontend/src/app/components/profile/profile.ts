import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {UserService} from '../../services/user-service/user-service';
import {EditUserDto, UserProfileDetail} from '../../dtos/user';
import {ToastrService} from 'ngx-toastr';
import {TopbarComponent} from '../topbar/topbar';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, TopbarComponent],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {

  showDiscardModal = false;

  // User data
  userData = {
    firstName: '',
    lastName: '',
    email: ''
  };

  // Original data (for comparison)
  originalData = { ...this.userData };

  // Password data
  passwordData = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  // Password visibility toggles
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  // Profile picture
  originalProfilePicture: string | null = null;
  profilePicture: string | null = null;

  // Menu toggle
  isMenuOpen = false;

  // Delete modal
  showDeleteModal = false;
  selectedFile: File | null = null;

  constructor(private router: Router, private userService: UserService, private toastr: ToastrService) {}

  ngOnInit() {
    // Load user data from API/service here
    const jwtToken = localStorage.getItem('authToken');
    if(!jwtToken) {
      console.error('JWT token not found in local storage');
      return;
    }
    this.loadUserData();
  }

  loadUserData() {
    this.userService.getProfile().subscribe({
      next: (data: UserProfileDetail) => {
        this.userData = {
          firstName: data.firstName,
          lastName: data.lastName,
          email: data.email
        };

        if (data.profilePicture) {
          this.profilePicture = `data:image/jpeg;base64,${data.profilePicture}`;
        } else {
          this.profilePicture = 'assets/icons/no_profile_photo.svg';
        }
        this.originalProfilePicture = this.profilePicture;
        this.originalData = { ...this.userData };
      },
      error: (err) => {
        console.error('Error loading profile:', err);
        if (err.status === 403 || err.status === 401) {
          //this.logout(); commented out for testing purposes
        }
      }
    });
  }

  // Toggle password visibility
  toggleCurrentPassword() {
    this.showCurrentPassword = !this.showCurrentPassword;
  }

  toggleNewPassword() {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPassword() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  // File upload
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      const maxSizeInBytes = 5 * 1024 * 1024;

      if (file.size > maxSizeInBytes) {
        this.toastr.error("File is too large. Maximum size is 5MB.", "Upload Error");

        event.target.value = '';
        return;
      }

      this.selectedFile = file;
      const reader = new FileReader();
      reader.onload = (e: any) => this.profilePicture = e.target.result;
      reader.readAsDataURL(file);
    }
  }

  // Check if specific field has changed
  hasChanges(field: keyof typeof this.userData): boolean {
    return this.userData[field] !== this.originalData[field];
  }

  // Check if any changes exist
  hasAnyChanges(): boolean {
    const dataChanged =
      this.userData.firstName !== this.originalData.firstName ||
      this.userData.lastName !== this.originalData.lastName;

    const passwordChanged =
      this.passwordData.currentPassword !== '' ||
      this.passwordData.newPassword !== '' ||
      this.passwordData.confirmPassword !== '';

    const pictureChanged = this.profilePicture !== this.originalProfilePicture;

    return dataChanged || passwordChanged || pictureChanged;
  }

  // Validate password change
  validatePasswordChange(): boolean {
    if (this.passwordData.newPassword || this.passwordData.confirmPassword) {
      if (!this.passwordData.currentPassword) {
        this.toastr.error('Please enter your current password');
        return false;
      }

      if (!this.passwordData.newPassword) {
        this.toastr.error('Please enter a new password');
        return false;
      }

      if (this.passwordData.newPassword.length < 6) {
        this.toastr.error('New password must be at least 6 characters');
        return false;
      }

      if (this.passwordData.newPassword !== this.passwordData.confirmPassword) {
        this.toastr.error('New passwords do not match');
        return false;
      }
    }

    return true;
  }

  onSave() {
    if (!this.validatePasswordChange()) {
      return;
    }
    const formData = new FormData();

    console.log('Saving profile data:', this.userData);
    const payload: EditUserDto = {
      firstName: this.userData.firstName,
      lastName: this.userData.lastName
    };

    if (this.passwordData.newPassword) {
      payload.oldPassword = this.passwordData.currentPassword;
      payload.newPassword = this.passwordData.newPassword;
    }

    this.originalData = { ...this.userData };
    formData.append('userData', new Blob([JSON.stringify(payload)], {
      type: 'application/json'
    }));
    if (this.selectedFile) {
      formData.append('file', this.selectedFile);
    }

    // Clear password fields
    this.passwordData = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    };


    this.userService.editProfile(formData).subscribe({
      next: (updatedUser) => {
        console.log('Update successful', updatedUser);

        this.userData.firstName = updatedUser.firstName;
        this.userData.lastName = updatedUser.lastName;

        this.originalData = { ...this.userData };
        this.originalProfilePicture = this.profilePicture;

        this.passwordData = {
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        };
        this.toastr.success('Profile updated successfully!');
      },
      error: (err) => {
        console.error('Update failed', err);

        if (err.status === 400) {
          this.toastr.error(typeof err.error === 'string' ? err.error : 'Update failed. Please check your inputs.');
        } else {
          this.toastr.error('An unexpected error occurred. Please try again.');
        }
      }
    });

  }

  onDiscard() {
    this.showDiscardModal = true;
  }
  confirmDiscard() {
    // Reset all data to original values
    this.userData = { ...this.originalData };
    this.passwordData = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
    this.profilePicture = this.originalProfilePicture;
    this.showDiscardModal = false;

    this.toastr.info('All changes have been discarded', 'Changes Discarded');
  }

  getChangedFields(): string[] {
    const changes: string[] = [];

    if (this.hasChanges('firstName')) changes.push('First Name');
    if (this.hasChanges('lastName')) changes.push('Last Name');
    if (this.passwordData.currentPassword || this.passwordData.newPassword) {
      changes.push('Password');
    }
    if (this.profilePicture !== this.originalProfilePicture) {
      changes.push('Profile Picture');
    }

    return changes;
  }

  goBack() {
    this.router.navigate(['/home']);
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  goToProfile() {
    this.isMenuOpen = false;
  }

  logout() {
    this.isMenuOpen = false;
    localStorage.removeItem('authToken');
    this.router.navigate(['/login']);
  }

  deleteProfile() {
    this.showDeleteModal = false;

    console.log('Deleting profile...');

    this.userService.deleteProfile().subscribe({
      next: (deletedUser) => {
        console.log('Deletion successful', deletedUser);
        this.toastr.success('Profile deleted successfully!');
        localStorage.removeItem("authToken");
        this.router.navigate(['/login']);

      },
      error: (err) => {
        console.error('Deletion failed', err);

        if (err.status === 400) {
          this.toastr.error(typeof err.error === 'string' ? err.error : 'Deletion failed.');
        } else {
          this.toastr.error('An unexpected error occurred. Please try again.');
        }
      }
    });
  }
}
