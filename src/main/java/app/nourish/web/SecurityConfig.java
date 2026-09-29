package app.nourish.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security config for the Cognito-backed OAuth2 resource server.
 *
 * <p>Every {@code /api/**} call must carry a valid Cognito access token
 * ({@code Authorization: Bearer <jwt>}); the token's signature is verified against
 * the pool's JWKS. Public paths (docs, health, meta) stay open.</p>
 *
 * <p>For local development without an identity provider, set
 * {@code nourish.security.enabled=false} to disable JWT enforcement — identity
 * then falls back to the {@code X-User-Id} header (see {@link CurrentUser}).</p>
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/meta",
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final boolean securityEnabled;
    private final String issuerUri;
    private final String jwkSetUri;
    private final String expectedClientId;
    private final CorsConfigurationSource corsSource;

    public SecurityConfig(
            @Value("${nourish.security.enabled:true}") boolean securityEnabled,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${nourish.security.expected-client-id:}") String expectedClientId,
            @Value("${nourish.cors.allowed-origins:http://localhost:3000}") String origins) {
        this.securityEnabled = securityEnabled;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
        this.expectedClientId = expectedClientId;
        this.corsSource = buildCors(origins);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityEnabled) {
            // Local dev: no token required; identity comes from X-User-Id.
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * JWT decoder that verifies the signature against Cognito's JWKS and validates
     * the issuer (and optionally the {@code client_id}, since Cognito access tokens
     * carry {@code client_id} rather than {@code aud}).
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        if (!securityEnabled) {
            // A decoder bean is still required by the context; return a harmless
            // one. It is never invoked because all requests are permitted.
            return token -> {
                throw new UnsupportedOperationException("Security is disabled");
            };
        }

        NimbusJwtDecoder decoder = (jwkSetUri != null && !jwkSetUri.isBlank())
                ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
                : (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = (issuerUri != null && !issuerUri.isBlank())
                ? JwtValidators.createDefaultWithIssuer(issuerUri)
                : JwtValidators.createDefault();

        if (expectedClientId != null && !expectedClientId.isBlank()) {
            OAuth2TokenValidator<Jwt> clientIdValidator =
                    new JwtClaimValidator<String>("client_id", expectedClientId::equals);
            decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                    withIssuer, clientIdValidator));
        } else {
            decoder.setJwtValidator(withIssuer);
        }
        return decoder;
    }

    private CorsConfigurationSource buildCors(String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split("\\s*,\\s*")).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
