import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { AlertsWebSocketService, AlertData, ConnectionStatus } from '../../services/websocket-service/ alert-websocket.service';
import { NotificationService } from '../../services/notification-service/notification-service';
import { UserService } from '../../services/user-service/user-service';
import { FarmService } from '../../services/farm-service/farm-service';

import { Farm } from '../../models/Farm';

interface AlertTypeInfo {
  icon: string;
  color: string;
  label: string;
}

@Component({
  selector: 'app-alerts-notification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alerts-notification.html',
  styleUrl: './alerts-notification.css',
})
export class AlertsNotification implements OnInit, OnDestroy {
  alerts: AlertData[] = [];
  isDropdownOpen = false;
  connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED;

  private userId: string | null = null;
  private currentFarm: Farm | null = null;

  private alertsSubscription?: Subscription;
  private listSubscription?: Subscription;
  private statusSubscription?: Subscription;
  private mainSubscription?: Subscription;

  constructor(
    private alertsService: AlertsWebSocketService,
    private userService: UserService,
    private farmService: FarmService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.userService.getProfile().pipe(take(1)).subscribe({
      next: (profile) => {
        if (profile && profile.email) {
          console.log('User email set as ID:', profile.email);
          this.userId = profile.email;

          this.setupWebSocketSubscriptions();
          this.subscribeToFarmUpdates();
        }
      },
      error: (err) => console.error('Error fetching profile:', err)
    });
  }

  private subscribeToFarmUpdates() {
    this.mainSubscription = this.farmService.selectedFarm$.subscribe(farm => {
      if (farm && farm.id && this.userId) {
        console.log('Farm switched/loaded:', farm.name);
        this.currentFarm = farm;

        this.alertsService.setFarmForUser(this.userId, farm.id);

        this.loadHistoryFromBackend(farm.id);
      }
    });
  }

  ngOnDestroy() {
    this.cleanupSubscriptions();
    this.disconnectWebSocket();
  }

  private loadHistoryFromBackend(farmId: string) {
    this.notificationService.getAlertHistory(farmId).subscribe({
      next: (history) => {
        console.log('Loaded alerts history:', history.length);

        const alertsHistory = history.filter(h =>
          h.recommendationType.includes('FROST_ALERT') ||
          h.recommendationType === 'IRRIGATE_NOW' ||
          h.recommendationType === 'HEAT_ALERT'   ||
          h.recommendationType === 'SAFETY_ALERT' ||
          h.recommendationType === 'STORM_ALERT'
        );

        const mappedHistory = alertsHistory.map(h => ({
          id: h.id,
          userId: h.userId,
          farmId: h.farmId,
          recommendedSeed: h.recommendedSeed || h.seedType || 'Seed',
          recommendationType: h.recommendationType,
          advice: h.message,
          reasoning: h.reasoning,
          weatherTimestamp: h.createdAt,
          metrics: h.metrics ? h.metrics : { temperature: h.temperature },
          receivedAt: h.createdAt,
          isRead: h.read
        }));

        this.alerts = mappedHistory;
      },
      error: (err) => console.error('Failed to load history', err)
    });
  }

  private setupWebSocketSubscriptions() {
    this.statusSubscription = this.alertsService.getConnectionStatus()
      .subscribe(status => this.connectionStatus = status);

    this.listSubscription = this.alertsService.getAllAlerts()
      .subscribe(alerts => {
      });

    this.alertsSubscription = this.alertsService.getAlertUpdates()
      .subscribe(newAlert => {
        this.alerts = [newAlert, ...this.alerts];
      });
  }


  private reconnectWebSocket(): void {
    console.log('Reconnecting alerts WebSocket...');
    this.alertsService.reconnect();
  }

  private disconnectWebSocket(): void {
    console.log('Disconnecting alerts WebSocket...');
    this.alertsService.disconnect();
  }

