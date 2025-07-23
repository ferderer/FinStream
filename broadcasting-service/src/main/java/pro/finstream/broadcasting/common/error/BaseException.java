package pro.finstream.broadcasting.common.error;

import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Base exception for the FinStream Broadcasting Service.
 */
@Getter
@EqualsAndHashCode(callSuper = false)
public class BaseException extends RuntimeException {

    private static final String ASSERTION_ARGS_NOT_EVEN =
        "Uneven number arguments! Data arguments to BaseException(id, ...data) should be given in pairs: name, value!";
    private static final String ASSERTION_ODD_ARG_NOT_STRING =
        "Every odd data argument to BaseException(id, ...data) must be a string!";

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

    public static BaseException webSocketError(ErrorCode code, String connectionId, Object... data) {
        Object[] extendedData = new Object[data.length + 2];
        extendedData[0] = "connectionId";
        extendedData[1] = connectionId;
        System.arraycopy(data, 0, extendedData, 2, data.length);
        return new BaseException(code, HttpStatus.BAD_REQUEST, extendedData);
    }

    public static BaseException kafkaError(ErrorCode code, String topic, String consumerGroup, Object... data) {
        Object[] extendedData = new Object[data.length + 4];
        extendedData[0] = "topic";
        extendedData[1] = topic;
        extendedData[2] = "consumerGroup";
        extendedData[3] = consumerGroup;
        System.arraycopy(data, 0, extendedData, 4, data.length);
        return new BaseException(code, HttpStatus.SERVICE_UNAVAILABLE, extendedData);
    }

    public static BaseException stockDataError(ErrorCode code, String symbol, Object... data) {
        Object[] extendedData = new Object[data.length + 2];
        extendedData[0] = "symbol";
        extendedData[1] = symbol;
        System.arraycopy(data, 0, extendedData, 2, data.length);
        return new BaseException(code, HttpStatus.UNPROCESSABLE_ENTITY, extendedData);
    }

    public static BaseException authError(ErrorCode code, String userId, Object... data) {
        Object[] extendedData = new Object[data.length + 2];
        extendedData[0] = "userId";
        extendedData[1] = userId;
        System.arraycopy(data, 0, extendedData, 2, data.length);
        return new BaseException(code, HttpStatus.UNAUTHORIZED, extendedData);
    }
}
