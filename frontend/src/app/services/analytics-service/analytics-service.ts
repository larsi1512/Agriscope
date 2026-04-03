import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Globals } from '../../global/globals';

export interface FeedbackStats {
  [key: string]: number;
}

export interface DashboardAnalytics {
  alertDistribution: {
    [alertType: string]: number;
  };
  cropVulnerability: {
    [cropType: string]: number;
  };
  waterSavings: {
    actionsTaken: number;
    actionsSaved: number;
  };
}

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  private farmAnalyticsBaseUrl: string;
  private notificationAnalyticsBaseUrl: string;

  constructor(private httpClient: HttpClient, private globals: Globals) {
    this.farmAnalyticsBaseUrl = this.globals.backendUri + '/farm-analytics';
    this.notificationAnalyticsBaseUrl = this.globals.backendUri + '/analytics';
  }

  getFeedbackStats(farmId: string): Observable<FeedbackStats> {
    return this.httpClient.get<FeedbackStats>(
      `${this.farmAnalyticsBaseUrl}/feedback-stats/${farmId}`
    );
  }

  getDashboardAnalytics(farmId: string): Observable<DashboardAnalytics> {
    return this.httpClient.get<DashboardAnalytics>(
      `${this.notificationAnalyticsBaseUrl}/dashboard/${farmId}`
    );
  }
}
