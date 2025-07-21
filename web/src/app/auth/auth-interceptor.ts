import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { OAuthService } from "angular-oauth2-oidc";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const oauthService = inject(OAuthService);

  if (oauthService.hasValidAccessToken() && isApiRequest(req.url)) {
    const token = oauthService.getAccessToken();

    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    return next(authReq);
  }

  return next(req);
};

// Helper function to determine if this is an API request that needs auth
function isApiRequest(url: string): boolean {
  return (
    url.startsWith("https://finstream.pro/api/") ||
    url.startsWith("https://localhost:8080/api/")
  );
}
