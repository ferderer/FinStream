package pro.finstream.broadcasting.domain.settings;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.finstream.broadcasting.common.auth.CurrentUserAccessor;
import pro.finstream.broadcasting.domain.Endpoints;
import pro.finstream.broadcasting.domain.settings.model.UserSettingsEntity;
import pro.finstream.broadcasting.domain.settings.model.UserSettingsRepository;

@RestController
@RequiredArgsConstructor
public class LoadSettings implements CurrentUserAccessor {

    public record UserSettingsResponse(
        Long userId,
        String timezone,
        String currency,
        boolean notificationsEnabled,
        LocalDateTime lastUpdated
    ) {}

    private final UserSettingsRepository userSettingsRepository;

    @GetMapping(Endpoints.API_SETTINGS)
    public UserSettingsResponse getSettings() {
        Long userId = currentUserId();
        
        // Get user settings or create defaults if not exists
        UserSettingsEntity settings = userSettingsRepository.findById(userId)
            .orElseGet(() -> createDefaultSettings(userId));
        
        return new UserSettingsResponse(
            settings.getUserId(),
            settings.getTimezone(),
            settings.getCurrency(),
            settings.isNotifications(),
            settings.getModified() != null 
                ? settings.getModified().atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
                : null
        );
    }
    
    private UserSettingsEntity createDefaultSettings(Long userId) {
        UserSettingsEntity defaultSettings = new UserSettingsEntity();
        defaultSettings.setUserId(userId);
        defaultSettings.setTimezone("UTC");
        defaultSettings.setCurrency("USD");
        defaultSettings.setNotifications(true);
        
        return userSettingsRepository.save(defaultSettings);
    }
}
