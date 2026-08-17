package com.swp391.techforge.service.warranty;

import com.swp391.techforge.entity.Role;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserStatus;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.repository.warranty.WarrantyTicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarrantyTicketServiceTest {

    @Mock
    private WarrantyTicketRepository warrantyTicketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WarrantyTicketService warrantyTicketService;

    // @Test
    // void receiveProduct_shouldCreateSubmittedTicket() {
    //     User user = new User();
    //     user.setUserId(1L);
    //     user.setFullName("Nguyễn Văn A");
    //     user.setEmail("a@example.com");
    //     user.setPasswordHash("hash");
    //     user.setStatus(UserStatus.ACTIVE);

    //     Role role = new Role();
    //     role.setRoleId(2);
    //     role.setRoleName("CUSTOMER");
    //     user.setRole(role);

    //     when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    //     when(warrantyTicketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    //     WarrantyTicket result = warrantyTicketService.receiveProduct(
    //             1L,
    //             "ABC123",
    //             "0909123456",
    //             "Máy không lên nguồn",
    //             null
    //     );

    //     assertEquals(WarrantyTicketStatus.SUBMITTED, result.getStatus());
    //     assertEquals("ABC123", result.getImeiSerial());
    //     assertEquals("Máy không lên nguồn", result.getIssueDesc());
    // }
}
