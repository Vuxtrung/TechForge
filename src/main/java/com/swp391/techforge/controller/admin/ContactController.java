package com.swp391.techforge.controller.admin;

import com.swp391.techforge.dto.contact.ContactRequest;
import com.swp391.techforge.entity.Contact;
import com.swp391.techforge.entity.ContactStatus;
import com.swp391.techforge.repository.contact.ContactRepository;
import com.swp391.techforge.service.contact.CaptchaService;
import com.swp391.techforge.service.contact.ContactRateLimiterService;
import com.swp391.techforge.service.email.EmailService;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Set;

@Controller
public class ContactController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "fullName", "email", "subject",
            "status");
    private final ContactRepository contactRepository;
    private final ContactRateLimiterService rateLimiterService;
    private final CaptchaService captchaService;
    private final EmailService emailService;

    public ContactController(ContactRepository contactRepository,
            JavaMailSender mailSender,
            ContactRateLimiterService rateLimiterService,
            CaptchaService captchaService,
            EmailService emailService) {
        this.contactRepository = contactRepository;
        this.rateLimiterService = rateLimiterService;
        this.captchaService = captchaService;
        this.emailService = emailService;
    }

    @GetMapping("/contact")
    public String showContactPage(Model model) {
        model.addAttribute("contactRequest", new ContactRequest());
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@Valid @ModelAttribute("contactRequest") ContactRequest request,
            BindingResult bindingResult, HttpServletRequest httpRequest,
            @RequestParam(value = "g-recaptcha-response", required = false) String recaptchaResponse,
            Model model) {

        if (!captchaService.verifyCaptcha(recaptchaResponse)) {
            bindingResult.reject("recaptcha", "Vui lòng xác nhận lại bạn không phải là người máy.");
        }

        if (bindingResult.hasErrors()) {
            return "contact";
        }

        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = httpRequest.getRemoteAddr();
        }

        if (!rateLimiterService.isAllowed(clientIp)) {
            bindingResult.reject("rateLimit", "Bạn đang gửi liên hệ quá nhanh. Vui lòng thử lại sau 1 phút.");
        }

        Contact contact = new Contact();
        contact.setFullName(request.getFullName());
        contact.setEmail(request.getEmail());
        contact.setSubject(request.getSubject());
        contact.setMessage(request.getMessage());

        contactRepository.save(contact);

        model.addAttribute("successMessage", "Gửi liên hệ thành công! Chúng tôi đã nhận được thông tin.");
        model.addAttribute("contactRequest", new ContactRequest());

        return "contact";
    }

    @GetMapping("/admin/contacts")
    public String listContacts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, safeSortBy));

        String safeKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        ContactStatus statusEnum = null;
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusEnum = ContactStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // status không hợp lệ -> coi như ALL
            }
        }

        Page<Contact> contactPage = contactRepository.search(safeKeyword, statusEnum, pageable);

        model.addAttribute("contactPage", contactPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", safeSortBy);
        model.addAttribute("direction", dir.name().toLowerCase());
        model.addAttribute("statusOptions", ContactStatus.values());
        return "admin/contact-list";
    }

    @GetMapping("/admin/contacts/{id}")
    public String viewContactDetail(@PathVariable long id, Model model) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin liên hệ với ID: " + id));
        model.addAttribute("contact", contact);
        return "admin/contact-form";
    }

    @PostMapping("/admin/contacts/{id}/toggle-hidden")
    public String toggleHidden(@PathVariable long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            RedirectAttributes redirectAttributes) {

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy liên hệ với ID: " + id));

        if (contact.getStatus() == ContactStatus.HIDDEN) {
            contact.setStatus(contact.getRepliedAt() != null ? ContactStatus.REPLIED : ContactStatus.PENDING);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hiện lại liên hệ.");
        } else {
            contact.setStatus(ContactStatus.HIDDEN);
            redirectAttributes.addFlashAttribute("successMessage", "Đã ẩn liên hệ.");
        }
        contactRepository.save(contact);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/contacts")
                .queryParam("status", status)
                .queryParam("sortBy", sortBy)
                .queryParam("direction", direction)
                .queryParam("page", page)
                .queryParam("size", size);

        if (StringUtils.hasText(keyword)) {
            builder.queryParam("keyword", keyword);
        }

        return "redirect:" + builder.build().toUriString();
    }

    @PostMapping("/admin/contacts/{id}/reply")
    public String replyContact(@PathVariable long id,
            @RequestParam("replyMessage") String replyMessage,
            Model model) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin liên hệ với ID: " + id));

        try {
            emailService.sendContactResponse(contact.getEmail(), contact.getSubject(), contact.getFullName(), replyMessage);
            contact.setRepliedAt(LocalDateTime.now());
            if (contact.getStatus() != ContactStatus.HIDDEN) {
                contact.setStatus(ContactStatus.REPLIED);
            }
            contactRepository.save(contact);

            model.addAttribute("successMessage", "Đã gửi phản hồi thành công đến " + contact.getEmail());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Gửi email thất bại: " + e.getMessage());
        }

        model.addAttribute("contact", contact);
        return "admin/contact-form";
    }
}
