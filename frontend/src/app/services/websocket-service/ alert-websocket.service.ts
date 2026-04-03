import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import { Globals } from '../../global/globals';

declare var SockJS: any;

export interface AlertData {
  id: string;
  userId: string;
  farmId: string;
  recommendedSeed: string;
  recommendationType: string;
  advice: string;
  reasoning: string;
  weatherTimestamp: string;
  metrics: {
    temperature?: number;
    deficit_amount?: number;
    [key: string]: any;
  };
  receivedAt?: Date;
}

export enum ConnectionStatus {
  CONNECTED = 'CONNECTED',
  CONNECTING = 'CONNECTING',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR'
}

@Injectable({
  providedIn: 'root'
})
export class AlertsWebSocketService implements OnDestroy {
  private serverUrl: string;
  private isConnecting = false;
  private stompClient: Client | null = null;
  private alertsSubject = new Subject<AlertData>();
  private connectionStatusSubject = new BehaviorSubject<ConnectionStatus>(ConnectionStatus.DISCONNECTED);

  private allAlerts: AlertData[] = [];
  private alertsListSubject = new BehaviorSubject<AlertData[]>([]);

  private currentUserId: string | null = null;
  private currentFarmId: string | null = null;

  constructor(private globals: Globals) {
    this.serverUrl = this.globals.wsUri + '/ws-alerts';
  }

  setFarmForUser(userId: string, farmId: string): void {
    console.log(`Alerts: Setting user ${userId}, farm ${farmId} - Connecting to WebSocket`);
    this.currentUserId = userId;
    this.currentFarmId = farmId;
    if (this.isConnected()) {
      console.log('Already connected to alerts, switching subscription...');
      this.disconnect();
      setTimeout(() => this.connectToWebSocket(farmId), 100);
    } else {
      this.connectionStatusSubject.next(ConnectionStatus.CONNECTING);
      this.connectToWebSocket(farmId);
    }
  }

  private sendAck(alertId: string, farmId: string): void {
    if (this.stompClient && this.stompClient.connected) {
      const ackPayload = {
        recommendationId: alertId,
        farmId: farmId,
        status: 'RECEIVED'
      };

      console.log('Sending ACK for alert:', alertId);

      this.stompClient.publish({
        destination: '/app/notification/ack',
        body: JSON.stringify(ackPayload)
      });
    }
  }

  private connectToWebSocket(farmId: string): void {
    if (this.isConnecting || this.stompClient?.connected) {
      console.log('Already connecting or connected');
      return;
    }

    try {
      this.isConnecting = true;

      if (typeof SockJS === 'undefined') {
        throw new Error('SockJS is not loaded');
      }

      const socket = new SockJS(this.serverUrl);

      this.stompClient = new Client({
        webSocketFactory: () => socket,
        debug: (str) => {
          console.log('STOMP Debug (Alerts):', str);
        },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('Connected to Alerts WebSocket server via SockJS');
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.CONNECTED);
          this.subscribeToAlerts(farmId);
        },
        onStompError: (frame) => {
          console.error('STOMP Error (Alerts):', frame);
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.ERROR);
        },
        onWebSocketError: (event) => {
          console.error('WebSocket Error (Alerts):', event);
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.ERROR);
        },
        onDisconnect: () => {
          console.log('Disconnected from Alerts WebSocket');
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.DISCONNECTED);
        }
      });

      this.stompClient.activate();

    } catch (error) {
      console.error('Failed to connect to Alerts WebSocket:', error);
      this.isConnecting = false;
      this.connectionStatusSubject.next(ConnectionStatus.ERROR);
    }
  }

  private subscribeToAlerts(farmId: string): void {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('Cannot subscribe: not connected');
      return;
    }

    const topic = `/topic/alerts/${farmId}`;
    console.log(`Subscribing to alerts for farm: ${topic}`);

    this.stompClient.subscribe(topic, (message) => {
      try {
        const alert: AlertData = JSON.parse(message.body);
        alert.receivedAt = new Date();

        console.log('Alert received:', alert);

        if (alert.farmId === this.currentFarmId) {
          this.allAlerts.push(alert);
          this.alertsListSubject.next([...this.allAlerts]);
          this.alertsSubject.next(alert);
          this.sendAck(alert.id, alert.farmId);
        }
      } catch (error) {
        console.error('Error parsing alert:', error);
      }
    });
  }

  getAlertUpdates(): Observable<AlertData> {
    return this.alertsSubject.asObservable();
  }

  getAllAlerts(): Observable<AlertData[]> {
    return this.alertsListSubject.asObservable();
  }

  getConnectionStatus(): Observable<ConnectionStatus> {
    return this.connectionStatusSubject.asObservable();
  }

  clearAlerts(): void {
    this.allAlerts = [];
    this.alertsListSubject.next([]);
  }

  removeAlert(id: string): void {
    this.allAlerts = this.allAlerts.filter(a => a.id !== id);
    this.alertsListSubject.next([...this.allAlerts]);
  }

  reconnect(): void {
    console.log('Manual reconnect for Alerts');

    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }

    this.connectionStatusSubject.next(ConnectionStatus.DISCONNECTED);

    if (this.currentFarmId && this.currentUserId) {
      setTimeout(() => {
        this.connectToWebSocket(this.currentFarmId!);
      }, 1000);
    }
  }

  disconnect(): void {
    console.log('Disconnecting Alerts WebSocket service');

    if (this.stompClient) {
      try {
        this.stompClient.deactivate();
        console.log('Alerts WebSocket deactivated');
      } catch (error) {
        console.error('Error deactivating STOMP client:', error);
      }
      this.stompClient = null;
    }

    this.connectionStatusSubject.next(ConnectionStatus.DISCONNECTED);
    this.isConnecting = false;
  }

  isConnected(): boolean {
    return this.stompClient?.connected || false;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.alertsSubject.complete();
    this.alertsListSubject.complete();
    this.connectionStatusSubject.complete();
  }
}
