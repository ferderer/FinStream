package pro.finstream.sso.support.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {
    private static final String ATTR_VALIDATIONS = "invalid";
    private static final String ATTR_MESSAGE_CODE = "code";
    private static final Map<Class<? extends Throwable>, HttpStatus> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put(java.nio.file.AccessDeniedException.class, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(org.springframework.security.authentication.BadCredentialsException.class, HttpStatus.UNAUTHORIZED);
        STATUS_MAP.put(jakarta.validation.ConstraintViolationException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(org.springframework.http.converter.HttpMessageNotReadableException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(org.springframework.security.authentication.InsufficientAuthenticationException.class, HttpStatus.FORBIDDEN);
        STATUS_MAP.put(org.springframework.web.bind.MethodArgumentNotValidException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(org.springframework.beans.TypeMismatchException.class, HttpStatus.BAD_REQUEST);
        STATUS_MAP.put(org.springframework.security.core.userdetails.UsernameNotFoundException.class, HttpStatus.NOT_FOUND);
        STATUS_MAP.put(UnsupportedOperationException.class, HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> ea = super.getErrorAttributes(webRequest, options);

        Throwable error = getError(webRequest);
        ea.put("status", getStatus(error));
        
        if (error != null) {
            switch (error) {
                case BaseException ex -> ea.put(ATTR_MESSAGE_CODE, ex.getCode());
                case ConstraintViolationException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_INVALID_INPUT);
                case MethodArgumentNotValidException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_INVALID_INPUT);
                case BindException ex -> ea.put(ATTR_MESSAGE_CODE, ErrorCode.E_INVALID_INPUT);
                default -> ea.put(ATTR_MESSAGE_CODE, error.getMessage());
            }

            switch (error) {
                case BaseException ex -> ea.put("data", ex.getData());
                case ConstraintViolationException ex -> ea.put(ATTR_VALIDATIONS, ex.getConstraintViolations()
                    .stream().map(ViolatedConstraint::new).collect(Collectors.toSet()));
                case MethodArgumentNotValidException ex -> ea.put(ATTR_VALIDATIONS, ex.getBindingResult().getFieldErrors()
                    .stream().map(ViolatedConstraint::new).collect(Collectors.toSet()));
                case BindException ex -> ea.put(ATTR_VALIDATIONS, ex.getBindingResult().getFieldErrors()
                    .stream().map(ViolatedConstraint::new).collect(Collectors.toSet()));
                default -> {}
            }
        }
        return ea;
    }

    /**
     * Extracts the HTTP status code from the thrown exception.
     *
     * @return HTTP status corresponding to the thrown exception, or 500 (INTERNAL_SERVER_ERROR) otherwise.
     */
    public HttpStatus getStatus(Throwable error) {
        if (error instanceof BaseException exception) {
            return exception.getStatus();
        }
        else if (error != null && STATUS_MAP.containsKey(error.getClass())) {
            return STATUS_MAP.get(error.getClass());
        }
        else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    public static record ViolatedConstraint(String code, String path, Object value) {
        public ViolatedConstraint(ConstraintViolation<?> violation) {
            this(violation.getMessage(), violation.getPropertyPath().toString(), violation.getInvalidValue());
        }

        public ViolatedConstraint(FieldError violation) {
            this(violation.getDefaultMessage(), violation.getField(), violation.getRejectedValue());
        }
    }
}
