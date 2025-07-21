package pro.finstream.sso.domain;

public interface Endpoints {
    String URL_ALL             = "/**";
    String URL_STATIC_ALL      = "/_/**";
    String URL_FAVICON         = "/favicon.ico";

    String URL_ERROR           = "/error";
    String URL_ERROR_403       = "/error/403";
    String URL_ERROR_404       = "/error/404";
    String URL_ERROR_500       = "/error/500";
    String URL_ERROR_ALL       = "/error/**";

    String URL_LOGIN           = "/login";
    String URL_LOGOUT          = "/connect/logout";
    String URL_REGISTER        = "/register";
    String URL_USERINFO        = "/userinfo";

    String URL_OAUTH2_ALL      = "/oauth2/**";
    String URL_OAUTH2_JWKS     = "/oauth2/jwks";
    String URL_WELL_KNOWN      = "/.well-known/**";
}
