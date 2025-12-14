// FineTransactionRepository.java
package com.intellilib.repositories;

import com.intellilib.models.FineTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FineTransactionRepository extends JpaRepository<FineTransaction, Long> {
    
    List<FineTransaction> findByMemberId(Long memberId);
    
    @Query("SELECT ft FROM FineTransaction ft WHERE ft.transactionDate BETWEEN :start AND :end")
    List<FineTransaction> findByDateRange(@Param("start") LocalDateTime start, 
                                          @Param("end") LocalDateTime end);
    
    @Query("SELECT SUM(ft.amount) FROM FineTransaction ft WHERE ft.status = 'COMPLETED'")
    Double getTotalCollectedFines();
    
    @Query("SELECT SUM(ft.amount) FROM FineTransaction ft WHERE ft.paymentMethod = 'WAIVED'")
    Double getTotalWaivedFines();
}