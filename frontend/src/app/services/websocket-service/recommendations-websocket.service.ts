import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { Client } from '@stomp/stompjs';
import { Globals } from '../../global/globals';

declare var SockJS: any;

export interface RecommendationData {
  id: string;
  userId: string;
  farmId: string;
  recommendedSeed: string;
  recommendationType: string;
  advice: string;
  reasoning: string;
  weatherTimestamp: string;
  metrics: {
    deficit_amount?: number;
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
export class RecommendationsWebSocketService implements OnDestroy {
  private serverUrl: string;
  private isConnecting = false;
  private stompClient: Client | null = null;
  private recommendationsSubject = new Subject<RecommendationData>();
  private connectionStatusSubject = new BehaviorSubject<ConnectionStatus>(ConnectionStatus.DISCONNECTED);

  private allRecommendations: RecommendationData[] = [];
  private recommendationsListSubject = new BehaviorSubject<RecommendationData[]>([]);

  private currentUserId: string | null = null;
  private currentFarmId: string | null = null;

  constructor(private globals: Globals) {
    this.serverUrl = this.globals.wsUri + '/ws-alerts';
  }

  setFarmForUser(userId: string, farmId: string): void {
    console.log(`Recommendations: Setting user ${userId}, farm ${farmId} - Connecting to WebSocket`);
    this.currentUserId = userId;
    this.currentFarmId = farmId;
    this.connectionStatusSubject.next(ConnectionStatus.CONNECTING);
    this.connectToWebSocket(farmId);
  }

  private sendAck(recId: string, farmId: string): void {
    if (this.stompClient && this.stompClient.connected) {
      const ackPayload = {
        recommendationId: recId,
        farmId: farmId,
        status: 'RECEIVED'
      };

      console.log('Sending ACK for recommendation:', recId);

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
          console.log('STOMP Debug (Recommendations):', str);
        },
        reconnectDelay: 5000,
        onConnect: () => {
          console.log('Connected to Recommendations WebSocket server via SockJS');
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.CONNECTED);
          this.subscribeToRecommendations(farmId);
        },
        onStompError: (frame) => {
          console.error('STOMP Error (Recommendations):', frame);
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.ERROR);
        },
        onWebSocketError: (event) => {
          console.error('WebSocket Error (Recommendations):', event);
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.ERROR);
        },
        onDisconnect: () => {
          console.log('Disconnected from Recommendations WebSocket');
          this.isConnecting = false;
          this.connectionStatusSubject.next(ConnectionStatus.DISCONNECTED);
        }
      });

      this.stompClient.activate();

    } catch (error) {
      console.error('Failed to connect to Recommendations WebSocket:', error);
      this.isConnecting = false;
      this.connectionStatusSubject.next(ConnectionStatus.ERROR);
    }
  }

  private subscribeToRecommendations(farmId: string): void {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('Cannot subscribe: not connected');
      return;
    }

    const topic = `/topic/recommendations/${farmId}`;
    console.log(`Subscribing to recommendations for farm: ${topic}`);

    this.stompClient.subscribe(topic, (message) => {
      try {
        const recommendation: RecommendationData = JSON.parse(message.body);
        recommendation.receivedAt = new Date();

        console.log('Recommendation received:', recommendation);

        if (recommendation.farmId === this.currentFarmId) {
          this.allRecommendations.push(recommendation);
          this.recommendationsListSubject.next([...this.allRecommendations]);
          this.recommendationsSubject.next(recommendation);
          this.sendAck(recommendation.id, recommendation.farmId);
        }
      } catch (error) {
        console.error('Error parsing recommendation:', error);
      }
    });
  }

  getRecommendationUpdates(): Observable<RecommendationData> {
    return this.recommendationsSubject.asObservable();
  }

  getAllRecommendations(): Observable<RecommendationData[]> {
    return this.recommendationsListSubject.asObservable();
  }


  getConnectionStatus(): Observable<ConnectionStatus> {
    return this.connectionStatusSubject.asObservable();
  }

  clearRecommendations(): void {
    this.allRecommendations = [];
    this.recommendationsListSubject.next([]);
  }

  removeRecommendation(id: string): void {
    this.allRecommendations = this.allRecommendations.filter(r => r.id !== id);
    this.recommendationsListSubject.next([...this.allRecommendations]);
  }

  reconnect(): void {
    console.log('Manual reconnect for Recommendations');

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
    console.log('Disconnecting Recommendations WebSocket service');

    if (this.stompClient) {
      try {
        this.stompClient.deactivate();
        console.log('Recommendations WebSocket deactivated');
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
    this.recommendationsSubject.complete();
    this.recommendationsListSubject.complete();
    this.connectionStatusSubject.complete();
  }
}
