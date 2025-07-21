import { Component, inject } from '@angular/core';
import { AuthService } from './auth/auth-service';

@Component({
  selector: "app-root",
  template: `
    <div class="container">
      <h1>FinStream Dashboard</h1>

      <div class="auth-section">
        @if (!authService.authenticated()) {
          <p>Please log in to access the dashboard</p>
          <button (click)="authService.login()" class="btn btn-primary">Login</button>
        }
        @else {
          <div class="user-info">
            @if (authService.userInfo()) {
              <div class="user-details">
                <h3>Welcome, {{ authService.userName() }}!</h3>

                @if (authService.userInfo()?.email) {
                  <p class="user-email">{{ authService.userInfo()?.email }}</p>
                }

                @if (authService.userInfo()?.roles?.length) {
                  <div class="user-roles">
                    <span class="role-label">Roles:</span>
                    @for (role of authService.userInfo()?.roles; track role) {
                      <span class="role-badge">{{ role }}</span>
                    }
                  </div>
                }

                <div class="user-actions">
                  <button (click)="authService.logout()" class="btn btn-sm btn-secondary">Logout</button>
                </div>
              </div>
            }
            @else {
              <div class="user-error">
                <p>Failed to load user information</p>
                <button (click)="authService.logout()" class="btn btn-sm btn-secondary">Logout</button>
              </div>
            }
          </div>
        }
      </div>

      @if (authService.authenticated()) {
        <div>
          <h2>Stock Dashboard</h2>
          <p>This is where your stock data will go...</p>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 20px;
      }
      .auth-section {
        background: #f5f5f5;
        padding: 15px;
        border-radius: 5px;
        margin-bottom: 20px;
      }
      .user-info {
        min-height: 80px;
      }
      .loading-user {
        display: flex;
        align-items: center;
        gap: 10px;
      }
      .spinner-small {
        width: 16px;
        height: 16px;
        border: 2px solid #f3f3f3;
        border-top: 2px solid #007bff;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }
      .user-details h3 {
        margin: 0 0 8px 0;
        color: #333;
      }
      .user-email {
        color: #666;
        font-size: 14px;
        margin: 0 0 12px 0;
      }
      .user-roles {
        margin: 12px 0;
      }
      .role-label {
        font-weight: 500;
        margin-right: 8px;
      }
      .role-badge {
        background: #e9ecef;
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 12px;
        margin-right: 4px;
      }
      .user-actions {
        margin-top: 15px;
        display: flex;
        gap: 10px;
      }
      .user-error {
        color: #dc3545;
      }
      .btn {
        padding: 10px 20px;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 16px;
      }
      .btn-sm {
        padding: 6px 12px;
        font-size: 14px;
      }
      .btn-primary {
        background-color: #007bff;
        color: white;
      }
      .btn-secondary {
        background-color: #6c757d;
        color: white;
      }
      .btn-outline {
        background-color: transparent;
        color: #007bff;
        border: 1px solid #007bff;
      }
      .btn:hover {
        opacity: 0.8;
      }
      @keyframes spin {
        0% {
          transform: rotate(0deg);
        }
        100% {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class App {
  authService = inject(AuthService);
}
