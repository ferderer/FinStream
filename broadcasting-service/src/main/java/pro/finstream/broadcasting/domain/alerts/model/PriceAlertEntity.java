package pro.finstream.broadcasting.domain.alerts.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity(name = "PriceAlert")
@Table(name = "alerts")
@Getter
@Setter
public class PriceAlertEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 15, nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(length = 8, nullable = false)
    private AlertType alertType;

    @Column(precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal targetPercent;

    @CreationTimestamp
    private Instant created;
    private Instant triggered;
    private boolean active = true;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PriceAlertEntity other && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
