package com.swp391.techforge.service.order;

import com.swp391.techforge.entity.DiscountType;
import com.swp391.techforge.entity.Order;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.entity.Voucher;
import com.swp391.techforge.entity.VoucherUsage;
import com.swp391.techforge.repository.order.VoucherRepository;
import com.swp391.techforge.repository.order.VoucherUsageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    public VoucherService(VoucherRepository voucherRepository,
                           VoucherUsageRepository voucherUsageRepository) {
        this.voucherRepository = voucherRepository;
        this.voucherUsageRepository = voucherUsageRepository;
    }

    // ================== F_31: search/filter/sort/paging cho admin ==================

    public Page<Voucher> search(String keyword, String discountTypeStr, String activeStr,
                                 int page, int size, Sort sort) {
        DiscountType discountType = null;
        if (discountTypeStr != null && !discountTypeStr.isBlank()) {
            try {
                discountType = DiscountType.valueOf(discountTypeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // filter không hợp lệ -> bỏ qua, coi như không lọc theo loại giảm giá
            }
        }

        Boolean active = null;
        if (activeStr != null && !activeStr.isBlank()) {
            active = Boolean.valueOf(activeStr.trim());
        }

        return voucherRepository.search(keyword, discountType, active, PageRequest.of(page, size, sort));
    }

    // ================== F_30: view/create/update voucher ==================

    public Voucher getById(Long id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher với id: " + id));
    }

    public long countUsed(Long id) {
        Voucher voucher = getById(id);
        return voucherUsageRepository.countByVoucher(voucher);
    }

    @Transactional
    public void create(Voucher voucher) {
        validateVoucherForm(voucher, null);
        voucher.setVoucherId(null);
        voucher.setCode(voucher.getCode().trim().toUpperCase());
        if (voucher.getIsActive() == null) {
            voucher.setIsActive(true);
        }
        if (voucher.getMaxUsagePerCustomer() == null) {
            voucher.setMaxUsagePerCustomer(1);
        }
        voucherRepository.save(voucher);
    }

    @Transactional
    public void update(Long id, Voucher voucher) {
        Voucher existing = getById(id);
        validateVoucherForm(voucher, id);

        existing.setCode(voucher.getCode().trim().toUpperCase());
        existing.setDescription(voucher.getDescription());
        existing.setDiscountType(voucher.getDiscountType());
        existing.setDiscountValue(voucher.getDiscountValue());
        existing.setMinOrderValue(voucher.getMinOrderValue());
        existing.setStartDate(voucher.getStartDate());
        existing.setEndDate(voucher.getEndDate());
        existing.setUsageLimit(voucher.getUsageLimit());
        existing.setMaxUsagePerCustomer(
                voucher.getMaxUsagePerCustomer() == null ? 1 : voucher.getMaxUsagePerCustomer());
        if (voucher.getIsActive() != null) {
            existing.setIsActive(voucher.getIsActive());
        }

        voucherRepository.save(existing);
    }

    @Transactional
    public void toggleActive(Long id) {
        Voucher voucher = getById(id);
        voucher.setIsActive(voucher.getIsActive() == null || !voucher.getIsActive());
        voucherRepository.save(voucher);
    }

    private void validateVoucherForm(Voucher voucher, Long currentId) {
        if (voucher.getCode() == null || voucher.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã voucher không được để trống.");
        }
        boolean duplicated = currentId == null
                ? voucherRepository.existsByCodeIgnoreCase(voucher.getCode().trim())
                : voucherRepository.existsByCodeIgnoreCaseAndVoucherIdNot(voucher.getCode().trim(), currentId);
        if (duplicated) {
            throw new IllegalArgumentException("Mã voucher \"" + voucher.getCode() + "\" đã tồn tại.");
        }

        if (voucher.getStartDate() == null || voucher.getEndDate() == null) {
            throw new IllegalArgumentException("Vui lòng nhập đầy đủ ngày bắt đầu và ngày kết thúc.");
        }
        if (!voucher.getEndDate().isAfter(voucher.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
        if (voucher.getDiscountValue() == null || voucher.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0.");
        }
        // BR-V07 (suy ra từ nghiệp vụ PERCENT): discount_value theo % phải nằm trong (0, 100].
        if (voucher.getDiscountType() == DiscountType.PERCENT
                && voucher.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Giá trị giảm giá theo phần trăm không được vượt quá 100%.");
        }
    }

    // ================== Checkout ==================

    // BR-V08: voucher phải được kiểm tra lại đầy đủ (BR-V02..BR-V06, BR-V10)
    // ngay tại thời điểm tạo đơn, không tin kết quả đã check ở bước preview giỏ hàng.
    @Transactional
    public Voucher validateForCheckout(String code, BigDecimal subtotal, User user) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }

        Voucher found = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không tồn tại."));

        // Lock voucher khi checkout để tránh 2 khách cùng lúc "lọt" qua kiểm tra
        // usage_limit (BR-V05) do đọc số đếm cũ trước khi đối phương ghi usage.
        Voucher voucher = voucherRepository.findByIdForUpdate(found.getVoucherId())
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không tồn tại."));

        // BR-V02: voucher phải đang active
        if (voucher.getIsActive() == null || !voucher.getIsActive()) {
            throw new IllegalArgumentException("Voucher này hiện không còn hoạt động.");
        }

        // BR-V03: phải trong khoảng thời gian hiệu lực
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new IllegalArgumentException("Voucher chưa đến thời gian áp dụng.");
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new IllegalArgumentException("Voucher đã hết hạn sử dụng.");
        }

        // BR-V04: đơn hàng phải đạt giá trị tối thiểu
        if (voucher.getMinOrderValue() != null
                && subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher này.");
        }

        // BR-V05: tổng số lượt dùng toàn hệ thống chưa vượt usage_limit
        if (voucher.getUsageLimit() != null) {
            long totalUsed = voucherUsageRepository.countByVoucher(voucher);
            if (totalUsed >= voucher.getUsageLimit()) {
                throw new IllegalArgumentException("Voucher đã hết lượt sử dụng.");
            }
        }

        // BR-V06: số lượt CHÍNH khách hàng này đã dùng voucher này chưa vượt giới hạn
        if (user != null) {
            int maxPerCustomer = voucher.getMaxUsagePerCustomer() == null
                    ? 1 : voucher.getMaxUsagePerCustomer();
            long usedByUser = voucherUsageRepository.countByVoucherAndUser(voucher, user);
            if (usedByUser >= maxPerCustomer) {
                throw new IllegalArgumentException("Bạn đã sử dụng hết số lượt cho phép của voucher này.");
            }
        }

        // BR-V10: số tiền giảm không được vượt quá tổng tiền hàng — kẹp giá trị
        // này ở calculateDiscount() thay vì chặn ở đây.
        return voucher;
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        if (voucher == null || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // FIXED_AMOUNT / COMBO_GIFT: dùng trực tiếp discount_value làm số tiền giảm
            // (COMBO_GIFT chưa có xử lý quà tặng riêng trong phạm vi hiện tại).
            discount = voucher.getDiscountValue();
        }

        
        // BR-V10: không giảm quá tổng tiền hàng.
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount;
    }

    // BR-V09: chỉ insert VoucherUsage sau khi đơn hàng được xác nhận thanh toán
    // thành công (COD: ngay khi tạo đơn; VNPAY: ở bước callback vnpay-return).
    @Transactional
    public void recordUsage(Voucher voucher, User user, Order order) {
        if (voucher == null || user == null || order == null) {
            return;
        }
        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(voucher);
        usage.setUser(user);
        usage.setOrder(order);
        usage.setUsedAt(LocalDateTime.now());
        voucherUsageRepository.save(usage);
    }

    // Hoàn lại lượt dùng khi đơn hàng bị hủy (COD ghi nhận usage ngay lúc tạo đơn,
    // nên nếu đơn đó sau đó bị hủy, phải xóa VoucherUsage tương ứng — nếu không,
    // khách và voucher sẽ bị trừ 1 lượt oan cho một đơn chưa từng hoàn tất).
    @Transactional
    public void releaseUsageForCancelledOrder(Order order) {
        if (order == null) {
            return;
        }
        voucherUsageRepository.deleteByOrder(order);
    }
}