package pro.finstream.stock.messaging;

import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KafkaProducerService {
    
    private static Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    
    @Channel("stock-prices")
    Emitter<Record<String, StockPriceEvent>> stockPricesEmitter;
    
    public void publishStockPrice(StockPriceEvent event) {
        try {
            log.info("Attempting to publish stock price event for {}", event.symbol());
            stockPricesEmitter.send(Record.of(event.symbol(), event))
                .whenComplete((success, failure) -> {
                    if (failure != null) {
                        log.error("Failed to publish event for " + event.symbol(), failure);
                    }
                    else {
                        log.info("Successfully published event for {}", event.symbol());
                    }
                });
        }
        catch (Exception e) {
            log.error("Exception while publishing event for " + event.symbol(), e);
        }
    }
}
