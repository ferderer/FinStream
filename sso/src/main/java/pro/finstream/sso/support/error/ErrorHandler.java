package pro.finstream.sso.support.error;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import pro.finstream.sso.domain.Endpoints;

@RestController
@RequiredArgsConstructor
public class ErrorHandler {

    private final CustomErrorAttributes ea;
    private static final ErrorAttributeOptions ALL_ERROR_ATTRIBUTES = ErrorAttributeOptions.of();

    @GetMapping(path = {Endpoints.URL_ERROR, Endpoints.URL_ERROR_ALL})
    public Map<String, Object> genericJsonError(WebRequest request) {
        return ea.getErrorAttributes(request, ALL_ERROR_ATTRIBUTES);
    }

    @GetMapping(path = Endpoints.URL_ERROR_404)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public String staticFileNotFound() {
        return "No such file";
    }
}

