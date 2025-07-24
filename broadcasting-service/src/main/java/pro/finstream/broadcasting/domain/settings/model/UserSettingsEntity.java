package pro.finstream.broadcasting.domain.settings.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity(name = "UserSettings")
@Table(name = "settings")
@Getter
@Setter
public class UserSettingsEntity {
    @Id
    private Long userId;

    @Column(length = 50, nullable = false)
    private String timezone = "UTC";

    @Column(length = 3, nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private boolean notifications = true;

    @CreationTimestamp
    private Instant created;

    @UpdateTimestamp
    @Version
    private Instant modified;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UserSettingsEntity other && Objects.equals(userId, other.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
