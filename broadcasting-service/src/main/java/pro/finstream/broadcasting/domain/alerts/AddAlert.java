package pro.finstream.broadcasting.domain.alerts;

import pro.finstream.broadcasting.domain.alerts.model.AlertType;
import pro.finstream.broadcasting.domain.alerts.model.PriceAlertRepository;
import pro.finstream.broadcasting.domain.alerts.model.PriceAlertEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.common.error.BaseException;
import pro.finstream.broadcasting.common.error.ErrorCode;
import static pro.finstream.broadcasting.domain.Endpoints.API_ALERTS;

@RestController
@RequiredArgsConstructor
public class AddAlert implements CurrentUserAccessor {

    public record CreateAlertRequest(
        @NotBlank(message = "Symbol is required")
        @Pattern(regexp = "^[A-Z0-9.-]{1,15}$", message = "Invalid stock symbol format")
        String symbol,
        
        @NotNull(message = "Alert type is required")
        AlertType alertType,
        
        @DecimalMin(value = "0.01", message = "Target price must be greater than 0")
        @DecimalMax(value = "999999.99", message = "Target price cannot exceed 999,999.99")
        BigDecimal targetPrice,
        
        @DecimalMin(value = "-99.99", message = "Target percent cannot be less than -99.99%")
        @DecimalMax(value = "999.99", message = "Target percent cannot exceed 999.99%")
        BigDecimal targetPercent
    ) {}

    public record CreateAlertResponse(
        String id,
        String symbol,
        String alertType,
        BigDecimal targetPrice,
        BigDecimal targetPercent,
        LocalDateTime created
    ) {}
    
    private final PriceAlertRepository priceAlertRepository;

    @PostMapping(API_ALERTS)
    public CreateAlertResponse createAlert(@Valid @RequestBody CreateAlertRequest request) {
        String normalizedSymbol = request.symbol().toUpperCase();
        
        // Validate alert type and required fields
        validateAlertRequest(request);
        
        // Create new alert
        PriceAlertEntity alert = new PriceAlertEntity();
        alert.setUserId(currentUserId());
        alert.setSymbol(normalizedSymbol);
        alert.setAlertType(request.alertType());
        alert.setTargetPrice(request.targetPrice());
        alert.setTargetPercent(request.targetPercent());
        alert.setActive(true);
        
        PriceAlertEntity saved = priceAlertRepository.save(alert);
        
        return new CreateAlertResponse(
            saved.getId().toString(),
            saved.getSymbol(),
            saved.getAlertType().name(),
            saved.getTargetPrice(),
            saved.getTargetPercent(),
            saved.getCreated().atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
        );
    }
    
    private void validateAlertRequest(CreateAlertRequest request) {
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