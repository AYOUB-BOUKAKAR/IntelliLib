package com.intellilib.services;

import com.intellilib.models.Borrow;
import com.intellilib.models.Book;
import com.intellilib.models.Member;
import com.intellilib.repositories.BorrowRepository;
import com.intellilib.repositories.BookRepository;
import com.intellilib.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BorrowService {
    
    private final BorrowRepository borrowRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public List<Borrow> getAllBorrows(){
        return borrowRepository.findAll();
    }
    
    public Borrow saveBorrow(Borrow borrow) {
        Optional<Book> book = bookRepository.findById(borrow.getBook().getId());
        Optional<Member> member = memberRepository.findById(borrow.getMember().getId());
        
        if (book.isEmpty() || member.isEmpty() || !book.get().isAvailable()) {
            throw new RuntimeException("Cannot borrow book");
        }
        return borrowRepository.save(borrow);
    }
    
    public void deleteBorrow(Long borrowId){
        borrowRepository.deleteById(borrowId);
    }
    
    public Borrow borrowBook(Long bookId, Long memberId, LocalDate dueDate) {
        Optional<Book> book = bookRepository.findById(bookId);
        Optional<Member> member = memberRepository.findById(memberId);
        
        if (book.isEmpty() || member.isEmpty() || !book.get().isAvailable()) {
            throw new RuntimeException("Cannot borrow book");
        }
        
        Book bookToBorrow = book.get();
        bookToBorrow.setAvailable(false);
        bookRepository.save(bookToBorrow);
        
        Borrow borrow = new Borrow(bookToBorrow, member.get(), dueDate);
        return borrowRepository.save(borrow);
    }

    public boolean returnBook(Long borrowId) {
        Optional<Borrow> borrow = borrowRepository.findById(borrowId);
        if (borrow.isPresent() && !borrow.get().isReturned()) {
            Borrow borrowRecord = borrow.get();
            borrowRecord.setReturned(true);
            borrowRecord.setReturnDate(LocalDate.now());
            
            Book book = borrowRecord.getBook();
            book.setAvailable(true);
            
            bookRepository.save(book);
            borrowRepository.save(borrowRecord);
            return true;
        }
        return false;
    }

    public List<Borrow> getBorrowsByMember(Long memberId) {
        return borrowRepository.findByMemberId(memberId);
    }
    
    public List<Borrow> getActiveBorrows() {
        return borrowRepository.findByReturnedFalse();
    }

    public long countActiveBorrowings() {
        return borrowRepository.countByReturnedFalse();
    }

    public long countOverdueBooks() {
        return borrowRepository.findOverdueBorrows(LocalDate.now()).size();
    }

    public double calculateTotalFines() {
        return borrowRepository.findAll().stream()
            .mapToDouble(Borrow::getFineAmount)
            .sum();
    }

    // NEW IMPLEMENTATIONS FOR MEMBER DASHBOARD

    public long countActiveBorrowingsForMember(Long memberId) {
        return borrowRepository.countByMemberIdAndReturnedFalse(memberId);
    }

    public long countOverdueBooksForMember(Long memberId) {
        LocalDate today = LocalDate.now();
        List<Borrow> memberBorrows = borrowRepository.findByMemberId(memberId);

        return memberBorrows.stream()
            .filter(borrow -> !borrow.isReturned() && borrow.getDueDate().isBefore(today))
            .count();
    }

    public double calculateFinesForMember(Long memberId) {
        List<Borrow> memberBorrows = borrowRepository.findByMemberId(memberId);

        return memberBorrows.stream()
            .mapToDouble(Borrow::getFineAmount)
            .sum();
    }

    public List<Borrow> getRecentBorrowingsForMember(Long memberId, int limit) {
        List<Borrow> allMemberBorrows = borrowRepository.findByMemberId(memberId);

        // Sort by borrow date descending and limit
        return allMemberBorrows.stream()
            .sorted((b1, b2) -> b2.getBorrowDate().compareTo(b1.getBorrowDate()))
            .limit(limit)
            .toList();
    }

    public long countTotalBorrowsForMember(Long memberId) {
        return borrowRepository.countByMemberId(memberId);
    }
}