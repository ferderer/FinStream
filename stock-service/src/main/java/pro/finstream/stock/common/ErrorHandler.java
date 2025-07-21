package pro.finstream.stock.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class ErrorHandler implements ExceptionMapper<Exception> {
    
    private static final Map<Class<? extends Throwable>, Response.Status> STATUS_MAP = Map.of(
        SecurityException.class, Response.Status.FORBIDDEN,
        IllegalArgumentException.class, Response.Status.BAD_REQUEST,
        ConstraintViolationException.class, Response.Status.BAD_REQUEST,
        UnsupportedOperationException.class, Response.Status.NOT_IMPLEMENTED
    );
    
    @Override
    public Response toResponse(Exception exception) {
        var status = getStatus(exception);
        var errorResponse = createErrorResponse(exception, status);
        
        return Response
            .status(status)
            .entity(errorResponse)
            .build();
    }
    
    private Response.Status getStatus(Exception error) {
        return STATUS_MAP.getOrDefault(error.getClass(), Response.Status.INTERNAL_SERVER_ERROR);
    }
    
    private ErrorResponse createErrorResponse(Exception error, Response.Status status) {
        return switch (error) {
            case ConstraintViolationException ex -> new ErrorResponse(
                "E_INVALID_INPUT",
                status.getStatusCode(),
                Instant.now().toString(),
                ex.getConstraintViolations().stream()
                    .map(ViolatedConstraint::new)
                    .collect(Collectors.toSet())
            );
            default -> new ErrorResponse(
                error.getMessage(),
                status.getStatusCode(),
                Instant.now().toString(),
                null
            );
        };
    }
    
    public record ErrorResponse(String code, int status, String timestamp, Set<ViolatedConstraint> invalid) {}
    
    public record ViolatedConstraint(String code, String path, Object value) {
        public ViolatedConstraint(ConstraintViolation<?> violation) {
            this(violation.getMessage(), violation.getPropertyPath().toString(), violation.getInvalidValue());
        }
    }
}
