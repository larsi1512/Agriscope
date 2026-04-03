import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import {Subscription} from 'rxjs';
import { take } from 'rxjs/operators';
import { RecommendationsWebSocketService, RecommendationData, ConnectionStatus } from '../../services/websocket-service/recommendations-websocket.service';
import { NotificationService } from '../../services/notification-service/notification-service';
import { UserService } from '../../services/user-service/user-service';
import { FarmService } from '../../services/farm-service/farm-service';

import { Farm } from '../../models/Farm';


interface RecommendationTypeInfo {
  icon: string;
  color: string;
  label: string;
}

@Component({
  selector: 'app-recommendations',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recommendations.html',
  styleUrl: './recommendations.css',
})
export class Recommendations implements OnInit, OnDestroy {
  recommendations: RecommendationData[] = [];
  currentRecommendationIndex = 0;
  isModalOpen = false;

  connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED;

  private userId: string | null = null;
  private currentFarm: Farm | null = null;


  private recommendationsSubscription?: Subscription;
  private listSubscription?: Subscription;
  private statusSubscription?: Subscription;
  private mainSubscription?: Subscription;

  constructor(
    private recommendationsService: RecommendationsWebSocketService,
    private userService: UserService,
    private farmService: FarmService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.setupWebSocketSubscriptions();
    this.initializeDataStream();
  }

  ngOnDestroy() {
    this.cleanupSubscriptions();
    this.disconnectWebSocket();
  }

  private initializeDataStream() {
    this.userService.getProfile().pipe(
      take(1)
    ).subscribe({
      next: (userProfile) => {
        const uniqueId =  userProfile.email;

        if (uniqueId) {
          this.userId = uniqueId;
          console.log('Recommendations: User identified:', this.userId);

          this.subscribeToFarmChanges();
        } else {
          console.error('Recommendations: Could not load User Identifier');
        }
      },
      error: (err) => {
        console.error('Recommendations: Failed to fetch profile', err);
      }
    });

    this.mainSubscription = this.farmService.selectedFarm$.subscribe(farm => {
      if (farm && farm.id) {
        this.currentFarm = farm;

        this.notificationService.getAlertHistory(farm.id).subscribe({
          next: (history) => {
            const recsHistory = history.filter(h =>
              h.recommendationType === 'IRRIGATE_SOON' ||
              h.recommendationType === 'DELAY_IRRIGATION'  ||
              h.recommendationType === 'MONITOR_CONDITIONS'  ||
              h.recommendationType === 'CONTINUE_NORMAL'  ||
              h.recommendationType === 'DELAY_OPERATIONS'  ||
              h.recommendationType === 'DISEASE_PREVENTION'  ||
              h.recommendationType === 'NUTRIENT_CHECK'  ||
              h.recommendationType === 'PLANNING_ALERT'  ||
              h.recommendationType === 'HEAT_STRESS_PREVENTION'  ||
              h.recommendationType === 'PEST_RISK' ||
              h.recommendationType === 'READY_TO_HARVEST'
            );

            this.recommendations = recsHistory.map(h => ({
              id: h.id,
              userId: h.userId,
              farmId: h.farmId,
              recommendedSeed: h.recommendedSeed || h.seedType || 'Seed',
              recommendationType: h.recommendationType,
              advice: h.message,
              reasoning: h.reasoning,
              weatherTimestamp: h.createdAt,
              metrics: {},
              receivedAt: h.createdAt
            }));
          }
        });
      }
    });
  }

  private subscribeToFarmChanges() {
    this.mainSubscription = this.farmService.selectedFarm$.subscribe(farm => {
      if (farm && farm.id) {
        console.log('Recommendations: Farm changed to', farm.name);

        if (this.currentFarm && this.currentFarm.id !== farm.id) {
          this.recommendationsService.disconnect();
          this.recommendationsService.clearRecommendations();
          this.recommendations = [];
          this.closeModal();
        }

        this.currentFarm = farm;
        this.connectWebSocket();
      } else {
        console.log('Recommendations: No farm selected');
        this.recommendationsService.disconnect();
        this.recommendations = [];
        this.closeModal();
      }
    });
  }

