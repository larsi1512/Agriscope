import {Injectable} from '@angular/core';
@Injectable({
  providedIn: 'root'
})
export class Globals {
  readonly backendUri: string = Globals.findBackendUrl();
  readonly wsUri: string = Globals.findWebSocketUrl();

  private static findBackendUrl(): string {
    if (window.location.port === '4200') { // local `ng serve`, backend at localhost:8080
      return 'http://localhost:8080/api';
    } else {
      // deployed: backend is available at same host via /api path
      return window.location.origin + '/api';
    }
  }

  private static findWebSocketUrl(): string {
    if (window.location.port === '4200') { // local `ng serve`, notification service at localhost:8085
      return 'http://localhost:8085';
    } else {
      // deployed: WebSocket goes through same host (api-gateway)
      return window.location.origin;
    }
  }
}


