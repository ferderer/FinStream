import { Component, inject, OnInit } from '@angular/core';
import { AuthService } from './auth-service';
import { Router } from '@angular/router';

@Component({
  selector: "app-callback",
  imports: [],
  template: `
    <div class="callback-container">
      @if (isProcessing) {
      <div class="loading">
        <h2>Processing login...</h2>
        <div class="spinner"></div>
      </div>
      } @else if (hasError) {
      <div class="error">
        <h2>Login failed</h2>
        <p>{{ errorMessage }}</p>
        <button (click)="goHome()" class="btn btn-primary">
          Return to Home
        </button>
      </div>
      }
    </div>
  `,
  styles: `
    .callback-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      text-align: center;
    }
    .loading, .error {
      max-width: 400px;
      padding: 2rem;
    }
    .spinner {
      border: 4px solid #f3f3f3;
      border-top: 4px solid #007bff;
      border-radius: 50%;
      width: 40px;
      height: 40px;
      animation: spin 1s linear infinite;
      margin: 20px auto;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    .btn {
      padding: 10px 20px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 16px;
      background-color: #007bff;
      color: white;
    }
    .error {
      color: #dc3545;
    }
    `,
})
export class Callback implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  isProcessing = true;
  hasError = false;
  errorMessage = "";

  async ngOnInit() {
    try {
      await this.authService.handleCallback();
      setTimeout(() => {
        this.router.navigate(["/"]);
      }, 500);
    }
    catch (error) {
      console.error("OAuth callback error:", error);
      this.isProcessing = false;
      this.hasError = true;
      this.errorMessage = "Failed to complete login. Please try again.";
    }
  }

  goHome(): void {
    this.router.navigate(["/"]);
  }
}
