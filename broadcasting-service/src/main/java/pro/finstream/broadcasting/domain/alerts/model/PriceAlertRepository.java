package pro.finstream.broadcasting.domain.alerts.model;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlertEntity, Long> {
    
    List<PriceAlertEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<PriceAlertEntity> findByUserIdAndSymbol(Long userId, String symbol);
    
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.active = true AND pa.symbol IN :symbols")
    List<PriceAlertEntity> findActiveAlertsBySymbols(@Param("symbols") Set<String> symbols);
    
    @Query("SELECT pa FROM PriceAlert pa WHERE pa.active = true AND pa.symbol = :symbol")
    List<PriceAlertEntity> findActiveAlertsBySymbol(@Param("symbol") String symbol);
    
    long countByUserIdAndActive(Long userId, boolean active);
    
    @Query("SELECT DISTINCT pa.symbol FROM PriceAlert pa WHERE pa.active = true")
    Set<String> findAllActiveAlertSymbols();
}
