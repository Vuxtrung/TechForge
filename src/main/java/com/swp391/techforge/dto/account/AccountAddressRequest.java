package com.swp391.techforge.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountAddressRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có 10 số")
    private String phone;

    @NotBlank(message = "Vui lòng chọn tỉnh/thành phố")
    private String province;

    @NotBlank(message = "Vui lòng chọn phường/xã")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(max = 255, message = "Địa chỉ chi tiết không được vượt quá 255 ký tự")
    private String addressLine;

    @NotBlank(message = "Vui lòng chọn loại địa chỉ")
    private String type;

    private boolean defaultAddress;
}