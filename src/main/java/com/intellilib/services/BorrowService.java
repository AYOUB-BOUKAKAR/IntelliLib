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
}