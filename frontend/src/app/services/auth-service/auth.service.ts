import {Injectable} from '@angular/core';
import {AuthRequest} from '../../dtos/auth-request';
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {tap} from 'rxjs/operators';
import {jwtDecode} from 'jwt-decode';
import {Globals} from '../../global/globals';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private authBaseUri: string;
  constructor(private httpClient: HttpClient, private globals: Globals) {
    this.authBaseUri = this.globals.backendUri + '/authentication';
  }

  /**
   * Login in the user. If it was successful, a valid JWT token will be stored
   *
   * @param authRequest User data
   */
  loginUser(authRequest: AuthRequest): Observable<any> {
    return this.httpClient.post<any>(this.authBaseUri, authRequest)
      .pipe(
        tap((authResponse: any) => {
          if (authResponse && authResponse.token) {
            this.setToken(authResponse.token);
          }
        })
      );
  }



  /**
   * Check if a valid JWT token is saved in the localStorage
   */
  isLoggedIn() {
    const token = this.getToken();
    if (!token) return false;
    return this.getTokenExpirationDate(token)!.valueOf() > new Date().valueOf();
  }

  logoutUser() {
    console.log('Logout');
    localStorage.removeItem('authToken');
  }

  getToken() {
    return localStorage.getItem('authToken');
  }

  private setToken(authResponse: string) {
    localStorage.setItem('authToken', authResponse);
  }

  private getTokenExpirationDate(token: string): Date | null {

    const decoded: any = jwtDecode(token);
    if (decoded['exp'] === undefined) {
      return new Date(0); //null return
    }

    const date = new Date(0);
    date.setUTCSeconds(decoded['exp']);
    return date;
  }

}
