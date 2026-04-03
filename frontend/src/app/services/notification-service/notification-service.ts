import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Globals} from '../../global/globals';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  private notificationUri: string;

  constructor(private httpClient: HttpClient, private globals: Globals) {
    this.notificationUri = this.globals.backendUri + '/notifications';
  }

  getLatestWeather(farmId: string): Observable<any> {
    return this.httpClient.get<any>(`${this.notificationUri}/weather/latest/${farmId}`);
  }

  getAlertHistory(farmId: string, unreadOnly: boolean = true): Observable<any[]> {
    return this.httpClient.get<any[]>(`${this.notificationUri}/alerts/${farmId}?unreadOnly=${unreadOnly}`);
  }

  markAsRead(alertId: string): Observable<void> {
    return this.httpClient.put<void>(`${this.notificationUri}/alerts/${alertId}/read`, {});
  }
}