  private setupWebSocketSubscriptions(): void {
    this.recommendationsSubscription = this.recommendationsService.getRecommendationUpdates().subscribe({
      next: (data: RecommendationData) => {
        if (this.currentFarm && data.farmId === this.currentFarm.id) {
          this.recommendations.unshift(data);
          console.log('New recommendation received and added:', data);
        }
      },
      error: (error) => console.error('Error in recommendation updates:', error)
    });

    this.statusSubscription = this.recommendationsService.getConnectionStatus().subscribe({
      next: (status: ConnectionStatus) => {
        this.connectionStatus = status;
        console.log('Recommendations connection status:', status);

        if (status === ConnectionStatus.DISCONNECTED && this.userId && this.currentFarm) {
          setTimeout(() => {
            if (this.connectionStatus === ConnectionStatus.DISCONNECTED) {
              this.reconnectWebSocket();
            }
          }, 5000);
        }
      },
      error: (error) => console.error('Error in connection status:', error)
    });
  }

  private connectWebSocket(): void {
    if (!this.userId || !this.currentFarm) return;

    console.log(`Connecting recommendations for user ${this.userId}, farm ${this.currentFarm.id}`);

    try {
      this.recommendationsService.setFarmForUser(this.userId, this.currentFarm.id);
    } catch (error) {
      console.error('Error connecting recommendations WebSocket:', error);
    }
  }

  private reconnectWebSocket(): void {
    console.log('Reconnecting recommendations WebSocket...');
    this.recommendationsService.reconnect();
  }

  private disconnectWebSocket(): void {
    this.recommendationsService.disconnect();
  }

  private cleanupSubscriptions(): void {
    if (this.recommendationsSubscription) this.recommendationsSubscription.unsubscribe();
    if (this.listSubscription) this.listSubscription.unsubscribe();
    if (this.statusSubscription) this.statusSubscription.unsubscribe();
    if (this.mainSubscription) this.mainSubscription.unsubscribe();
  }

  /**
   * Format seed name: remove underscores and capitalize properly
   */
  formatSeedName(seedName: string | undefined): string {
    if (!seedName) return '';

    let formatted = seedName.replace(/_/g, ' ');

    formatted = formatted.split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');

    return formatted;
  }
  /**
   * Get the count of recommendations
   */
  getRecommendationsCount(): number {
    return this.recommendations.length;
  }

