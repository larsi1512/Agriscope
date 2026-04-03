import { Injectable } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import { Globals } from '../../global/globals';

declare var SockJS: any;

export interface WeatherData {
  user_id: string;
  time: string;
  farm_id: string;
  lat: number;
  lon: number;
  weather_code: number;
  temp: number;
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
export class WeatherWebSocketService {
  private serverUrl: string;
  private isConnecting = false;
  private stompClient: Client | null = null;
  private weatherSubject = new Subject<WeatherData>();
  private connectionStatusSubject = new BehaviorSubject<ConnectionStatus>(ConnectionStatus.DISCONNECTED);

  private currentUserId: string | null = null;
  private currentFarmId: string | null = null;

  private weatherSubscription: any = null;

  constructor(private globals: Globals) {
    this.serverUrl = this.globals.wsUri + '/ws-alerts';
  }

  setFarmForUser(userId: string, farmId: string): void {
    console.log(`Setting user ${userId}, farm ${farmId} - Connecting to WebSocket`);
    this.currentUserId = userId;
    this.currentFarmId = farmId;
    if (this.isConnected()) {
      console.log('Already connected, switching subscription to new farm...');
      this.subscribeToWeather(farmId);
    } else {
      this.connectionStatusSubject.next(ConnectionStatus.CONNECTING);
      this.connectToWebSocket(farmId);
    }
  }

  private connectToWebSocket(farmId: string): void {
    if (this.isConnecting || this.isConnected()) {
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
          console.log('STOMP Debug:', str);
        },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('Connected to Weather WebSocket server via SockJS');
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.CONNECTED);
          this.subscribeToWeather(farmId);
        },
        onStompError: (frame) => {
          console.error('STOMP Error:', frame);
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.ERROR);
        },
        onWebSocketError: (event) => {
          console.error('WebSocket Error:', event);
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.ERROR);
        },
        onDisconnect: () => {
          console.log('Disconnected from WebSocket');
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.DISCONNECTED);
        }
      });

      this.stompClient.activate();

    } catch (error) {
      console.error('Failed to connect:', error);
      this.isConnecting = false;
      this.connectionStatusSubject.next(ConnectionStatus.ERROR);
    }
  }


  private subscribeToWeather(farmId: string): void {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('Cannot subscribe: not connected');
      return;
    }

    if (this.weatherSubscription) {
      console.log('Unsubscribing from previous farm weather...');
      this.weatherSubscription.unsubscribe();
      this.weatherSubscription = null;
    }

    const topic = `/topic/weather/${farmId}`;
    console.log(`Subscribing to weather for farm: ${topic}`);

    this.stompClient.subscribe(topic, (message) => {
      try {
        const weatherData: WeatherData = JSON.parse(message.body);

        if (weatherData.farm_id === this.currentFarmId) {
          this.weatherSubject.next(weatherData);
        }
      } catch (error) {
        console.error('Error parsing weather data:', error);
      }
    });
  }

  /**
   * Get weather updates observable
   */
  getWeatherUpdates(): Observable<WeatherData> {
    return this.weatherSubject.asObservable();
  }

  /**
   * Get connection status observable
   */
  getConnectionStatus(): Observable<ConnectionStatus> {
    return this.connectionStatusSubject.asObservable();
  }

  /**
   * Reconnect manually
   */
  reconnect(): void {
    console.log('Manual reconnect');

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

  /**
   * Disconnect
   */
  public disconnect(): void {
    console.log('Disconnecting WebSocket service');

    if (this.stompClient) {
      try {
        this.stompClient.deactivate();
        console.log('WebSocket deactivated');
      } catch (error) {
        console.error('Error deactivating STOMP client:', error);
      }
      this.stompClient = null;
    }

    this.connectionStatusSubject.next(ConnectionStatus.DISCONNECTED);
    this.isConnecting = false;
  }

  /**
   * Check if connected
   */
  public isConnected(): boolean {
    return this.stompClient?.connected || false;
  }
}
