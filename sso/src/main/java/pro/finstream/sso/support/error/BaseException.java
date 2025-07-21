package pro.finstream.sso.support.error;

import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Defines base exception, providing the ability to pass message codes with additional data.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class BaseException extends RuntimeException {

    private static final String ASSERTION_ARGS_NOT_EVEN =
        "Uneven number arguments! Data arguments to BaseException(id, ...data) should be given in pairs: name, value!";
    private static final String ASSERTION_ODD_ARG_NOT_STRING =
        "Every odd data argument to RestApiException(id, ...data) must be a string!";

    private final ErrorCode code;
    private final HttpStatus status;
    private final Map<String, Object> data = new HashMap<>();

    public BaseException(ErrorCode errorCode, Object... data) {
        super(errorCode.name());
        code = errorCode;
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        mapData(data);
    }

    public BaseException(ErrorCode errorCode, Throwable cause, Object... data) {
        super(errorCode.name(), cause);
        code = errorCode;
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        mapData(data);
    }

    public BaseException(ErrorCode errorCode, HttpStatus httpStatus, Object... data) {
        super(errorCode.name());
        code = errorCode;
        status = httpStatus;
        mapData(data);
    }

    public BaseException(ErrorCode errorCode, Throwable cause, HttpStatus httpStatus, Object... data) {
        super(errorCode.name(), cause);
        code = errorCode;
        status = httpStatus;
        mapData(data);
    }

    private void mapData(Object[] data) {
        Assert.isTrue((data.length % 2) == 0, ASSERTION_ARGS_NOT_EVEN);

        for(int i = 1, N = data.length; i < N; i += 2) {
            Assert.isInstanceOf(String.class, data[i - 1], ASSERTION_ODD_ARG_NOT_STRING);
            this.data.put((String) data[i - 1], data[i]);
        }
    }
}
