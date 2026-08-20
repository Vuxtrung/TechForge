package com.swp391.techforge.dto.blog;

import com.swp391.techforge.entity.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) tiếp nhận dữ liệu thêm mới và cập nhật bài viết Blog
 */
@Getter
@Setter
@NoArgsConstructor
public class BlogRequest {

    private Long id;

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;

    private String slug;

    @NotBlank(message = "Vui lòng chọn danh mục bài viết")
    private String category;

    @NotBlank(message = "Tóm tắt bài viết không được để trống")
    private String summary;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content;

    private String thumbnailUrl;

    @NotNull(message = "Vui lòng chọn trạng thái bài viết")
    private BlogStatus status = BlogStatus.DRAFT;
}
