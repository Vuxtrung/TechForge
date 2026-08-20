package com.swp391.techforge.controller.admin;

import com.swp391.techforge.dto.contact.ContactRequest;
import com.swp391.techforge.entity.Contact;
import com.swp391.techforge.repository.contact.ContactRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ContactController {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/contact")
    public String showContactPage(Model model) {
        // model.addAttribute("contactRequest", new ContactRequest());
        return "redirect:/";
    }

    @PostMapping("/contact")
    public String submitContact(@Valid @ModelAttribute("contactRequest") ContactRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "contact";
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Contact> contactPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            contactPage = contactRepository
                    .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrSubjectContainingIgnoreCase(
                            keyword.trim(), keyword.trim(), keyword.trim(), pageable);
            model.addAttribute("keyword", keyword);
        } else {
            contactPage = contactRepository.findAll(pageable);
        }

        model.addAttribute("contactPage", contactPage);
        return "admin/contact-list";
    }

    @GetMapping("/admin/contacts/{id}")
    public String viewContactDetail(@PathVariable Long id, Model model) {
        if (id == null) {
            return "admin/contacts";
        }
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin liên hệ với ID: " + id));
        model.addAttribute("contact", contact);
        return "admin/contact-form";
    }

    @PostMapping("/admin/contacts/{id}/delete")
    public String deleteContact(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (id == null) {
            return "admin/contacts";
        }
        if (!contactRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy liên hệ để xoá.");
            return "redirect:/admin/contacts";
        }

        contactRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xoá liên hệ thành công");
        return "redirect:/admin/contacts";
    }

    @PostMapping("/admin/contacts/{id}/reply")
    public String replyContact(@PathVariable Long id,
            @RequestParam("replyMessage") String replyMessage,
            Model model) {
        if (id == null) {
            return "admin/contacts";
        }
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin liên hệ với ID: " + id));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(contact.getEmail());
            message.setSubject("Phản hồi từ TechForge về yêu cầu: " + contact.getSubject());
            message.setText("Xin chào " + contact.getFullName() + ",\n\n" +
                    "Cảm ơn bạn đã liên hệ với TechForge. Phản hồi từ nhân viên:\n\n" +
                    replyMessage + "\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ hỗ trợ TechForge");

            mailSender.send(message);

            model.addAttribute("successMessage", "Đã gửi phản hồi thành công đến " + contact.getEmail());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Gửi email thất bại: " + e.getMessage());
        }

        model.addAttribute("contact", contact);
        return "admin/contact-form";
    }
}