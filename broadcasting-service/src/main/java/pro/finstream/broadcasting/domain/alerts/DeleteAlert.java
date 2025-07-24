package pro.finstream.broadcasting.domain.alerts;

import pro.finstream.broadcasting.domain.alerts.model.PriceAlertRepository;
import pro.finstream.broadcasting.domain.alerts.model.PriceAlertEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.common.error.BaseException;
import pro.finstream.broadcasting.common.error.ErrorCode;
import pro.finstream.broadcasting.domain.Endpoints;

@RestController
@RequiredArgsConstructor
public class DeleteAlert implements CurrentUserAccessor {
    public record DeleteAlertRequest(
        @NotNull(message = "Alert ID is required")
        Long id
    ) {}

    public record DeleteAlertResponse(
        Long id,
        String symbol,
        String message,
        LocalDateTime timestamp
    ) {}
    
    private final PriceAlertRepository priceAlertRepository;

    @DeleteMapping(Endpoints.API_ALERTS)
    public DeleteAlertResponse deleteAlert(@Valid @RequestBody DeleteAlertRequest request) {
        PriceAlertEntity alert = priceAlertRepository.findById(request.id())
            .orElseThrow(() -> new BaseException(ErrorCode.E_NOT_FOUND, HttpStatus.NOT_FOUND, "id", request.id()));

        if (!alert.getUserId().equals(currentUserId())) {
            throw new BaseException(ErrorCode.E_ACCESS_DENIED, HttpStatus.FORBIDDEN);
        }

        String symbol = alert.getSymbol();
        priceAlertRepository.delete(alert);
        
        return new DeleteAlertResponse(
            request.id(),
            symbol,
            "Alert deleted successfully",
            LocalDateTime.now()
        );
    }
}
