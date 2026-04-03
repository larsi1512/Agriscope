import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Globals } from '../../global/globals';
import {Seed} from '../../models/Seed'
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SeedService {
  private signUpBaseUri: string;

  constructor(private httpClient: HttpClient, private globals: Globals) {
    this.signUpBaseUri = this.globals.backendUri + '/seeds';
  }

  getAll(): Observable<Seed[]> {
    return this.httpClient.get<Seed[]>(`${this.signUpBaseUri}/getAll`);
  }

  getSeedByName(name : String): Observable<Seed> {
    return this.httpClient.get<Seed>(`${this.signUpBaseUri}/getByName/${name}` );
  }

}
