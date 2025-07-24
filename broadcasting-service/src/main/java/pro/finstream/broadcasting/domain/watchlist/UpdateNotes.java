package pro.finstream.broadcasting.domain.watchlist;

import pro.finstream.broadcasting.domain.watchlist.model.WatchlistRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.common.error.BaseException;
import pro.finstream.broadcasting.common.error.ErrorCode;
import pro.finstream.broadcasting.domain.Endpoints;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistItemEntity;

@RestController
@RequiredArgsConstructor
public class UpdateNotes implements CurrentUserAccessor {

    public record UpdateNotesRequest(
        @NotBlank(message = "Symbol is required")
        @Pattern(regexp = "^[A-Z0-9.-]{1,15}$", message = "Invalid stock symbol format")
        String symbol,

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
    ) {}

    private final WatchlistRepository repository;

    @PutMapping(Endpoints.API_WATCHLIST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNotes(@Valid @RequestBody UpdateNotesRequest request) {
        String normalizedSymbol = request.symbol().toUpperCase();
        
        WatchlistItemEntity item = repository.findByUserIdAndSymbol(currentUserId(), normalizedSymbol)
            .orElseThrow(() -> new BaseException(ErrorCode.E_NOT_FOUND, HttpStatus.NOT_FOUND, "symbol", normalizedSymbol));
        
        item.setNotes(request.notes());
        repository.saveAndFlush(item);
    }
}
