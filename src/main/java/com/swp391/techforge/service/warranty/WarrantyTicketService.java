package com.swp391.techforge.service.warranty;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.WarrantyTicket;
import com.swp391.techforge.entity.WarrantyTicketStatus;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.warranty.WarrantyTicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WarrantyTicketService {

    private final WarrantyTicketRepository warrantyTicketRepository;
    private final UserRepository userRepository;

    public WarrantyTicketService(WarrantyTicketRepository warrantyTicketRepository,
                                UserRepository userRepository) {
        this.warrantyTicketRepository = warrantyTicketRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<WarrantyTicket> search(String keyword, String status, int page, int size, Sort sort) {
        WarrantyTicketStatus ticketStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                ticketStatus = WarrantyTicketStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                ticketStatus = null;
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        return warrantyTicketRepository.search(keyword, ticketStatus, pageable);
    }

    @Transactional(readOnly = true)
    public List<WarrantyTicket> getAllForExport(String keyword, String status) {
        WarrantyTicketStatus ticketStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                ticketStatus = WarrantyTicketStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                ticketStatus = null;
            }
        }

        return warrantyTicketRepository.search(keyword, ticketStatus, Pageable.unpaged()).getContent();
    }

    @Transactional(readOnly = true)
    public WarrantyTicket getById(Long id) {
        return warrantyTicketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu bảo hành."));
    }

    @Transactional
    public WarrantyTicket receiveProduct(Long userId, String imeiSerial, String phoneLookup,
                                        String issueDesc, Long orderItemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng."));

        WarrantyTicket ticket = new WarrantyTicket();
        ticket.setUser(user);
        ticket.setImeiSerial(imeiSerial);
        ticket.setPhoneLookup(phoneLookup);
        ticket.setIssueDesc(issueDesc);
        ticket.setOrderItemId(orderItemId);
        ticket.setStatus(WarrantyTicketStatus.SUBMITTED);
        return warrantyTicketRepository.save(ticket);
    }

    @Transactional
    public WarrantyTicket updateProgress(Long ticketId, String status, String assignedStaffId) {
        WarrantyTicket ticket = getById(ticketId);

        if (status != null && !status.isBlank()) {
            ticket.setStatus(WarrantyTicketStatus.valueOf(status));
        }

        if (assignedStaffId != null && !assignedStaffId.isBlank()) {
            Long staffId = Long.parseLong(assignedStaffId);
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên kỹ thuật."));
            ticket.setAssignedStaff(staff);
        }

        if (ticket.getStatus() == WarrantyTicketStatus.REPAIRED ||
                ticket.getStatus() == WarrantyTicketStatus.REPLACED_1_1 ||
                ticket.getStatus() == WarrantyTicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        return warrantyTicketRepository.save(ticket);
    }

    @Transactional
    public WarrantyTicket markReplaced1For1(Long ticketId) {
        WarrantyTicket ticket = getById(ticketId);
        ticket.setStatus(WarrantyTicketStatus.REPLACED_1_1);
        ticket.setResolvedAt(LocalDateTime.now());
        return warrantyTicketRepository.save(ticket);
    }

    @Transactional
    public WarrantyTicket markRepaired(Long ticketId) {
        WarrantyTicket ticket = getById(ticketId);
        ticket.setStatus(WarrantyTicketStatus.REPAIRED);
        ticket.setResolvedAt(LocalDateTime.now());
        return warrantyTicketRepository.save(ticket);
    }

    @Transactional
    public WarrantyTicket closeTicket(Long ticketId) {
        WarrantyTicket ticket = getById(ticketId);
        ticket.setStatus(WarrantyTicketStatus.CLOSED);
        ticket.setResolvedAt(LocalDateTime.now());
        return warrantyTicketRepository.save(ticket);
    }
}
