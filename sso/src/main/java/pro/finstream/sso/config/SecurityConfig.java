package pro.finstream.sso.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import static pro.finstream.sso.domain.Endpoints.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${ui.url}")
    private String uiUrl;

    @Bean
    @Order(0)
    public SecurityFilterChain staticSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(URL_STATIC_ALL, URL_FAVICON)
            .authorizeHttpRequests(r -> r.anyRequest().permitAll())
            .requestCache(rc -> rc.disable())
            .securityContext(sc -> sc.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable()) //NOSONAR – static resources don't need CSRF
            .cors(cors -> {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(uiUrl));
                configuration.setAllowedHeaders(List.of("Accept", "Cache-Control", "If-Modified-Since"));
                configuration.setAllowedMethods(List.of("GET", "HEAD", "OPTIONS"));
                configuration.setAllowCredentials(false);
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                cors.configurationSource(source);
            })
            .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(URL_WELL_KNOWN, URL_OAUTH2_ALL, URL_LOGIN, URL_LOGOUT)
            .cors(cors -> {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of(uiUrl));
                configuration.setAllowedHeaders(List.of("Accept", "Content-Type", "Authorization", "X-Requested-With", "Cookie"));
                configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
                configuration.setAllowCredentials(true); // Required for OAuth flows
                configuration.setMaxAge(1800L); // 30 minutes cache

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                cors.configurationSource(source);
            })
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .preload(true)
                    .maxAgeInSeconds(31536000)
                )
            )
            .authorizeHttpRequests(registry -> registry
                .requestMatchers(URL_WELL_KNOWN, URL_OAUTH2_JWKS, URL_LOGIN).permitAll()
                .anyRequest().authenticated()
            )
            .with(new OAuth2AuthorizationServerConfigurer().oidc(Customizer.withDefaults()), Customizer.withDefaults())
            .formLogin(configurer -> configurer
                .defaultSuccessUrl(uiUrl)
            )
            .logout(logout -> logout.disable())
            .build();
    }
}
