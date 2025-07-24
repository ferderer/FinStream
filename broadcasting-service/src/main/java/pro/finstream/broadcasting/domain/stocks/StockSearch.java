package pro.finstream.broadcasting.domain.stocks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.domain.Endpoints;
import pro.finstream.broadcasting.domain.stocks.model.StockEntity;
import pro.finstream.broadcasting.domain.stocks.model.StockRepository;

@RestController
@RequiredArgsConstructor
public class StockSearch {

    public record StockSearchResult(
        String symbol,
        String companyName,
        String sector,
        Long marketCap
    ) {}

    public record StockSearchResponse(
        List<StockSearchResult> results,
        int totalResults,
        int page,
        int size,
        boolean hasNext,
        String query,
        LocalDateTime timestamp
    ) {}

    private final StockRepository stockRepository;

    @GetMapping(Endpoints.API_SEARCH)
    public StockSearchResponse searchStocks(
        @RequestParam String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        String query = q.trim();
        
        if (query.length() < 1) {
            return new StockSearchResponse(List.of(), 0, page, size, false, query, LocalDateTime.now());
        }
        
        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1 || size > 100) size = 20;
        
        String searchPattern = "%" + query.toUpperCase() + "%";
        
        // Search by symbol and company name, combine results
        List<StockEntity> symbolMatches = stockRepository.findBySymbolContainingIgnoreCase(searchPattern);
        List<StockEntity> nameMatches = stockRepository.findByCompanyNameContainingIgnoreCase(searchPattern);
        
        // Combine and deduplicate results, prioritizing symbol matches
        List<StockEntity> allResults = Stream.concat(
                symbolMatches.stream(),
                nameMatches.stream()
                    .filter(stock -> symbolMatches.stream()
                        .noneMatch(existing -> existing.getSymbol().equals(stock.getSymbol())))
            )
            .toList();
        
        int totalResults = allResults.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalResults);
        
        List<StockSearchResult> pagedResults = allResults.stream()
            .skip(startIndex)
            .limit(size)
            .map(stock -> new StockSearchResult(
                stock.getSymbol(),
                stock.getCompany(),
                stock.getSector(),
                stock.getMarketCap()
            ))
            .toList();
        
        boolean hasNext = endIndex < totalResults;
        
        return new StockSearchResponse(
            pagedResults,
            totalResults,
            page,
            size,
            hasNext,
            query,
            LocalDateTime.now()
        );
    }
}
