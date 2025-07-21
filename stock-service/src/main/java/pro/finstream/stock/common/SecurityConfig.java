package pro.finstream.stock.common;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class SecurityConfig {

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken jwt;

    public Long getCurrentUserId() {
        String idStr = jwt.getClaim("id");
        return idStr != null ? Long.valueOf(idStr) : null;
    }

    public String getCurrentUsername() {
        return jwt.getSubject();
    }

    public Set<String> getUserRoles() {
        return identity.getRoles();
    }

    public boolean hasRole(String role) {
        return getUserRoles().contains(role);
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    public boolean isUser() {
        return hasRole("USER");
    }
}
