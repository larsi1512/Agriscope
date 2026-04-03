import { Injectable } from '@angular/core';
interface Jwt {
  [key: string]: any;
}
@Injectable({
  providedIn: 'root'
})
export class JwtUtilServiceService {

  constructor() { }

  getEmailFromToken(jwtToken: string): string |null {
    try {
      const decodedToken = this.decodeJwt(jwtToken);
      return decodedToken?.['sub']; // 'sub' is the standard claim for subject (email)
    } catch (error) {
      console.error('Error decoding JWT token:', error);
      return null;
    }
  }

  getExpiryDateFromToken(jwtToken: string): Date | null {
    try {
      const decodedToken = this.decodeJwt(jwtToken);
      const timestamp = decodedToken?.['exp'];
      return timestamp ? new Date(timestamp * 1000) : null;
    } catch (error) {
      console.error('Error decoding JWT token:', error);
      return null;
    }
  }

  private decodeJwt(token: string): Jwt | null {
    try {
      // You can use your preferred method of decoding here
      const decodedToken: Jwt = JSON.parse(atob(token.split('.')[1]));
      return decodedToken;
    } catch (error) {
      console.error('Error decoding JWT token:', error);
      return null;
    }
  }
}
