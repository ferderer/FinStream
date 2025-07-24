package pro.finstream.broadcasting.domain.alerts;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.domain.Endpoints;
import pro.finstream.broadcasting.domain.alerts.model.PriceAlertEntity;
import pro.finstream.broadcasting.domain.alerts.model.PriceAlertRepository;
import pro.finstream.broadcasting.domain.stocks.model.StockRepository;

@RestController
@RequiredArgsConstructor
public class LoadAlerts implements CurrentUserAccessor {

    public record AlertResponse(
        String id,
        String symbol,
        String companyName,
        String alertType,
        BigDecimal targetPrice,
        BigDecimal targetPercent,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime triggeredAt
    ) {}

    public record GetAlertsResponse(
        List<AlertResponse> alerts,
        int activeCount,
        int totalCount,
        LocalDateTime timestamp
    ) {}
    
    private final PriceAlertRepository priceAlertRepository;
    private final StockRepository stockRepository;

    @GetMapping(Endpoints.API_ALERTS)
    public GetAlertsResponse getAlerts() {
        List<PriceAlertEntity> alerts = priceAlertRepository.findByUserIdOrderByCreatedAtDesc(currentUserId());
        
        // Get company names for enrichment
        Set<String> symbols = alerts.stream()
            .map(PriceAlertEntity::getSymbol)
            .collect(Collectors.toSet());
        
        Map<String, String> companyNames = stockRepository.findBySymbolIn(symbols)
            .stream()
            .collect(Collectors.toMap(
                stock -> stock.getSymbol(),
                stock -> stock.getCompany()
            ));
        
        List<AlertResponse> alertResponses = alerts.stream()
            .map(alert -> new AlertResponse(
                alert.getId().toString(),
                alert.getSymbol(),
                companyNames.getOrDefault(alert.getSymbol(), alert.getSymbol()),
                alert.getAlertType().name(),
                alert.getTargetPrice(),
                alert.getTargetPercent(),
                alert.isActive(),
                alert.getCreated().atZone(java.time.ZoneOffset.UTC).toLocalDateTime(),
                alert.getTriggered() != null ? alert.getTriggered().atZone(java.time.ZoneOffset.UTC).toLocalDateTime() : null
            ))
            .toList();
        
        long activeCount = alerts.stream()
            .filter(PriceAlertEntity::isActive)
            .count();
        
        return new GetAlertsResponse(
            alertResponses,
            (int) activeCount,
            alerts.size(),
            LocalDateTime.now()
        );
    }
}
