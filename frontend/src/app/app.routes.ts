import { Routes } from '@angular/router';
import { Login } from './components/login/login';
import { Signup } from './components/signup/signup';
import { AuthGuard } from './guard/auth-guard';
import { NoAuthGuard } from './guard/no-auth-guard';
import { HomeComponent } from './components/home/home';
import { NewFarmComponent } from './components/new-farm-component/new-farm-component';
import { LandingPageComponent } from './components/landing-page/landing-page';
import { SeedsComponent } from './components/seeds/seeds';
import { Profile } from './components/profile/profile';
import {Feedback} from './components/feedback/feedback';
import {StatisticComponent} from './components/statistic/statistic';
import {ResetPassword} from './components/reset-password/reset-password';
import {ForgotPassword} from './components/forgot-password/forgot-password';

export const routes: Routes = [
  { path: '', component: LandingPageComponent, canActivate: [NoAuthGuard] },
  { path: 'login', component: Login, canActivate: [NoAuthGuard] },
  { path: 'signup', component: Signup, canActivate: [NoAuthGuard] },
  { path: 'home', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'new-farm', component: NewFarmComponent, canActivate: [AuthGuard] },
  { path: 'seeds', component: SeedsComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: Profile, canActivate: [AuthGuard] },
  { path: 'feedback', component: Feedback, canActivate: [AuthGuard] },
  { path: 'statistic', component: StatisticComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: Profile, canActivate: [AuthGuard] },
  { path: 'reset-password', component: ResetPassword, canActivate: [NoAuthGuard] },
  { path: 'forgot-password', component: ForgotPassword, canActivate: [NoAuthGuard] }
];
