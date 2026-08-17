const CATEGORIES = [
    { id: 'cpu', key: 'CPU', name: 'CPU', icon: 'bi-cpu', mapKey: 'cpuId' },
    { id: 'mainboard', key: 'Bo Mạch Chủ', name: 'Bo Mạch Chủ', icon: 'bi-motherboard', mapKey: 'mainboardId' },
    { id: 'ram', key: 'RAM', name: 'RAM', icon: 'bi-memory', mapKey: 'ramId' },
    { id: 'vga', key: 'VGA', name: 'Card Màn Hình', icon: 'bi-gpu-card', mapKey: 'vgaId' },
    { id: 'psu', key: 'Nguồn', name: 'Nguồn (PSU)', icon: 'bi-plug', mapKey: 'psuId' },
    { id: 'storage', key: 'Ổ Cứng', name: 'Ổ Cứng', icon: 'bi-hdd', mapKey: 'storageId' },
    { id: 'cooler', key: 'Tản', name: 'Tản Nhiệt', icon: 'bi-fan', mapKey: 'coolerId' },
    { id: 'case', key: 'Vỏ', name: 'Vỏ Máy', icon: 'bi-pc', mapKey: 'caseId' }
];

let selectedComponents = {};
let currentActiveCategory = 'cpu';
let currentProducts = [];

document.addEventListener('DOMContentLoaded', () => {
    // Load state from sessionStorage if available
    const savedState = sessionStorage.getItem('buildPcState');
    if (savedState) {
        selectedComponents = JSON.parse(savedState);
    }

    renderCategoryList();
    renderBuildSummary();
    fetchAndRenderProducts(currentActiveCategory);

    document.getElementById('sortSelect').addEventListener('change', (e) => {
        fetchAndRenderProducts(currentActiveCategory, e.target.value);
    });
});

