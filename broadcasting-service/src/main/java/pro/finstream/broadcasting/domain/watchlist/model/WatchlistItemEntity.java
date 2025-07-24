package pro.finstream.broadcasting.domain.watchlist.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity(name = "WatchlistItem")
@Table(name = "watchlist")
@Getter
@Setter
public class WatchlistItemEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 15, nullable = false)
    private String symbol;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime added;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WatchlistItemEntity other && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
