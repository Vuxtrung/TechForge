package com.swp391.techforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "warranty_tickets")
@Getter
@Setter
public class WarrantyTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "imei_serial", nullable = false, length = 100)
    private String imeiSerial;

    @Column(name = "phone_lookup", length = 20)
    private String phoneLookup;

    @Lob
    @Column(name = "issue_desc")
    private String issueDesc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WarrantyTicketStatus status = WarrantyTicketStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
