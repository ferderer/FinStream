package pro.finstream.stock.finnhub;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.finstream.stock.messaging.KafkaProducerService;
import pro.finstream.stock.messaging.StockPriceEvent;

@ApplicationScoped
public class FinnhubService {
    private static Logger log = LoggerFactory.getLogger(FinnhubService.class);
    
    @RestClient
    FinnhubRestClient finnhubClient;

    @Inject
    KafkaProducerService kafkaProducer;

    @ConfigProperty(name = "finnhub.api-key")
    String apiKey;
    
    @ConfigProperty(name = "finnhub.symbols", defaultValue = "AAPL,GOOGL,MSFT")
    List<String> symbols;
    
    @Scheduled(every = "30s")  // Poll every 30 seconds
    public void fetchStockPrices() {
        log.info("Fetching stock prices for {} symbols", symbols.size());
        
        for (String symbol : symbols) {
            try {
                var quote = finnhubClient.getQuote(symbol, apiKey);
                log.info("Raw API response for {}: {}", symbol, quote);
                processQuote(symbol, quote);
            }
            catch (Exception e) {
                log.error("Failed to fetch quote for symbol: " + symbol, e);
            }
        }
    }
    
    private void processQuote(String symbol, FinnhubQuoteDto quote) {
        log.info("Quote for {}: price={}, change={}, changePercent={}, high={}, low={}", 
            symbol, quote.currentPrice(), quote.change(), quote.percentChange(),
            quote.highPrice(), quote.lowPrice());
        
        var event = new StockPriceEvent(symbol, quote.currentPrice(), quote.change(), quote.percentChange(),
            quote.highPrice(), quote.lowPrice(), quote.timestamp(), "finnhub", Instant.now());
        
        kafkaProducer.publishStockPrice(event);
        log.info("Published stock price event for {} to Kafka", symbol);
    }
}
