package pro.finstream.broadcasting.domain.watchlist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.common.error.BaseException;
import pro.finstream.broadcasting.common.error.ErrorCode;
import static pro.finstream.broadcasting.domain.Endpoints.API_WATCHLIST;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistItemEntity;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistRepository;

@RestController
@RequiredArgsConstructor
public class AddItem implements CurrentUserAccessor {

    public record AddWatchlistRequest(
        @NotBlank(message = "Symbol is required")
        @Pattern(regexp = "^[A-Z0-9.-]{1,15}$", message = "Invalid stock symbol format")
        String symbol,

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
    ) {}

    public record AddWatchlistResponse(
        long id, String symbol, String notes, LocalDateTime created
    ) {}
    
    private final WatchlistRepository watchlistRepository;

    @PostMapping(API_WATCHLIST)
    public AddWatchlistResponse addToWatchlist(@Valid @RequestBody AddWatchlistRequest request) {
        if (watchlistRepository.existsByUserIdAndSymbol(currentUserId(), request.symbol())) {
            throw new BaseException(ErrorCode.E_ALREADY_ADDED, HttpStatus.BAD_REQUEST, "symbol", request.symbol());
        }
        
        // Create new watchlist item
        WatchlistItemEntity item = new WatchlistItemEntity();
        item.setUserId(currentUserId());
        item.setSymbol(request.symbol().toUpperCase());
        item.setNotes(request.notes());
        
        WatchlistItemEntity saved = watchlistRepository.save(item);
        
        return new AddWatchlistResponse(saved.getId(), saved.getSymbol(), saved.getNotes(), saved.getAdded());
    }
}
