package pro.finstream.broadcasting.domain.stockprice;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import pro.finstream.broadcasting.common.error.BaseException;
import pro.finstream.broadcasting.common.error.ErrorCode;
import pro.finstream.broadcasting.domain.stomp.StockBroadcastingService;

@Service
@Slf4j
public class StockPriceConsumerService {

    private final StockBroadcastingService broadcastingService;

    public StockPriceConsumerService(StockBroadcastingService broadcastingService) {
        this.broadcastingService = broadcastingService;
    }
    
    @KafkaListener(topics = "finstream.stock.prices", groupId = "finstream-broadcaster")
    public void consumeStockPrice(
        @Payload StockPrice stockPrice,
        ConsumerRecord<String, StockPrice> record,
        Acknowledgment acknowledgment,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp
    ) {    
        try {
            log.debug("Received stock price message: partition={}, offset={}, timestamp={}, key={}", 
                     partition, offset, timestamp, record.key());
            
            log.info("Processing stock price update: symbol={}, price={}, change={}", 
                    stockPrice.symbol(), stockPrice.price(), stockPrice.change());
            validateStockPrice(stockPrice);
            broadcastingService.broadcastStockPrice(stockPrice);
            acknowledgment.acknowledge();
            log.debug("Successfully processed and acknowledged stock price for symbol: {}", stockPrice.symbol());            
        }
        catch (Exception e) {
            log.error("Failed to process stock price message: partition={}, offset={}, stockPrice={}", 
                     partition, offset, stockPrice, e);
            
            // For now, acknowledge even failed messages to prevent infinite retry
            // In production, you might want to send to a dead letter topic
            acknowledgment.acknowledge();
            
            throw BaseException.kafkaError(
                ErrorCode.E_KAFKA_MESSAGE_DESERIALIZATION_ERROR,
                "topic", "finstream.stock.prices",
                "group-id", "finstream-broadcaster",
                "partition", partition,
                "offset", offset,
                "error", e.getMessage()
            );
        }
    }

    private void validateStockPrice(StockPrice stockPrice) {
        if (stockPrice.symbol() == null || stockPrice.symbol().trim().isEmpty()) {
            throw BaseException.stockDataError(
                ErrorCode.E_STOCK_PRICE_MISSING_SYMBOL,
                stockPrice.symbol(),
                "reason", "Symbol is null or empty"
            );
        }
        
        if (stockPrice.price() == null) {
            throw BaseException.stockDataError(
                ErrorCode.E_STOCK_PRICE_INVALID_FORMAT,
                stockPrice.symbol(),
                "reason", "Price is null"
            );
        }
        
        log.debug("Stock price validation passed for symbol: {}", stockPrice.symbol());
    }
}
