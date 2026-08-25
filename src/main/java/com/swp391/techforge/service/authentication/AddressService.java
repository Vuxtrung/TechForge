package com.swp391.techforge.service.authentication;

import com.swp391.techforge.dto.account.AccountAddressRequest;
import com.swp391.techforge.entity.AddressType;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.UserAddress;
import com.swp391.techforge.repository.authentication.UserAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final UserAddressRepository userAddressRepository;

    public AddressService(UserAddressRepository userAddressRepository) {
        this.userAddressRepository = userAddressRepository;
    }

    public List<UserAddress> getAddressesForUser(Long userId) {
        return userAddressRepository.findByUserUserIdOrderByIsDefaultDescAddressIdDesc(userId);
    }

    public void prepareAddressRequest(AccountAddressRequest request, User user, Long addressId, String mode) {
        request.setRecipientName(user.getFullName());
        request.setPhone(user.getPhone());
        request.setType(AddressType.HOME.name());

        if (addressId != null && ("edit".equals(mode) || "details".equals(mode))) {
            userAddressRepository.findByAddressIdAndUserUserId(addressId, user.getUserId())
                    .ifPresent(address -> {
                        request.setRecipientName(address.getRecipientName());
                        request.setPhone(address.getPhone());
                        request.setProvince(address.getProvince());
                        request.setWard(address.getWard());
                        request.setAddressLine(address.getAddressLine());
                        request.setType(address.getType().name());
                        request.setDefaultAddress(address.isDefault());
                    });
        }
    }

    @Transactional
    public void saveAddress(Long userId, Long addressId, AccountAddressRequest request) {
        UserAddress address;

        if (addressId == null) {
            address = new UserAddress();
            User userRef = new User();
            userRef.setUserId(userId);
            address.setUser(userRef);
        } else {
            address = userAddressRepository.findByAddressIdAndUserUserId(addressId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ"));
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setWard(request.getWard());
        address.setAddressLine(request.getAddressLine());
        address.setType(AddressType.valueOf(request.getType()));

        if (request.isDefaultAddress()) {
            updateDefaultFlag(userId, address);
        } else if (addressId == null
                && userAddressRepository.findByUserUserIdAndIsDefaultTrue(userId).isEmpty()) {
            address.setDefault(true);
        }

        userAddressRepository.save(address);
    }

    @SuppressWarnings("null")
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ"));

        updateDefaultFlag(userId, address);
        userAddressRepository.save(address);
    }

    @Transactional
    public boolean deleteAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ"));

        if (address.isDefault()) {
            return false;
        }

        userAddressRepository.delete(address);
        return true;
    }

    private void updateDefaultFlag(Long userId, UserAddress address) {
        userAddressRepository.findByUserUserIdAndIsDefaultTrue(userId)
                .filter(current -> !current.getAddressId().equals(address.getAddressId()))
                .ifPresent(current -> {
                    current.setDefault(false);
                    userAddressRepository.saveAndFlush(current);
                });
        address.setDefault(true);
    }
}