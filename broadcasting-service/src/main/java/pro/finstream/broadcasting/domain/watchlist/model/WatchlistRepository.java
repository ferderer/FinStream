package pro.finstream.broadcasting.domain.watchlist.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pro.finstream.broadcasting.domain.watchlist.model.WatchlistItemEntity;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItemEntity, Long> {
    
    List<WatchlistItemEntity> findByUserIdOrderByAddedAtDesc(Long userId);
    
    Optional<WatchlistItemEntity> findByUserIdAndSymbol(Long userId, String symbol);
    
    boolean existsByUserIdAndSymbol(Long userId, String symbol);
    
    void deleteByUserIdAndSymbol(Long userId, String symbol);
    
    long countByUserId(Long userId);
    
    @Query("SELECT w.symbol FROM WatchlistItem w WHERE w.userId = :userId")
    Set<String> findSymbolsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT w.symbol FROM WatchlistItem w")
    Set<String> findAllDistinctSymbols();
}