  getExactDate(recommendation: RecommendationData): string {
    if (!recommendation.receivedAt) return '';

    const date = new Date(recommendation.receivedAt);

    return date.toLocaleString('en-GB', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Check if there are any recommendations
   */
  hasRecommendations(): boolean {
    return this.recommendations.length > 0;
  }

  /**
   * Open the details modal
   */
  openModal(): void {
    if (this.hasRecommendations()) {
      this.currentRecommendationIndex = 0;
      this.isModalOpen = true;
    }
  }

  /**
   * Close the modal
   */
  closeModal(): void {
    this.isModalOpen = false;
  }

  /**
   * Get current recommendation being displayed
   */
  getCurrentRecommendation(): RecommendationData | null {
    if (this.recommendations.length === 0) return null;
    return this.recommendations[this.currentRecommendationIndex];
  }

  /**
   * Navigate to next recommendation
   */
  nextRecommendation(): void {
    if (this.currentRecommendationIndex < this.recommendations.length - 1) {
      this.currentRecommendationIndex++;
    }
  }

  /**
   * Navigate to previous recommendation
   */
  previousRecommendation(): void {
    if (this.currentRecommendationIndex > 0) {
      this.currentRecommendationIndex--;
    }
  }

  /**
   * Check if there's a next recommendation
   */
  hasNext(): boolean {
    return this.currentRecommendationIndex < this.recommendations.length - 1;
  }

  /**
   * Check if there's a previous recommendation
   */
  hasPrevious(): boolean {
    return this.currentRecommendationIndex > 0;
  }

  /**
   * Get seed icon path
   */
  getSeedIcon(seedType: string): string {
    const seed = seedType.toLowerCase();
    const iconMap: { [key: string]: string } = {
      'wheat': 'assets/icons/wheat.svg',
      'corn': 'assets/icons/corn.svg',
      'barley': 'assets/icons/barely.svg',
      'white_grapes': 'assets/icons/white_grape.svg',
      'black_grapes': 'assets/icons/grape.svg',
      'pumpkin': 'assets/icons/pumpkin.svg'
    };

    return iconMap[seed] || 'assets/icons/wheat.svg';
  }

  /**
   * Get recommendation type information (icon, color, label)
   */
  getRecommendationTypeInfo(type: string): RecommendationTypeInfo {
    const typeMap: { [key: string]: RecommendationTypeInfo } = {
      'MONITOR_CONDITIONS': { icon: '👁️', color: '#10B981', label: 'Monitor Conditions' },
      'CONTINUE_NORMAL': { icon: '✅', color: '#10B981', label: 'Continue Normal' },
      'DELAY_OPERATIONS': { icon: '⏸️', color: '#F97316', label: 'Delay Operations' },

      'IRRIGATE_SOON': { icon: '💧', color: '#3B82F6', label: 'Irrigate Soon' },
      'DELAY_IRRIGATION': { icon: '⏳', color: '#F59E0B', label: 'Delay Irrigation' },
      'DISEASE_PREVENTION': { icon: '🦠', color: '#8B5CF6', label: 'Disease Risk' },
      'PEST_RISK': { icon: '🐛', color: '#EF4444', label: 'Pest Risk' },
      'NUTRIENT_CHECK': { icon: '🧪', color: '#10B981', label: 'Nutrient Check' },
      'PLANNING_ALERT': { icon: '📅', color: '#6366F1', label: 'Planning Advice' },
      'HEAT_STRESS_PREVENTION': { icon: '☂️', color: '#F97316', label: 'Heat Prevention' },
      'READY_TO_HARVEST': { icon: '🚜', color: '#EAB308', label: 'Ready to Harvest' }
    };

    return typeMap[type] || {
      icon: 'ℹ️',
      color: '#6B7280',
      label: 'General Advice'
    };
  }

  /**
   * Get farmer warning icon based on recommendation type
   */
  getRecommendationIcon(type: string): string {
    const iconMap: { [key: string]: string } = {
      'FROST_ALERT': 'assets/icons/frozen_farmer.svg',
      'HEAT_ALERT': 'assets/icons/heated_farmer.svg',
      'CONTINUE_NORMAL': 'assets/icons/happy_farmer.svg',
      'MONITOR_CONDITIONS': 'assets/icons/monitoring_farmer.svg',
      'SAFETY_ALERT': 'assets/icons/alert_farmer.svg',
      'DELAY_OPERATIONS': 'assets/icons/farmer_delayed.svg',
      'IRRIGATE_NOW': 'assets/icons/farmer_irrigate.svg',
      'IRRIGATION_NEEDED': 'assets/icons/farmer_irrigate.svg',
      'RAIN_ALERT': 'assets/icons/rainy_farmer.svg',
      'READY_TO_HARVEST': 'assets/icons/happy_farmer.svg',
      'DISEASE_PREVENTION': 'assets/icons/alert_farmer.svg',
      'PEST_RISK': 'assets/icons/alert_farmer.svg'
    };

    return iconMap[type] || 'assets/icons/alert_farmer.svg';
  }

  /**
   * Format advice title: replace underscores with spaces and add exclamation
   */
  formatAdviceTitle(advice: string): string {
    if (!advice) return '';

    let formatted = advice.replace(/_/g, ' ');

    formatted = formatted.split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');

    return formatted;
  }

  /**
   * Calculate time since recommendation was received
   */
  getTimeSince(recommendation: RecommendationData): string {
    if (!recommendation.receivedAt) return 'Just now';

    const now = new Date();
    const received = new Date(recommendation.receivedAt);
    const diffMs = now.getTime() - received.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  }

  /**
   * Clear all recommendations
   */
  clearAllRecommendations(): void {
    this.recommendations.forEach(rec => {
      this.notificationService.markAsRead(rec.id).subscribe();
    });

    this.recommendationsService.clearRecommendations();
    this.recommendations = [];
    this.closeModal();
  }

  formatExactTime(dateInput?: string | Date): string {
    if (!dateInput) return '';
    const date = new Date(dateInput);
    return date.toLocaleTimeString([], {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit' });
  }

  /**
   * Delete current recommendation
   */
  deleteCurrentRecommendation(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }

    const current = this.getCurrentRecommendation();
    if (current) {
      this.notificationService.markAsRead(current.id).subscribe({
        next: () => {
          console.log(`Recommendation ${current.id} marked as read/deleted`);

          this.recommendations = this.recommendations.filter(r => r.id !== current.id);

          if (this.currentRecommendationIndex >= this.recommendations.length) {
            this.currentRecommendationIndex = Math.max(0, this.recommendations.length - 1);
          }

          if (this.recommendations.length === 0) {
            this.closeModal();
          }
        },
        error: (err) => console.error('Error deleting recommendation:', err)
      });
    }
  }
}
