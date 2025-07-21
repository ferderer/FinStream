package pro.finstream.sso.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import static org.springframework.util.StreamUtils.copyToString;
import pro.finstream.sso.domain.login.UserInfo;
import pro.finstream.sso.support.auth.AuthenticationToken;

@Configuration
public class SecurityBeans {

    @Value("${ui.url}")
    private String uiUrl;

    @Value("${self.url}")
    private String selfUrl;

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient client = RegisteredClient.withId("2f058b63-9175-4bd2-beea-a3938cc32f2f")
            .clientId("finstream-sso")
            .clientIdIssuedAt(Instant.now().minus(7L, ChronoUnit.DAYS))
            .clientName("Finstream Web Portal")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri(uiUrl + "/callback")
            .postLogoutRedirectUri(uiUrl + "/")
            .scope(OidcScopes.OPENID)
            .scope(OidcScopes.PROFILE)
            .scope(OidcScopes.EMAIL)
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true)
                .build())
            .tokenSettings(TokenSettings.builder()
                .authorizationCodeTimeToLive(Duration.ofMinutes(1))
                .accessTokenTimeToLive(Duration.ofMinutes(15))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .reuseRefreshTokens(false)
                .build()
            )
            .build();

        return new InMemoryRegisteredClientRepository(client);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            var claims = context.getClaims();
            var principal = context.getPrincipal();
            
            AuthenticationToken authToken = ((AuthenticationToken) principal);
            UserInfo ui = (UserInfo) authToken.getPrincipal();

            claims.claim("id", ui.id().toString());
            claims.claim("roles", ui.roles());
            claims.subject(ui.username());

            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                claims.claim("email", ui.email());
                claims.claim("username", ui.username());
            }
        };
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws JOSEException, IOException {
        return new ImmutableJWKSet<>(new JWKSet(
            JWK.parseFromPEMEncodedObjects(copyToString(new ClassPathResource("keys/private.pem").getInputStream(), UTF_8))
        ));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
