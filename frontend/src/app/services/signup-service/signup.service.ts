import { Injectable } from '@angular/core';
import { Globals } from '../../global/globals'
import {HttpClient} from '@angular/common/http';
import {SignupDetail} from '../../dtos/signup';


@Injectable({
  providedIn: 'root',
})
export class SignupService {
  private signUpBaseUri: string;
  constructor(private httpClient: HttpClient, private globals: Globals) {
    this.signUpBaseUri = this.globals.backendUri + '/users';
  }

  signupUser(signupDetail: SignupDetail) {
    return this.httpClient.post(this.signUpBaseUri, signupDetail)
  }
}
