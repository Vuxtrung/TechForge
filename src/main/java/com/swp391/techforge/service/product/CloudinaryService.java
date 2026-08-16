package com.swp391.techforge.service.product;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Upload ảnh vào folder mặc định "techforge/products" (giữ nguyên hành vi cũ).
     */
    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, "techforge/products");
    }

    /**
     * Upload ảnh vào folder chỉ định, trả về secure_url.
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", folder)
        );
        return (String) result.get("secure_url");
    }
}