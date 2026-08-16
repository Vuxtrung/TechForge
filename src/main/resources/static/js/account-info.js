(function () {
    const form = document.getElementById('accountInfoForm');
    if (!form) return;

    const avatarInput = document.getElementById('avatarInput');
    const avatarError = document.getElementById('avatarError');
    const MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB

    // ---- Preview avatar khi chọn ảnh ----
    avatarInput.addEventListener('change', function () {
        avatarError.classList.add('d-none');
        const file = this.files[0];
        if (!file) return;

        const allowedTypes = ['image/png', 'image/jpeg', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
            avatarError.textContent = 'Chỉ chấp nhận file JPG, PNG hoặc WEBP.';
            avatarError.classList.remove('d-none');
            this.value = '';
            return;
        }

        if (file.size > MAX_AVATAR_SIZE) {
            avatarError.textContent = 'Kích thước ảnh vượt quá 2MB.';
            avatarError.classList.remove('d-none');
            this.value = '';
            return;
        }

        const reader = new FileReader();
        reader.onload = function (e) {
            let img = document.getElementById('avatarPreview');
            const placeholder = document.getElementById('avatarPlaceholder');

            if (!img) {
                img = document.createElement('img');
                img.id = 'avatarPreview';
                img.className = 'rounded-circle border object-fit-cover w-100 h-100';
                img.alt = 'Avatar';
                placeholder.replaceWith(img);
            }
            img.src = e.target.result;
        };
        reader.readAsDataURL(file);
    });

    // ---- Validate trước khi submit ----
    const phoneRegex = /^[0-9]{10}$/;

    form.addEventListener('submit', function (e) {
        let valid = true;

        const fullName = document.getElementById('fullName');
        fullName.classList.remove('is-invalid');
        if (!fullName.value.trim()) {
            fullName.classList.add('is-invalid');
            valid = false;
        }

        const phone = document.getElementById('phone');
        phone.classList.remove('is-invalid');
        if (phone.value.trim() && !phoneRegex.test(phone.value.trim())) {
            phone.classList.add('is-invalid');
            valid = false;
        }

        if (!valid) {
            e.preventDefault();
        }
    });
})();