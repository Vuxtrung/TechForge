document.addEventListener('DOMContentLoaded', function () {
    const avatarInput = document.getElementById('avatarInput');
    const avatarError = document.getElementById('avatarError');
    const MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    avatarInput.addEventListener('change', function () {
        console.log("Đã chọn file!");

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
            avatarError.textContent = 'Kích thước ảnh vượt quá 5MB.';
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
});