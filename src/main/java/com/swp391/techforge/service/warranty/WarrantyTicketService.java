package com.swp391.techforge.service.warranty;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.WarrantyTicket;
import com.swp391.techforge.entity.WarrantyTicketStatus;
import com.swp391.techforge.entity.OrderItem;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.order.OrderItemRepository;
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
    private final OrderItemRepository orderItemRepository;

    public WarrantyTicketService(WarrantyTicketRepository warrantyTicketRepository,
                                UserRepository userRepository,
                                OrderItemRepository orderItemRepository) {
        this.warrantyTicketRepository = warrantyTicketRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
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

    @Transactional(readOnly = true)
    public List<WarrantyTicket> lookupForCustomer(String query) {
        return warrantyTicketRepository.findByImeiSerialIgnoreCaseOrPhoneLookup(query, query);
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
    public WarrantyTicket createForCustomer(Long orderItemId, User customer, String imeiSerial, String issueDesc) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong đơn hàng."));
        if (customer == null || orderItem.getOrder() == null || orderItem.getOrder().getUser() == null
                || !orderItem.getOrder().getUser().getUserId().equals(customer.getUserId())) {
            throw new IllegalArgumentException("Bạn không có quyền tạo phiếu cho sản phẩm này.");
        }
        if (imeiSerial == null || imeiSerial.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập IMEI hoặc Serial của sản phẩm.");
        }

        WarrantyTicket ticket = new WarrantyTicket();
        ticket.setUser(customer);
        ticket.setOrderItemId(orderItemId);
        ticket.setImeiSerial(imeiSerial.trim());
        ticket.setPhoneLookup(customer.getPhone());
        ticket.setIssueDesc(issueDesc);
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