  private cleanupSubscriptions(): void {
    if (this.alertsSubscription) this.alertsSubscription.unsubscribe();
    if (this.listSubscription) this.listSubscription.unsubscribe();
    if (this.statusSubscription) this.statusSubscription.unsubscribe();
    if (this.mainSubscription) this.mainSubscription.unsubscribe();
  }

  getAlertsCount(): number {
    return this.alerts.length;
  }

  hasAlerts(): boolean {
    return this.alerts.length > 0;
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  closeDropdown(): void {
    this.isDropdownOpen = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.alerts-notification-wrapper')) {
      this.closeDropdown();
    }
  }

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

  getAlertTypeInfo(type: string): AlertTypeInfo {
    const typeMap: { [key: string]: AlertTypeInfo } = {
      'FROST_ALERT': { icon: '❄️', color: '#3B82F6', label: 'Frost Alert' },
      'HEAT_ALERT': { icon: '🌡️', color: '#EF4444', label: 'Heat Alert' },
      'SAFETY_ALERT': { icon: '⚠️', color: '#EF4444', label: 'Safety Alert' },
      'STORM_ALERT': { icon: '⛈️', color: '#1F2937', label: 'Storm Alert' },
      'IRRIGATE_NOW': { icon: '💧', color: '#2563EB', label: 'Irrigate Now' },
    };

    return typeMap[type] || {
      icon: '🚨',
      color: '#EF4444',
      label: 'Alert'
    };
  }

  formatSeedName(seedName: string | undefined): string {
    if (!seedName) return '';
    let formatted = seedName.replace(/_/g, ' ');
    formatted = formatted.split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
    return formatted;
  }

  formatExactTime(dateInput?: string | Date): string {
    if (!dateInput) return '';
    const date = new Date(dateInput);
    return date.toLocaleString('en-GB', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatAdviceTitle(advice: string): string {
    if (!advice) return '';
    let formatted = advice.replace(/_/g, ' ');
    formatted = formatted.split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
    return formatted;
  }

  getTimeSince(alert: AlertData): string {
    if (!alert.receivedAt) return 'Just now';
    const now = new Date();
    const received = new Date(alert.receivedAt);
    const diffMs = now.getTime() - received.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  }

  formatTemperature(temp?: number): string {
    if (temp === undefined) return 'N/A';
    return `${temp.toFixed(2)}°C`;
  }

  formatReasoning(reasoning: string | undefined): string {
    if (!reasoning) return '';

    return reasoning.replace(/(-?\d+\.\d+)°?C/g, (match, temp) => {
      const rounded = parseFloat(temp).toFixed(2);
      return `${rounded}°C`;
    });
  }
  dismissAlert(alertId: string): void {
    this.notificationService.markAsRead(alertId).subscribe({
      next: () => {
        console.log(`Alert ${alertId} marked as read in DB`);

        this.alertsService.removeAlert(alertId);
        this.alerts = this.alerts.filter(a => a.id !== alertId);
      },
      error: (err) => console.error('Failed to mark alert as read', err)
    });
  }

  isIrrigationAlert(type: string): boolean {
    return type === 'IRRIGATE_NOW' || type === 'IRRIGATE_SOON' || type === 'DELAY_IRRIGATION';
  }

  formatDeficit(alert: AlertData): string {
    let val = alert.metrics?.deficit_amount;

    if (val === undefined || val === null) {
      const match = alert.reasoning?.match(/Deficit:\s*([\d.]+)\s*mm/i);

      if (match && match[1]) {
        return `${match[1]} mm`;
      }
    } else {
      return `${Number(val).toFixed(1)} mm`;
    }

    return 'N/A';
  }

  clearAllAlerts(): void {
    this.alerts.forEach(alert => {
      this.notificationService.markAsRead(alert.id).subscribe();
    });

    this.alertsService.clearAlerts();
    this.alerts = [];
    this.closeDropdown();
  }

}
