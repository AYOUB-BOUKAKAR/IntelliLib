package com.intellilib.repositories;

import com.intellilib.models.Borrow;
import com.intellilib.models.Book;
import com.intellilib.models.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRepository extends JpaRepository<Borrow, Long> {
    // FREE: save(), findById(), findAll(), deleteById(), etc.
    
    // Find borrows by book
    List<Borrow> findByBook(Book book);
    
    // Find borrows by member
    List<Borrow> findByMember(Member member);
    
    // Find active borrows (not returned)
    List<Borrow> findByReturnedFalse();
    
    
    // Find borrows by book ID
    List<Borrow> findByBookId(Long bookId);
    
    // Find borrows by member ID
    List<Borrow> findByMemberId(Long memberId);
    
    long countByReturnedFalse();
    
    // Count active borrows for a specific member
    long countByMemberIdAndReturnedFalse(Long memberId);
    
    // Count total borrows for a specific member
    long countByMemberId(Long memberId);
    
    // Find overdue borrows
    @Query("SELECT b FROM Borrow b WHERE b.returned = false AND b.dueDate < :today")
    List<Borrow> findOverdueBorrows(LocalDate today);
    
    @Query("SELECT b FROM Borrow b WHERE b.returned = false AND b.dueDate < :today AND b.daysOverdue > :days")
    List<Borrow> findBorrowsOverdueByDays(@Param("today") LocalDate today, @Param("days") int days);
    
    default List<Borrow> findBorrowsOverdueByDays(int days) {
        return findBorrowsOverdueByDays(LocalDate.now(), days);
    }
    
    @Query("SELECT b FROM Borrow b WHERE b.member.id = :memberId AND b.fineStatus = 'PENDING' AND b.fineAmount > 0")
    List<Borrow> findPendingFinesByMember(@Param("memberId") Long memberId);
    
    @Query("SELECT b FROM Borrow b WHERE b.fineStatus = 'PENDING' AND b.fineAmount > 0")
    List<Borrow> findAllPendingFines();
    
    @Query("SELECT b FROM Borrow b WHERE b.member.id = :memberId AND b.returned = false")
    List<Borrow> findActiveBorrowsByMember(@Param("memberId") Long memberId);
}