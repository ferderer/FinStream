package pro.finstream.broadcasting.domain.alerts;

import pro.finstream.broadcasting.domain.alerts.model.AlertType;
import pro.finstream.broadcasting.domain.alerts.model.PriceAlertRepository;
import pro.finstream.broadcasting.domain.alerts.model.PriceAlertEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.common.error.BaseException;
import pro.finstream.broadcasting.common.error.ErrorCode;
import pro.finstream.broadcasting.domain.Endpoints;

@RestController
@RequiredArgsConstructor
public class UpdateAlert implements CurrentUserAccessor {

    public record UpdateAlertRequest(
        @NotBlank(message = "Alert ID is required")
        Long id,
        
        @NotNull(message = "Alert type is required")
        AlertType alertType,
        
        @DecimalMin(value = "0.01", message = "Target price must be greater than 0")
        @DecimalMax(value = "999999.99", message = "Target price cannot exceed 999,999.99")
        BigDecimal targetPrice,
        
        @DecimalMin(value = "-99.99", message = "Target percent cannot be less than -99.99%")
        @DecimalMax(value = "999.99", message = "Target percent cannot exceed 999.99%")
        BigDecimal targetPercent,
        
        @NotNull(message = "Active status is required")
        Boolean active
    ) {}

    public record UpdateAlertResponse(
        Long id,
        String symbol,
        AlertType alertType,
        BigDecimal targetPrice,
        BigDecimal targetPercent,
        boolean active,
        LocalDateTime updatedAt
    ) {}
    
    private final PriceAlertRepository priceAlertRepository;

    @PutMapping(Endpoints.API_ALERTS)
    public UpdateAlertResponse updateAlert(@Valid @RequestBody UpdateAlertRequest request) {
        PriceAlertEntity alert = priceAlertRepository.findById(request.id())
            .orElseThrow(() -> new BaseException(ErrorCode.E_NOT_FOUND, HttpStatus.NOT_FOUND, "id", request.id()));

        if (!alert.getUserId().equals(currentUserId())) {
            throw new BaseException(ErrorCode.E_ACCESS_DENIED,  "id", request.id());
        }
        validateAlertRequest(request);
        
        // Update alert fields
        alert.setAlertType(request.alertType());
        alert.setTargetPrice(request.targetPrice());
        alert.setTargetPercent(request.targetPercent());
        alert.setActive(request.active());
        
        // Reset triggered status if reactivating
        if (request.active() && alert.getTriggered() != null) {
            alert.setTriggered(null);
        }
        
        PriceAlertEntity updated = priceAlertRepository.save(alert);
        
        return new UpdateAlertResponse(
            updated.getId(),
            updated.getSymbol(),
            updated.getAlertType(),
            updated.getTargetPrice(),
            updated.getTargetPercent(),
            updated.isActive(),
            LocalDateTime.now()
        );
    }
    
    private void validateAlertRequest(UpdateAlertRequest request) {
        switch (request.alertType()) {
            case ABOVE, BELOW -> {
                if (request.targetPrice() == null) {
                    throw new BaseException(ErrorCode.E_TARGET_PRICE_REQUIRED, HttpStatus.BAD_REQUEST);
                }
            }
            case CHANGE -> {
                if (request.targetPercent() == null) {
                    throw new BaseException(ErrorCode.E_TARGET_PERCENT_REQUIRED, HttpStatus.BAD_REQUEST);
                }
            }
        }
    }
}
