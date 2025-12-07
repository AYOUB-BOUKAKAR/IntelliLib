package com.intellilib.services;

import com.intellilib.models.Member;
import com.intellilib.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public Member createMember(Member member) {
        // Validate unique fields
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new RuntimeException("Email '" + member.getEmail() + "' is already registered");
        }
        if (memberRepository.existsByPhone(member.getPhone())) {
            throw new RuntimeException("Phone number '" + member.getPhone() + "' is already registered");
        }
        
        // Set default values if not provided
        if (member.getMembershipDate() == null) {
            member.setMembershipDate(LocalDate.now());
        }
        
        return memberRepository.save(member);
    }

    public Member updateMember(Long id, Member memberDetails) {
        return memberRepository.findById(id)
                .map(existingMember -> {
                    // Check if email is being changed and already exists
                    if (!existingMember.getEmail().equals(memberDetails.getEmail()) 
                            && memberRepository.existsByEmail(memberDetails.getEmail())) {
                        throw new RuntimeException("Email '" + memberDetails.getEmail() + "' is already registered");
                    }
                    
                    // Check if phone is being changed and already exists
                    if (!existingMember.getPhone().equals(memberDetails.getPhone()) 
                            && memberRepository.existsByPhone(memberDetails.getPhone())) {
                        throw new RuntimeException("Phone number '" + memberDetails.getPhone() + "' is already registered");
                    }
                    
                    existingMember.setFullName(memberDetails.getFullName());
                    existingMember.setEmail(memberDetails.getEmail());
                    existingMember.setPhone(memberDetails.getPhone());
                    existingMember.setAddress(memberDetails.getAddress());
                    existingMember.setMembershipExpiry(memberDetails.getMembershipExpiry());
                    existingMember.setActive(memberDetails.isActive());
                    
                    return memberRepository.save(existingMember);
                })
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
    }

    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    public Optional<Member> getMemberById(Long id) {
        return memberRepository.findById(id);
    }

    public Member getMemberByIdOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with id: " + id));
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public List<Member> getActiveMembers() {
        return memberRepository.findByActiveTrue();
    }

    public List<Member> getInactiveMembers() {
        return memberRepository.findByActiveFalse();
    }

    public List<Member> searchMembersByName(String name) {
        return memberRepository.findByFullNameContainingIgnoreCase(name);
    }

    public Optional<Member> getMemberByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

    public Optional<Member> getMemberByPhone(String phone) {
        return memberRepository.findByPhone(phone);
    }

    public void deactivateMember(Long id) {
        memberRepository.findById(id).ifPresent(member -> {
            member.setActive(false);
            memberRepository.save(member);
        });
    }

    public void activateMember(Long id) {
        memberRepository.findById(id).ifPresent(member -> {
            member.setActive(true);
            memberRepository.save(member);
        });
    }

    public boolean memberExists(String email, String phone) {
        return memberRepository.existsByEmail(email) || memberRepository.existsByPhone(phone);
    }

    public long getTotalMembers() {
        return memberRepository.count();
    }

    public long getActiveMembersCount() {
        return memberRepository.findByActiveTrue().size();
    }
}