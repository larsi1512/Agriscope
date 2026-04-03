import { Injectable } from '@angular/core';
import {Globals} from '../../global/globals';
import {HttpClient} from '@angular/common/http';
import {AuthService} from '../auth-service/auth.service';
import {Observable, ObservedValueOf} from 'rxjs';
import {EditUserDto, UserProfileDetail} from '../../dtos/user';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private profileDataUri;
  constructor(private httpClient: HttpClient, private globals: Globals, private authService: AuthService) {
    this.profileDataUri = this.globals.backendUri + "/users/me";
  }
  getProfile() :Observable<UserProfileDetail> {
    return this.httpClient.get<UserProfileDetail>(this.profileDataUri);
  }

    editProfile(data: FormData): Observable<UserProfileDetail> {
    return this.httpClient.put<UserProfileDetail>(this.profileDataUri, data);
  }

  deleteProfile(): Observable<any>{
    return this.httpClient.put(this.profileDataUri + "/delete", {})
  }
}
