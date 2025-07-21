import { Injectable, signal, computed } from '@angular/core';
import { OAuthService, AuthConfig, UserInfo } from 'angular-oauth2-oidc';

const authConfig: AuthConfig = {
  issuer: 'http://localhost:8080',
  clientId: 'finstream-sso',
  responseType: 'code',
  redirectUri: window.location.origin + '/callback',
  requireHttps: false,
  postLogoutRedirectUri: window.location.origin + '/',
};

export interface FinStreamUserInfo extends UserInfo {
  id: string;
  username: string;
  email: string;
  roles: string[];
}

@Injectable({
  providedIn: "root",
})
export class AuthService {
  private _isLoggedIn = signal(false);
  private _userInfo = signal<FinStreamUserInfo | null>(null);

  authenticated = computed(() => this._isLoggedIn());
  userInfo = computed(() => this._userInfo());
  userName = computed(() => {
    const info = this._userInfo();
    return info?.sub || "User";
  });

  constructor(private oauthService: OAuthService) {
    this.configureOAuth();
    this.setupTokenEvents();
  }

  private configureOAuth(): void {
    this.oauthService.configure(authConfig);
    this.oauthService.loadDiscoveryDocumentAndTryLogin().then(() => {
      this.updateAuthState();
    });
  }

  private setupTokenEvents(): void {
    this.oauthService.events.subscribe((event) => {
      if (event.type === "token_received" || event.type === "token_refreshed") {
        this.updateAuthState();
      }
      if (event.type === "logout") {
        this._isLoggedIn.set(false);
        this._userInfo.set(null);
      }
    });
  }

  private updateAuthState(): void {
    const hasToken = this.oauthService.hasValidAccessToken();
    this._isLoggedIn.set(hasToken);

    if (hasToken) {
      this._userInfo.set(this.oauthService.getIdentityClaims() as FinStreamUserInfo);
    }
  }

  login(): void {
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
  }

  get accessToken(): string {
    return this.oauthService.getAccessToken();
  }

  async handleCallback(): Promise<void> {
    try {
      await this.oauthService.tryLoginCodeFlow();
      console.log("Login successful");
      this.updateAuthState();
    } catch (error) {
      console.error("Callback handling failed:", error);
      throw new Error("Code flow login failed");
    }
  }
}
