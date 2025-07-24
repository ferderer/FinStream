package pro.finstream.broadcasting.domain.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.domain.Endpoints;
import pro.finstream.broadcasting.domain.settings.model.UserSettingsEntity;
import pro.finstream.broadcasting.domain.settings.model.UserSettingsRepository;

@RestController
@RequiredArgsConstructor
public class UpdateSettings implements CurrentUserAccessor {

    public record UpdateSettingsRequest(
        @NotBlank(message = "Timezone is required")
        @Size(max = 50, message = "Timezone cannot exceed 50 characters")
        String timezone,
        
        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g., USD, EUR)")
        String currency,
        
        @NotNull(message = "Notifications enabled flag is required")
        Boolean notificationsEnabled
    ) {}

    public record UpdateSettingsResponse(
        Long userId,
        String timezone,
        String currency,
        boolean notificationsEnabled,
        LocalDateTime updatedAt
    ) {}

    private final UserSettingsRepository userSettingsRepository;

    @PutMapping(Endpoints.API_SETTINGS)
    public UpdateSettingsResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        Long userId = currentUserId();
        
        // Get existing settings or create new
        UserSettingsEntity settings = userSettingsRepository.findById(userId)
            .orElse(new UserSettingsEntity());
        
        // Update fields
        settings.setUserId(userId);
        settings.setTimezone(request.timezone());
        settings.setCurrency(request.currency().toUpperCase());
        settings.setNotifications(request.notificationsEnabled());
        
        UserSettingsEntity updated = userSettingsRepository.save(settings);
        
        return new UpdateSettingsResponse(
            updated.getUserId(),
            updated.getTimezone(),
            updated.getCurrency(),
            updated.isNotifications(),
            LocalDateTime.now()
        );
    }
}
