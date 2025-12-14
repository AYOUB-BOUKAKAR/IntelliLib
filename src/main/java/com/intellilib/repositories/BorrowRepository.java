package com.intellilib.repositories;

import com.intellilib.models.Borrow;
import com.intellilib.models.Book;
import com.intellilib.models.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    
    // Find overdue borrows
    @Query("SELECT b FROM Borrow b WHERE b.returned = false AND b.dueDate < :today")
    List<Borrow> findOverdueBorrows(LocalDate today);
    
    // Find borrows by book ID
    List<Borrow> findByBookId(Long bookId);
    
    // Find borrows by member ID
    List<Borrow> findByMemberId(Long memberId);

    long countByReturnedFalse();

    // NEW METHODS NEEDED FOR BORROWSERVICE

    // Count active borrows for a specific member
    long countByMemberIdAndReturnedFalse(Long memberId);

    // Count total borrows for a specific member
    long countByMemberId(Long memberId);
}