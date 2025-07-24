package pro.finstream.broadcasting.domain.watchlist.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WatchlistItem (
    long id,
    String symbol, 
    String companyName,
    String notes,
    LocalDateTime created,
    BigDecimal currentPrice,
    BigDecimal change,
    BigDecimal changePercent
){}
