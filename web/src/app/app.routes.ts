import { Routes } from '@angular/router';
import { App } from './app';
import { Callback } from './auth/callback';

export const routes: Routes = [
  { path: "callback", component: Callback },
  { path: "", component: App },
  { path: "**", redirectTo: "/" },
];