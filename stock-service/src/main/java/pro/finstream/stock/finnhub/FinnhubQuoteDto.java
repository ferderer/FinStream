package pro.finstream.stock.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubQuoteDto(
    @JsonProperty("c") BigDecimal currentPrice,
    @JsonProperty("d") BigDecimal change,
    @JsonProperty("dp") BigDecimal percentChange,
    @JsonProperty("h") BigDecimal highPrice,
    @JsonProperty("l") BigDecimal lowPrice,
    @JsonProperty("o") BigDecimal openPrice,
    @JsonProperty("pc") BigDecimal previousClose,
    @JsonProperty("t") Long timestamp
) {}
