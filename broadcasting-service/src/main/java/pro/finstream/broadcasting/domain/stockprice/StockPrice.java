package pro.finstream.broadcasting.domain.stockprice;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockPrice(
    String symbol,
    BigDecimal price,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal high,
    BigDecimal low,
    Long timestamp,
    String source,
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    LocalDateTime processedAt
) {
    
    /**
     * Utility method to check if the price represents a positive change.
     */
    public boolean isPositiveChange() {
        return change != null && change.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Utility method to check if the price represents a negative change.
     */
    public boolean isNegativeChange() {
        return change != null && change.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Format the price change for display (with + for positive values).
     */
    public String getFormattedChange() {
        if (change == null) return "0.00";
        return (change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + change.toString();
    }
    
    /**
     * Format the percentage change for display (with + for positive values).
     */
    public String getFormattedChangePercent() {
        if (changePercent == null) return "0.00%";
        return (changePercent.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + changePercent + "%";
    }
}
