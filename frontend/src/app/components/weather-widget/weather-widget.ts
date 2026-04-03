import { Component, OnInit, OnDestroy} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { NotificationService } from '../../services/notification-service/notification-service';
import { WeatherWebSocketService, WeatherData, ConnectionStatus } from '../../services/websocket-service/weather-websocket.service';
import { UserService } from '../../services/user-service/user-service';
import { FarmService } from '../../services/farm-service/farm-service';

import { Farm } from '../../models/Farm';

interface WeatherInfo {
  description: string;
  icon: string;
  background: string;
}

@Component({
  selector: 'app-weather-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './weather-widget.html',
  styleUrl: './weather-widget.css',
})
export class WeatherWidget implements OnInit, OnDestroy {
  weatherData: WeatherData | null = null;
  locationName: string = 'Loading location...';
  currentDate: Date = new Date();
  lastUpdated: Date | null = null;
  weatherInfo: WeatherInfo | null = null;

  connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED;
  private wsSubscription?: Subscription;
  private statusSubscription?: Subscription;
  private mainSubscription?: Subscription;
  private dateInterval?: any;


  private userId: string | null = null;
  private currentFarm: Farm | null = null;


  constructor(
    private weatherService: WeatherWebSocketService,
    private userService: UserService,
    private farmService: FarmService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.dateInterval = setInterval(() => {
      this.currentDate = new Date();
    }, 60000);

    this.statusSubscription = this.weatherService.getConnectionStatus().subscribe({
      next: (status) => {
        this.connectionStatus = status;
        if (status === ConnectionStatus.DISCONNECTED && this.userId && this.currentFarm) {
          console.log('Connection lost. Attempting to reconnect in 5s...');
          setTimeout(() => {
            if (this.connectionStatus === ConnectionStatus.DISCONNECTED) {
              this.reconnectWebSocket();
            }
          }, 5000);
        }
      }
    });

    this.wsSubscription = this.weatherService.getWeatherUpdates().subscribe({
      next: (data: WeatherData) => {
        if (this.currentFarm && data.farm_id === this.currentFarm.id) {
          this.handleWeatherData(data);
        }
      },
      error: (err) => console.error('Weather update error:', err)
    });

    this.initializeDataStream();
  }

  private initializeDataStream() {
    this.userService.getProfile().pipe(
      take(1)
    ).subscribe({
      next: (userProfile) => {
        if (userProfile && userProfile.email) {
          this.userId = userProfile.email;
          console.log('WeatherWidget: User identified via email:', this.userId);

          this.subscribeToFarmChanges();
        } else {
          console.error('WeatherWidget: Could not load User Email');
          this.locationName = 'Error loading user';
        }
      },
      error: (err) => {
        console.error('WeatherWidget: Failed to fetch profile', err);
        this.locationName = 'Please log in';
      }
    });
  }

  private subscribeToFarmChanges() {
    this.mainSubscription = this.farmService.selectedFarm$.subscribe(farm => {
      if (farm && farm.id) {
        this.locationName = `Loading weather for ${farm.name}...`;
        this.currentFarm = farm;

        this.weatherService.setFarmForUser(this.userId!, farm.id);

        this.notificationService.getLatestWeather(farm.id).subscribe({
          next: (data) => {
            if (data) {
              console.log('Loaded weather from DB:', data);

              const mappedData: WeatherData = {
                user_id: data.userId,
                farm_id: data.farmId,
                time: data.timestamp,
                lat: 0,
                lon: 0,
                weather_code: data.weatherCode,
                temp: data.temp
              };

              this.handleWeatherData(mappedData);
            }
          },
          error: (err) => console.log('No weather history yet or error:', err)
        });
      }
    });
  }

  ngOnDestroy() {
    this.disconnectWebSocket();

    if (this.wsSubscription) this.wsSubscription.unsubscribe();
    if (this.statusSubscription) this.statusSubscription.unsubscribe();
    if (this.mainSubscription) this.mainSubscription.unsubscribe();

    if (this.dateInterval) clearInterval(this.dateInterval);
  }

  private connectWebSocket(): void {
    if (!this.userId || !this.currentFarm) return;

    console.log(`Connecting WebSocket for Farm: ${this.currentFarm.id}`);
    try {
      this.weatherService.setFarmForUser(this.userId, this.currentFarm.id);
    } catch (error) {
      console.error('Error connecting WebSocket:', error);
    }
  }

  private disconnectWebSocket(): void {
    try {
      this.weatherService.disconnect();
    } catch (error) {
      console.error('Error disconnecting WebSocket:', error);
    }
  }

  reconnectWebSocket(): void {
    console.log('Manual reconnect initiated');
    try {
      this.weatherService.reconnect();
    } catch (error) {
      console.error('Error reconnecting WebSocket:', error);
    }
  }


  handleWeatherData(data: WeatherData) {
    if (!this.currentFarm || data.farm_id !== this.currentFarm.id) {
      return;
    }

    this.weatherData = data;
    this.lastUpdated = new Date();
    this.weatherInfo = this.getWeatherInfo(data.weather_code);

    if (this.currentFarm) {
      this.locationName = this.currentFarm.name;
    }
  }


  /**
   * Manually reconnect WebSocket
   */


