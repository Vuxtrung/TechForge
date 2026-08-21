package com.swp391.techforge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "province", nullable = false, length = 100)
    private String province;

    @Column(name = "ward", nullable = false, length = 100)
    private String ward;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AddressType type = AddressType.HOME;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;
}