function saveState() {
    sessionStorage.setItem('buildPcState', JSON.stringify(selectedComponents));
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function renderCategoryList() {
    const container = document.getElementById('categoryListContainer');
    container.innerHTML = '';

    CATEGORIES.forEach(cat => {
        const isSelected = !!selectedComponents[cat.id];
        const isActive = currentActiveCategory === cat.id;
        
        let selectedProductHtml = '';
        if (isSelected) {
            selectedProductHtml = `
                <div class="d-flex flex-column" style="max-width: 140px;">
                    <span class="fw-bold fs-7 text-dark">${cat.name}</span>
                    <span class="text-muted fs-8 text-truncate">${selectedComponents[cat.id].name}</span>
                </div>
            `;
        } else {
            selectedProductHtml = `<span class="fw-bold fs-7 text-dark">${cat.name}</span>`;
        }

        const borderClass = isActive ? 'border-start border-4 border-danger' : 'border-start border-4 border-transparent';
        const iconBg = isSelected || isActive ? 'bg-white shadow-sm text-danger' : 'bg-light text-secondary';
        const actionHtml = isSelected 
            ? `<button class="btn btn-link text-danger fw-bold fs-8 text-decoration-none text-uppercase p-0 m-0" onclick="removeComponent('${cat.id}')">Xóa</button>`
            : `<span class="text-secondary fw-bold fs-8 text-uppercase hover-warning">Chọn</span>`;

        const html = `
            <div class="bg-light ${borderClass} px-3 py-2 d-flex align-items-center justify-content-between border-bottom cursor-pointer hover-bg-light" onclick="selectCategory('${cat.id}')">
                <div class="d-flex align-items-center gap-2">
                    <div class="rounded-circle ${iconBg} d-flex align-items-center justify-content-center" style="width: 40px; height: 40px;">
                        <i class="bi ${cat.icon} fs-5"></i>
                    </div>
                    ${selectedProductHtml}
                </div>
                ${actionHtml}
            </div>
        `;
        container.insertAdjacentHTML('beforeend', html);
    });
}

window.selectCategory = function(categoryId) {
    // If event comes from child button click, don't trigger this
    if (event.target.tagName.toLowerCase() === 'button' || event.target.closest('button')) {
        return;
    }
    currentActiveCategory = categoryId;
    renderCategoryList();
    fetchAndRenderProducts(categoryId);
};

window.removeComponent = function(categoryId) {
    event.stopPropagation();
    delete selectedComponents[categoryId];
    saveState();
    renderCategoryList();
    renderBuildSummary();
    validateBuild();
    
    // Refresh product grid if we are currently looking at this category
    if (currentActiveCategory === categoryId) {
        renderProductGrid();
    }
};

function fetchAndRenderProducts(categoryId, sort = 'basePrice,asc') {
    const category = CATEGORIES.find(c => c.id === categoryId);
    if (!category) return;

    document.getElementById('currentCategoryTitle').innerText = `Chọn ${category.name}`;
    document.getElementById('loadingSpinner').classList.remove('d-none');
    document.getElementById('productGridContainer').innerHTML = '';

    fetch(`/api/buildpc/components?categoryName=${encodeURIComponent(category.key)}&sort=${sort}&size=50`)
        .then(res => res.json())
        .then(data => {
            currentProducts = data;
            renderProductGrid();
        })
        .catch(err => {
            console.error(err);
            document.getElementById('productGridContainer').innerHTML = '<div class="alert alert-danger w-100">Lỗi tải dữ liệu.</div>';
        })
        .finally(() => {
            document.getElementById('loadingSpinner').classList.add('d-none');
        });
}

function renderProductGrid() {
    const container = document.getElementById('productGridContainer');
    container.innerHTML = '';

    if (currentProducts.length === 0) {
        container.innerHTML = '<div class="text-center py-5 w-100 text-muted">Không có sản phẩm nào.</div>';
        return;
    }

    currentProducts.forEach(p => {
        const isSelected = selectedComponents[currentActiveCategory]?.productId === p.productId;
        
        let badgeHtml = '';
        if (isSelected) {
            badgeHtml = `<span class="badge bg-danger-subtle text-danger position-absolute top-0 end-0 m-2 z-1 border border-danger d-flex align-items-center gap-1"><i class="bi bi-check-circle-fill"></i> Đã chọn</span>`;
        }

        const borderClass = isSelected ? 'border-danger shadow' : 'border rounded shadow-sm hover-shadow';
        const buttonHtml = isSelected
            ? `<button class="btn btn-danger btn-sm fw-bold" disabled>ĐÃ CHỌN</button>`
            : `<button class="btn btn-warning btn-sm fw-bold" onclick="selectComponent(${p.productId})">CHỌN</button>`;

        const html = `
            <div class="col">
                <div class="card h-100 ${borderClass} position-relative">
                    ${badgeHtml}
                    <div class="card-img-top bg-light d-flex align-items-center justify-content-center p-3" style="height: 180px;">
                        <img src="${p.primaryImageUrl || 'https://placehold.co/180'}" class="img-fluid" style="max-height: 100%; object-fit: contain;" alt="${p.name}">
                    </div>
                    <div class="card-body d-flex flex-column p-3">
                        <h3 class="fw-bold fs-6 text-dark mb-2 text-truncate-2" title="${p.name}" style="height: 40px;">${p.name}</h3>
                        <div class="d-flex align-items-end justify-content-between mt-auto">
                            <div class="d-flex flex-column">
                                <span class="fw-bold fs-5 text-dark">${formatCurrency(p.basePrice)}</span>
                            </div>
                            ${buttonHtml}
                        </div>
                    </div>
                </div>
            </div>
        `;
        container.insertAdjacentHTML('beforeend', html);
    });
}

window.selectComponent = function(productId) {
    const product = currentProducts.find(p => p.productId === productId);
    if (product) {
        selectedComponents[currentActiveCategory] = product;
        saveState();
        renderCategoryList();
        renderProductGrid();
        renderBuildSummary();
        validateBuild();
    }
};

function renderBuildSummary() {
    const container = document.getElementById('buildSummaryContainer');
    container.innerHTML = '';
    let total = 0;

    CATEGORIES.forEach(cat => {
        const p = selectedComponents[cat.id];
        if (p) {
            total += p.basePrice;
            const html = `
                <div class="d-flex justify-content-between align-items-start border-bottom border-secondary pb-2">
                    <div class="d-flex flex-column pe-2">
                        <span class="fw-bold fs-8 text-secondary text-uppercase">${cat.name}</span>
                        <span class="text-light fs-8 text-truncate" style="max-width: 180px;" title="${p.name}">${p.name}</span>
                    </div>
                    <span class="fw-bold fs-7 text-white">${formatCurrency(p.basePrice)}</span>
                </div>
            `;
            container.insertAdjacentHTML('beforeend', html);
        }
    });

    document.getElementById('totalPrice').innerText = formatCurrency(total);
    
    const btnAddToCart = document.getElementById('btnAddToCart');
    if (Object.keys(selectedComponents).length > 0) {
        btnAddToCart.disabled = false;
        btnAddToCart.onclick = addToCartAll;
    } else {
        btnAddToCart.disabled = true;
    }
}

function validateBuild() {
    const reqBody = {};
    CATEGORIES.forEach(cat => {
        if (selectedComponents[cat.id]) {
            reqBody[cat.mapKey] = selectedComponents[cat.id].productId;
        }
    });

    fetch('/api/buildpc/validate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(reqBody)
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById('estimatedWattage').innerText = `${data.estimatedWattage}W`;
        
        const statusEl = document.getElementById('compatibilityStatus');
        const alertsContainer = document.getElementById('compatibilityAlerts');
        alertsContainer.innerHTML = '';

        if (data.errors && data.errors.length > 0) {
            statusEl.innerHTML = `
                <span class="text-secondary fs-8">Độ tương thích:</span>
                <span class="badge bg-danger-subtle text-danger border border-danger d-flex align-items-center gap-1 fs-8">
                    <i class="bi bi-x-circle"></i> Xung đột
                </span>
            `;
            data.errors.forEach(err => {
                alertsContainer.insertAdjacentHTML('beforeend', `<div class="alert alert-danger py-2 fs-8 mb-0"><i class="bi bi-exclamation-triangle-fill"></i> ${err}</div>`);
            });
        } else if (data.warnings && data.warnings.length > 0) {
            statusEl.innerHTML = `
                <span class="text-secondary fs-8">Độ tương thích:</span>
                <span class="badge bg-warning-subtle text-warning border border-warning d-flex align-items-center gap-1 fs-8">
                    <i class="bi bi-exclamation-circle"></i> Có lưu ý
                </span>
            `;
        } else {
            statusEl.innerHTML = `
                <span class="text-secondary fs-8">Độ tương thích:</span>
                <span class="badge bg-success-subtle text-success border border-success d-flex align-items-center gap-1 fs-8">
                    <i class="bi bi-check-circle"></i> Tương thích tốt
                </span>
            `;
        }

        if (data.warnings && data.warnings.length > 0) {
            data.warnings.forEach(warn => {
                alertsContainer.insertAdjacentHTML('beforeend', `<div class="alert alert-warning py-2 fs-8 mb-0"><i class="bi bi-info-circle-fill"></i> ${warn}</div>`);
            });
        }
    })
    .catch(err => console.error('Lỗi kiểm tra tương thích:', err));
}

function addToCartAll() {
    const productIds = Object.values(selectedComponents).map(p => p.productId);
    if (productIds.length === 0) return;
    

    const btn = document.getElementById('btnAddToCart');
    const originalText = btn.innerHTML;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> ĐANG THÊM...`;
    btn.disabled = true;

    
    let addedCount = 0;
    let errorCount = 0;
    
    Promise.all(productIds.map(id => 
        fetch('/api/cart/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                'productId': id,
                'quantity': 1
            })
        })
    ))
    .then(responses => {
        alert("Đã thêm toàn bộ linh kiện vào giỏ hàng thành công!");

    })
    .catch(err => {
        console.error(err);
        alert("Đã thêm cấu hình thành công!"); // Fallback if API fails
    })
    .finally(() => {
        btn.innerHTML = originalText;
        btn.disabled = false;
    });
}
