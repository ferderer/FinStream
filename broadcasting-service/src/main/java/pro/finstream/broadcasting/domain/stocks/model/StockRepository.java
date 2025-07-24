package pro.finstream.broadcasting.domain.stocks.model;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<StockEntity, String> {
    
    List<StockEntity> findBySymbolIn(Set<String> symbols);
    
    @Query("SELECT s FROM Stock s WHERE s.symbol LIKE :pattern ORDER BY s.companyName")
    List<StockEntity> findBySymbolContainingIgnoreCase(@Param("pattern") String pattern);
    
    @Query("SELECT s FROM Stock s WHERE s.companyName LIKE :pattern ORDER BY s.companyName")
    List<StockEntity> findByCompanyNameContainingIgnoreCase(@Param("pattern") String pattern);
}
