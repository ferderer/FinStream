package pro.finstream.broadcasting.domain.watchlist;

import pro.finstream.broadcasting.domain.watchlist.model.WatchlistRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.common.error.BaseException;
import pro.finstream.broadcasting.common.error.ErrorCode;
import pro.finstream.broadcasting.domain.Endpoints;

@RestController
@RequiredArgsConstructor
public class DeleteItem implements CurrentUserAccessor {

    private final WatchlistRepository watchlistRepository;

    @DeleteMapping(Endpoints.API_WATCHLIST)
    public String removeFromWatchlist(
        @NotBlank(message = "Symbol is required")
        @Pattern(regexp = "^[A-Z0-9.-]{1,15}$", message = "Invalid stock symbol format")
        String symbol
    ) {
        String normalizedSymbol = symbol.toUpperCase();
        
        if (!watchlistRepository.existsByUserIdAndSymbol(currentUserId(), normalizedSymbol)) {
            throw new BaseException(ErrorCode.E_NOT_FOUND, HttpStatus.NOT_FOUND, "symbol", normalizedSymbol);
        }
        watchlistRepository.deleteByUserIdAndSymbol(currentUserId(), normalizedSymbol);
        return normalizedSymbol;
    }
}