  getWeatherInfo(code: number): WeatherInfo {
    const weatherMap: { [key: number]: WeatherInfo } = {
      0: { description: 'Clear Sky', icon: '☀️', background: 'linear-gradient(135deg, #FFE259 0%, #FFA751 100%)' },
      1: { description: 'Mainly Clear', icon: '🌤️', background: 'linear-gradient(135deg, #FFE259 0%, #FFA751 100%)' },
      2: { description: 'Partly Cloudy', icon: '⛅', background: 'linear-gradient(135deg, #89CFF0 0%, #4A90E2 100%)' },
      3: { description: 'Overcast', icon: '☁️', background: 'linear-gradient(135deg, #9CA3AF 0%, #6B7280 100%)' },
      45: { description: 'Foggy', icon: '🌫️', background: 'linear-gradient(135deg, #D1D5DB 0%, #9CA3AF 100%)' },
      48: { description: 'Depositing Rime Fog', icon: '🌫️', background: 'linear-gradient(135deg, #D1D5DB 0%, #9CA3AF 100%)' },
      51: { description: 'Light Drizzle', icon: '🌦️', background: 'linear-gradient(135deg, #7EC8E3 0%, #4A90E2 100%)' },
      53: { description: 'Moderate Drizzle', icon: '🌧️', background: 'linear-gradient(135deg, #5DADE2 0%, #3498DB 100%)' },
      55: { description: 'Dense Drizzle', icon: '🌧️', background: 'linear-gradient(135deg, #5DADE2 0%, #2980B9 100%)' },
      56: { description: 'Light Freezing Drizzle', icon: '🌨️', background: 'linear-gradient(135deg, #AED6F1 0%, #85C1E2 100%)' },
      57: { description: 'Dense Freezing Drizzle', icon: '🌨️', background: 'linear-gradient(135deg, #AED6F1 0%, #5DADE2 100%)' },
      61: { description: 'Slight Rain', icon: '🌧️', background: 'linear-gradient(135deg, #5DADE2 0%, #3498DB 100%)' },
      63: { description: 'Moderate Rain', icon: '🌧️', background: 'linear-gradient(135deg, #3498DB 0%, #2874A6 100%)' },
      65: { description: 'Heavy Rain', icon: '⛈️', background: 'linear-gradient(135deg, #2874A6 0%, #1B4F72 100%)' },
      66: { description: 'Light Freezing Rain', icon: '🌨️', background: 'linear-gradient(135deg, #AED6F1 0%, #85C1E2 100%)' },
      67: { description: 'Heavy Freezing Rain', icon: '🌨️', background: 'linear-gradient(135deg, #85C1E2 0%, #5DADE2 100%)' },
      71: { description: 'Slight Snow', icon: '🌨️', background: 'linear-gradient(135deg, #E8F4F8 0%, #B8D4E0 100%)' },
      73: { description: 'Moderate Snow', icon: '❄️', background: 'linear-gradient(135deg, #D6EAF8 0%, #AED6F1 100%)' },
      75: { description: 'Heavy Snow', icon: '❄️', background: 'linear-gradient(135deg, #AED6F1 0%, #85C1E2 100%)' },
      77: { description: 'Snow Grains', icon: '🌨️', background: 'linear-gradient(135deg, #E8F4F8 0%, #D6EAF8 100%)' },
      80: { description: 'Slight Rain Showers', icon: '🌦️', background: 'linear-gradient(135deg, #7EC8E3 0%, #4A90E2 100%)' },
      81: { description: 'Moderate Rain Showers', icon: '🌧️', background: 'linear-gradient(135deg, #4A90E2 0%, #3498DB 100%)' },
      82: { description: 'Violent Rain Showers', icon: '⛈️', background: 'linear-gradient(135deg, #2874A6 0%, #1B4F72 100%)' },
      85: { description: 'Slight Snow Showers', icon: '🌨️', background: 'linear-gradient(135deg, #E8F4F8 0%, #D6EAF8 100%)' },
      86: { description: 'Heavy Snow Showers', icon: '❄️', background: 'linear-gradient(135deg, #AED6F1 0%, #85C1E2 100%)' },
      95: { description: 'Thunderstorm', icon: '⛈️', background: 'linear-gradient(135deg, #34495E 0%, #2C3E50 100%)' },
      96: { description: 'Thunderstorm with Hail', icon: '⛈️', background: 'linear-gradient(135deg, #2C3E50 0%, #1C2833 100%)' },
      99: { description: 'Thunderstorm with Heavy Hail', icon: '⛈️', background: 'linear-gradient(135deg, #1C2833 0%, #17202A 100%)' }
    };

    const result = weatherMap[code] || {
      description: 'Unknown',
      icon: '❓',
      background: 'linear-gradient(135deg, #E0E0E0 0%, #BDBDBD 100%)'
    };

    return result;
  }



  getFormattedDate(): string {
    try {
      const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

      const day = days[this.currentDate.getDay()];
      const date = this.currentDate.getDate();
      const month = months[this.currentDate.getMonth()];
      const year = this.currentDate.getFullYear();

      return `${day}, ${date} ${month} ${year}`;
    } catch (error) {
      console.error('Error in getFormattedDate:', error);
      return 'Monday, 1 Jan 2025';
    }
  }

  getTimeSinceUpdate(): string {
    if (!this.lastUpdated) return '';

    const now = new Date();
    const diffMs = now.getTime() - this.lastUpdated.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;

    const diffDays = Math.floor(diffHours / 24);
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  }

  getFeelsLikeTemp(): number {
    if (!this.weatherData) return 0;

    const temp = this.weatherData.temp;
    const adjustment = this.weatherData.weather_code >= 61 ? -2 : 0;

    return Math.round(temp + adjustment);
  }
}
