package pro.finstream.stock.stockdata;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.eclipse.microprofile.jwt.JsonWebToken;
import pro.finstream.stock.common.SecurityConfig;

@Path("/stocks")
public class StockResource {
    
    @Inject
    SecurityConfig security;
    
    @Inject
    JsonWebToken jwt;
    
    @Inject
    SecurityIdentity identity;
    
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public HealthResponse health() {
        return new HealthResponse("Stock Service", "UP", System.currentTimeMillis());
    }
    
    @GET
    @Path("/user-info")
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_JSON)
    public UserInfoResponse userInfo() {
        return new UserInfoResponse(
            security.getCurrentUserId(),
            security.getCurrentUsername(),
            security.getUserRoles()
        );
    }

    @GET
    @Path("/debug")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> debug() {
        return Map.of(
            "principal", jwt.getName(),
            "claims", jwt.getClaimNames(),
            "roles-claim", jwt.getClaim("roles"),
            "identity-roles", identity.getRoles()
        );
    }

    @GET
    @Path("/jwt-debug")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jwtDebug(@Context jakarta.ws.rs.core.SecurityContext securityContext) {
        return Response.ok(Map.of(
            "userPrincipal", securityContext.getUserPrincipal(),
            "isSecure", securityContext.isSecure(),
            "authScheme", securityContext.getAuthenticationScheme()
        )).build();
    }    

    public record HealthResponse(String service, String status, long timestamp) {}
    public record UserInfoResponse(Long id, String username, java.util.Set<String> roles) {}
}
