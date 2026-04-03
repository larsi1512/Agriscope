import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { Farm } from '../../models/Farm';
import { HttpClient } from '@angular/common/http';
import { FarmCheckResponse, FarmCreateDto } from '../../dtos/farm';
import { tap, catchError } from 'rxjs/operators';
import { Globals } from '../../global/globals';
import { FieldStatus } from '../../models/FieldStatus';
import { FieldDetailsDto } from '../../dtos/field';

export interface HarvestRequest {
  harvestDate: Date;
  answers: any[];
}

@Injectable({
  providedIn: 'root',
})
export class FarmService {
  private farmsBaseUri: string;
  private readonly STORAGE_KEY = 'selectedFarmId';

  // Subject to store and share farm data across components
  private farmsSubject = new BehaviorSubject<Farm[]>([]);
  farms$ = this.farmsSubject.asObservable();  // Observable for subscribing components

  private selectedFarmSubject = new BehaviorSubject<Farm | null>(null);  // Subject to track the selected farm
  selectedFarm$ = this.selectedFarmSubject.asObservable();  // Observable for selected farm

  constructor(private httpClient: HttpClient, private globals: Globals) {
    this.farmsBaseUri = this.globals.backendUri + '/farms';

    // Load all farms on initialization (this will handle restoration)
    this.loadFarms().subscribe();
  }

  // Set the selected farm and notify all subscribers
  selectFarm(farm: Farm) {
    this.selectedFarmSubject.next(farm);  // Update selected farm in the service
    console.log('Selected Farm:', farm);  // Log farm information each time it's selected

    // Save to localStorage
    if (farm && farm.id) {
      localStorage.setItem(this.STORAGE_KEY, farm.id);
    }
  }

  getSelectedFarm(): Farm | null {
    return this.selectedFarmSubject.value;
  }

  clearSelectedFarm() {
    this.selectedFarmSubject.next(null);
    // Remove from localStorage
    localStorage.removeItem(this.STORAGE_KEY);
  }

  // Get a specific farm by ID (for specific use cases)
  getFarmById(farmId: string): Observable<Farm> {
    return this.httpClient.get<Farm>(`${this.farmsBaseUri}/${farmId}`);
  }

  // Method to get farms in memory (direct access to BehaviorSubject)
  getFarmsInMemory(): Farm[] {
    return this.farmsSubject.value;  // Return the current value of farms from memory
  }

  // Add a new farm and update the list of farms
  addNewFarm(farm: FarmCreateDto): Observable<Farm> {
    farm.fields= [
      { id: 1, status: FieldStatus.empty },
      { id: 2, status: FieldStatus.empty },
      { id: 3, status: FieldStatus.empty },
      { id: 4, status: FieldStatus.empty },
      { id: 5, status: FieldStatus.empty },
      { id: 6, status: FieldStatus.empty }
    ];

    return this.httpClient.post<Farm>(this.farmsBaseUri, farm).pipe(
      tap((newFarm) => {
        const currentFarms = this.farmsSubject.value;
        this.farmsSubject.next([...currentFarms, newFarm]);  // Add the new farm to the farms list
        this.selectFarm(newFarm);
      })
    );
  }

  harvestField(farmId: string, fieldId: number, data: HarvestRequest): Observable<void> {
    return this.httpClient.post<void>(`${this.farmsBaseUri}/${farmId}/fields/${fieldId}/harvest`, data);
  }

  submitFeedback(historyId: string, answers: any[]): Observable<void> {
    return this.httpClient.post<void>(`${this.farmsBaseUri}/harvest-history/${historyId}/feedback`, answers);
  }

  getHarvestHistory(farmId: string): Observable<any[]> {
    return this.httpClient.get<any[]>(`${this.farmsBaseUri}/${farmId}/harvest-history`);
  }

  getFeedbackQuestions(): Observable<any[]> {
    return this.httpClient.get<any[]>('assets/data/feedback-questions.json');
  }

  // Fetch farms from the backend
  loadFarms(): Observable<Farm[]> {
    return this.httpClient.get<Farm[]>(this.farmsBaseUri).pipe(
      tap((farms) => {
        this.farmsSubject.next(farms);  // Update farms list

        // Check if we have a saved farm ID in localStorage
        const savedFarmId = localStorage.getItem(this.STORAGE_KEY);

        if (savedFarmId) {
          // Try to find and select the saved farm
          const savedFarm = farms.find(f => f.id === savedFarmId);
          if (savedFarm) {
            this.selectedFarmSubject.next(savedFarm);
            console.log('Restored selected farm:', savedFarm);
          } else {
            // Saved farm not found in the list, clear storage and select first farm
            console.warn('Saved farm not found, clearing localStorage');
            localStorage.removeItem(this.STORAGE_KEY);
            if (farms.length) {
              this.selectFarm(farms[0]);
            }
          }
        } else if (farms.length && !this.selectedFarmSubject.value) {
          // No saved farm, select the first one
          this.selectFarm(farms[0]);
        }
      }),
      catchError((error) => {
        console.error('Error loading farms:', error);
        return throwError(() => error);
      })
    );
  }

  checkHasFarms(): Observable<FarmCheckResponse> {
    return this.httpClient.get<FarmCheckResponse>(`${this.farmsBaseUri}/check`);
  }

  // Update the selected field in the backend
  updateField(field: FieldDetailsDto): Observable<Farm> {
    const selectedFarm = this.selectedFarmSubject.value;

    if (!selectedFarm) {
      return throwError(() => new Error('No farm selected'));
    }

    return this.httpClient.put<Farm>(
      `${this.farmsBaseUri}/${selectedFarm.id}/fields`,
      field
    ).pipe(
      tap(updatedFarm => {
        // Update selected farm
        this.selectedFarmSubject.next(updatedFarm);

        // Update farms list
        const farms = this.farmsSubject.value.map(f =>
          f.id === updatedFarm.id ? updatedFarm : f
        );
        this.farmsSubject.next(farms);
      })
    );
  }

  deleteFeedback(historyId: string): Observable<void> {
    console.log('Deleting feedback for history ID:', historyId);
    return this.httpClient.delete<void>(`${this.farmsBaseUri}/harvest-history/${historyId}`).pipe(
      tap(() => console.log('Delete request completed')),
      catchError((error) => {
        console.error('Delete request failed:', error);
        return throwError(() => error);
      })
    );
  }

  deleteAllFeedbackForFarm(farmId: string): Observable<void> {
    console.log('Deleting all feedback for farm:', farmId);
    return this.httpClient.delete<void>(`${this.farmsBaseUri}/${farmId}/harvest-history`).pipe(
      tap(() => console.log('All feedback deleted successfully')),
      catchError((error) => {
        console.error('Delete all feedback failed:', error);
        return throwError(() => error);
      })
    );
  }

  //TODO: Add delete farm?
}
