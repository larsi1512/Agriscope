import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = localStorage.getItem('authToken');
    console.log('Intercepting Request');
    console.log('Token:', token);

    if (token) {
      const cloned = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log('Cloned Request:', cloned);  // Log the cloned request with headers
      return next.handle(cloned);  // Proceed with the modified request
    }

    return next.handle(request);  // Proceed with the original request if no token
  }
}