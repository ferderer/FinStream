import { Routes } from '@angular/router';
import { App } from './app';
import { Callback } from './auth/callback';
import { Dashboard } from './dashboard/dashboard';

export const routes: Routes = [
  { path: "callback", component: Callback },
  { path: 'dashboard', component: Dashboard },
  { path: "", component: App },
  { path: "**", redirectTo: "/" },
];