package com.swp391.techforge.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffCreateRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 50, message = "Họ tên không vượt quá 50 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 50, message = "Email không vượt quá 50 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @Size(max = 10, message = "Số điện thoại không vượt quá 10 ký tự")
    private String phone;

    @NotNull(message = "Vui lòng chọn vai trò nhân viên")
    private Integer roleId;
}
