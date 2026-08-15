package com.swp391.techforge.dto.order;

public class CheckoutRequest {
    private String recipientName;
    private String phone;
    private String email;
    private String province;
    private String district;
    private String ward;
    private String addressLine;
    private String orderNote;
    private String paymentMethod; // COD or VNPAY
    private String shippingMethod; // STANDARD (30.000đ) or EXPRESS (50.000đ)
    private String voucherCode;

    public CheckoutRequest() {
    }

    public String getFullShippingAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null && !addressLine.trim().isEmpty()) sb.append(addressLine.trim()).append(", ");
        if (ward != null && !ward.trim().isEmpty()) sb.append(ward.trim()).append(", ");
        if (district != null && !district.trim().isEmpty()) sb.append(district.trim()).append(", ");
        if (province != null && !province.trim().isEmpty()) sb.append(province.trim());
        return sb.toString();
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getOrderNote() {
        return orderNote;
    }

    public void setOrderNote(String orderNote) {
        this.orderNote = orderNote;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
}
