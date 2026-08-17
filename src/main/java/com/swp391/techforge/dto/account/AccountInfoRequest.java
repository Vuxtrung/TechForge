package com.swp391.techforge.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AccountInfoRequest {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^([0-9]{10})?$", message = "Số điện thoại phải gồm 10 chữ số")
    private String phone;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    private MultipartFile avatarFile;
}