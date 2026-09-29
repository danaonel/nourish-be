package app.nourish.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Resolves the current user id — the stable Cognito subject ({@code sub}) claim.
 *
 * <p>When {@code nourish.security.enabled=true} (the default and the correct
 * setting for any multi-user deployment), the id is taken from the validated JWT
 * in the security context; it cannot be spoofed by the client.</p>
 *
 * <p>When security is disabled for local development, the id falls back to the
 * {@code X-User-Id} header so the app is still runnable without an identity
 * provider. Controllers call {@link #resolve(HttpServletRequest)} unchanged.</p>
 */
@Component
public class CurrentUser {
    public static final String HEADER = "X-User-Id";
    public static final String DEFAULT_DEV_USER = "demo-user";

    private final boolean securityEnabled;

    public CurrentUser(@Value("${nourish.security.enabled:true}") boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public String resolve(HttpServletRequest request) {
        if (securityEnabled) {
            return subjectFromJwt();
        }
        String id = request.getHeader(HEADER);
        return (id == null || id.isBlank()) ? DEFAULT_DEV_USER : id;
    }

    private String subjectFromJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null && !sub.isBlank()) {
                return sub;
            }
        }
        // Should not happen for an authenticated /api/** request, but fail closed.
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
    }
}
