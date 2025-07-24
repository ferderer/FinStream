package pro.finstream.broadcasting.domain.stocks.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity(name = "Stock")
@Table(name = "stocks")
@Getter
@Setter
public class StockEntity {
    @Id
    @Column(length = 15)
    private String symbol;

    @Column(length = 100, nullable = false)
    private String company;

    @Column(length = 100)
    private String sector;

    private Long marketCap;

    @UpdateTimestamp
    private Instant updated;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StockEntity other && Objects.equals(symbol, other.getSymbol());
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }
}

