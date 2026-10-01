// ============================================================
// FOODLOOP - MAIN APPLICATION JAVASCRIPT
// ============================================================

"use strict";


// ============================================================
// API HELPER
// ============================================================

async function api(url, options = {}) {

    options.credentials = "same-origin";

    const response = await fetch(url, options);

    const text = await response.text();

    let data = {};

    try {
        data = text ? JSON.parse(text) : {};
    } catch (error) {
        data = {
            ok: false,
            message: text || "Invalid server response."
        };
    }

    if (!response.ok) {
        throw new Error(
            data.message ||
            `Request failed (${response.status})`
        );
    }

    return data;
}


// ============================================================
// HTML ESCAPE
// ============================================================

function escapeHtml(value) {

    if (value === null || value === undefined) {
        return "";
    }

    return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


// ============================================================
// IMAGE URL
// ============================================================

function getImageUrl(image) {

    if (!image) {
        return "";
    }

    let value = String(image).trim();

    if (!value) {
        return "";
    }

    if (
        value.startsWith("http://") ||
        value.startsWith("https://") ||
        value.startsWith("data:image/")
    ) {
        return value;
    }

    value = value.replace(/\\/g, "/");

    if (value.includes("/uploads/")) {
        value =
            value.substring(
                value.lastIndexOf("/uploads/") + 9
            );
    }

    value = value.replace(/^\/+/, "");

    if (
        value.toLowerCase().startsWith("uploads/")
    ) {
        return "/" + value;
    }

    return "/uploads/" + value;
}


// ============================================================
// NOTIFICATION
// ============================================================

function notify(message, type = "success") {

    let container =
        document.getElementById(
            "foodloopNotifications"
        );

    if (!container) {

        container =
            document.createElement("div");

        container.id =
            "foodloopNotifications";

        container.className =
            "toast-container";

        document.body.appendChild(container);
    }

    const notification =
        document.createElement("div");

    notification.className =
        `toast ${type}`;

    notification.innerHTML = `
        <span>
            ${escapeHtml(message)}
        </span>
    `;

    container.appendChild(notification);

    setTimeout(() => {

        if (notification.parentElement) {
            notification.remove();
        }

    }, 4500);
}


// ============================================================
// CURRENT USER
// ============================================================

async function user() {

    try {

        const data =
            await api("/api/auth/me");

        if (
            data &&
            data.authenticated &&
            data.user
        ) {
            return data.user;
        }

        return null;

    } catch (error) {

        console.error(
            "FoodLoop user error:",
            error
        );

        return null;
    }
}


// ============================================================
// LOGOUT
// ============================================================

async function logout() {

    try {

        await api(
            "/api/auth/logout",
            {
                method: "POST"
            }
        );

    } catch (error) {

        console.error(
            "Logout error:",
            error
        );

    } finally {

        try {
            localStorage.removeItem(
                "foodloopUser"
            );

            sessionStorage.removeItem(
                "foodloopUser"
            );
        } catch (error) {
            console.warn(
                "Storage cleanup failed."
            );
        }

        window.location.href =
            "/index.html";
    }
}


// ============================================================
// NAVIGATION
// ============================================================

function setupNavigation(currentUser) {

    const nameElement =
        document.getElementById(
            "navUserName"
        );

    const roleElement =
        document.getElementById(
            "navUserRole"
        );

    if (nameElement && currentUser) {
        nameElement.textContent =
            currentUser.name || "User";
    }

    if (roleElement && currentUser) {
        roleElement.textContent =
            currentUser.role || "";
    }

    const logoutButton =
        document.getElementById(
            "logoutBtn"
        );

    if (logoutButton) {

        logoutButton.addEventListener(
            "click",
            async function (event) {

                event.preventDefault();

                await logout();

            }
        );
    }
}


// ============================================================
// STATUS CLASS
// ============================================================

function statusClass(status) {

    const value =
        String(status || "")
            .toUpperCase();

    if (
        value === "ACCEPTED" ||
        value === "APPROVED" ||
        value === "AVAILABLE" ||
        value === "COMPLETED" ||
        value === "DELIVERED" ||
        value === "PAID"
    ) {
        return "status-accepted";
    }

    if (
        value === "REJECTED" ||
        value === "CANCELLED"
    ) {
        return "status-rejected";
    }

    if (
        value === "ASSIGNED" ||
        value === "PICKED_UP" ||
        value === "ON_THE_WAY"
    ) {
        return "status-assigned";
    }

    return "status-pending";
}


// ============================================================
// STATUS LABEL
// ============================================================

function statusLabel(status) {

    const value =
        String(status || "PENDING")
            .toUpperCase();

    if (value === "APPROVED") {
        return "ACCEPTED";
    }

    return value.replace(
        /_/g,
        " "
    );
}


// ============================================================
// AVAILABLE FOOD
// ============================================================

async function loadAvailableFood() {

    const container =
        document.getElementById(
            "available"
        );

    if (!container) {
        return;
    }

    container.innerHTML =
        `<div class="loading">Loading available food...</div>`;

    try {

        const data =
            await api(
                "/api/food?mode=available"
            );

        const foods =
            Array.isArray(data)
                ? data
                : Array.isArray(data.food)
                    ? data.food
                    : [];

        renderAvailableFood(
            foods,
            container
        );

    } catch (error) {

        console.error(error);

        container.innerHTML = `
            <div class="empty-state">
                <h3>Unable to load food</h3>
                <p>
                    ${escapeHtml(
                        error.message ||
                        "Please try again."
                    )}
                </p>
            </div>
        `;
    }
}


// ============================================================
// RENDER AVAILABLE FOOD
// ============================================================

function renderAvailableFood(
    foods,
    container
) {

    if (!foods.length) {

        container.innerHTML = `
            <div class="empty-state">
                <h3>No food available</h3>
                <p>
                    There is currently no food available for claiming.
                </p>
            </div>
        `;

        return;
    }

    container.innerHTML = `
        <div class="food-grid">
            ${
                foods.map(food => {

                    const image =
                        getImageUrl(
                            food.image
                        );

                    return `
                        <article
                            class="food-card"
                            data-category="${escapeHtml(
                                food.category || ""
                            )}"
                        >

                            <div class="food-image">

                                ${
                                    image
                                        ? `
                                            <img
                                                src="${escapeHtml(image)}"
                                                alt="${escapeHtml(
                                                    food.foodName ||
                                                    "Food"
                                                )}"
                                                onerror="
                                                    this.parentElement.innerHTML =
                                                    '<div class=&quot;food-image no-image&quot;>🍱 No image</div>';
                                                "
                                            >
                                          `
                                        : `
                                            <div class="food-image no-image">
                                                🍱
                                            </div>
                                          `
                                }

                            </div>


                            <div class="food-content">

                                <div class="request-header">

                                    <div>

                                        <h3>
                                            ${escapeHtml(
                                                food.foodName
                                            )}
                                        </h3>

                                    </div>

                                    <span
                                        class="status ${statusClass(
                                            food.status
                                        )}"
                                    >
                                        ${escapeHtml(
                                            statusLabel(
                                                food.status ||
                                                "AVAILABLE"
                                            )
                                        )}
                                    </span>

                                </div>


                                <div class="food-meta">

                                    <div class="food-meta-item">

                                        <span class="food-meta-label">
                                            Quantity
                                        </span>

                                        <span class="food-meta-value">
                                            ${escapeHtml(
                                                food.quantity
                                            )}
                                        </span>

                                    </div>


                                    <div class="food-meta-item">

                                        <span class="food-meta-label">
                                            Category
                                        </span>

                                        <span class="food-meta-value">
                                            ${escapeHtml(
                                                food.category
                                            )}
                                        </span>

                                    </div>


                                    <div class="food-meta-item">

                                        <span class="food-meta-label">
                                            Location
                                        </span>

                                        <span class="food-meta-value">
                                            ${escapeHtml(
                                                food.location
                                            )}
                                        </span>

                                    </div>


                                    <div class="food-meta-item">

                                        <span class="food-meta-label">
                                            Expiry
                                        </span>

                                        <span class="food-meta-value">
                                            ${escapeHtml(
                                                food.expiryDate ||
                                                food.expiry_date ||
                                                "-"
                                            )}
                                        </span>

                                    </div>

                                </div>


                                ${
                                    food.description
                                        ? `
                                            <p
                                                class="mt-2"
                                                style="color:var(--muted);"
                                            >
                                                ${escapeHtml(
                                                    food.description
                                                )}
                                            </p>
                                          `
                                        : ""
                                }


                                <div class="food-actions">

                                    <button
                                        type="button"
                                        class="btn btn-blue claim-food-btn"
                                        data-food-id="${food.id}"
                                    >
                                        Claim Food
                                    </button>

                                </div>

                            </div>

                        </article>
                    `;

                }).join("")
            }
        </div>
    `;


    container
        .querySelectorAll(
            ".claim-food-btn"
        )
        .forEach(button => {

            button.addEventListener(
                "click",
                function () {

                    claimFood(
                        this.dataset.foodId
                    );

                }
            );

        });
}


// ============================================================
// FILTER AVAILABLE FOOD
// ============================================================

function filterAvailableFood() {

    const select =
        document.getElementById(
            "filter"
        );

    const container =
        document.getElementById(
            "available"
        );

    if (!select || !container) {
        return;
    }

    const selected =
        String(select.value || "")
            .toLowerCase();

    container
        .querySelectorAll(
            ".food-card"
        )
        .forEach(card => {

            const category =
                String(
                    card.dataset.category ||
                    ""
                ).toLowerCase();

            card.style.display =
                !selected ||
                category === selected
                    ? ""
                    : "none";

        });
}


// ============================================================
// CLAIM FOOD
// ============================================================

async function claimFood(foodId) {

    if (!foodId) {
        return;
    }

    try {

        const body =
            new URLSearchParams();

        body.append(
            "action",
            "claim"
        );

        body.append(
            "foodId",
            foodId
        );

        const data =
            await api(
                "/api/food",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/x-www-form-urlencoded"
                    },

                    body
                }
            );

        if (data.ok === false) {

            throw new Error(
                data.message ||
                "Unable to claim food."
            );
        }

        notify(
            data.message ||
            "Food claim submitted successfully.",
            "success"
        );

        await loadAvailableFood();

        if (
            typeof window.loadMyClaims ===
            "function"
        ) {

            await window.loadMyClaims();

        }

    } catch (error) {

        console.error(error);

        notify(
            error.message ||
            "Unable to claim food.",
            "error"
        );
    }
}


// ============================================================
// DONOR FOOD
// ============================================================

async function loadDonorFood() {

    const container =
        document.getElementById(
            "mine"
        );

    if (!container) {
        return;
    }

    container.innerHTML =
        `<div class="loading">Loading your donations...</div>`;

    try {

        const data =
            await api(
                "/api/food?mode=mine"
            );

        const foods =
            Array.isArray(data)
                ? data
                : [];

        if (!foods.length) {

            container.innerHTML = `
                <div class="empty-state">

                    <h3>
                        No donated food
                    </h3>

                    <p>
                        You have not donated any food yet.
                    </p>

                </div>
            `;

            return;
        }


        container.innerHTML = `

            <div class="food-grid">

                ${
                    foods.map(food => {

                        const image =
                            getImageUrl(
                                food.image
                            );

                        const status =
                            String(
                                food.status ||
                                "AVAILABLE"
                            ).toUpperCase();

                        return `

                            <article class="food-card">

                                <div class="food-image">

                                    ${
                                        image
                                            ? `
                                                <img
                                                    src="${escapeHtml(image)}"
                                                    alt="${escapeHtml(
                                                        food.foodName
                                                    )}"
                                                    onerror="
                                                        this.parentElement.innerHTML =
                                                        '<div class=&quot;food-image no-image&quot;>🍱 No image</div>';
                                                    "
                                                >
                                              `
                                            : `
                                                <div class="food-image no-image">
                                                    🍱
                                                </div>
                                              `
                                    }

                                </div>


                                <div class="food-content">

                                    <div class="request-header">

                                        <div>

                                            <h3>
                                                ${escapeHtml(
                                                    food.foodName
                                                )}
                                            </h3>

                                        </div>


                                        <span
                                            class="status ${statusClass(
                                                status
                                            )}"
                                        >
                                            ${escapeHtml(
                                                statusLabel(status)
                                            )}
                                        </span>

                                    </div>


                                    <div class="food-meta">

                                        <div class="food-meta-item">

                                            <span class="food-meta-label">
                                                Quantity
                                            </span>

                                            <span class="food-meta-value">
                                                ${escapeHtml(
                                                    food.quantity
                                                )}
                                            </span>

                                        </div>


                                        <div class="food-meta-item">

                                            <span class="food-meta-label">
                                                Category
                                            </span>

                                            <span class="food-meta-value">
                                                ${escapeHtml(
                                                    food.category
                                                )}
                                            </span>

                                        </div>


                                        <div class="food-meta-item">

                                            <span class="food-meta-label">
                                                Location
                                            </span>

                                            <span class="food-meta-value">
                                                ${escapeHtml(
                                                    food.location
                                                )}
                                            </span>

                                        </div>


                                        <div class="food-meta-item">

                                            <span class="food-meta-label">
                                                Claims
                                            </span>

                                            <span class="food-meta-value">
                                                ${escapeHtml(
                                                    food.claimCount ??
                                                    food.claims ??
                                                    0
                                                )}
                                            </span>

                                        </div>

                                    </div>


                                    ${
                                        status === "AVAILABLE"
                                            ? `

                                                <div class="food-actions">

                                                    <button
                                                        type="button"
                                                        class="btn btn-danger delete-food-btn"
                                                        data-food-id="${food.id}"
                                                    >
                                                        Delete
                                                    </button>

                                                </div>

                                              `
                                            : ""
                                    }

                                </div>

                            </article>

                        `;

                    }).join("")
                }

            </div>

        `;


        container
            .querySelectorAll(
                ".delete-food-btn"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    function () {

                        deleteFood(
                            this.dataset.foodId
                        );

                    }
                );

            });

    } catch (error) {

        console.error(error);

        container.innerHTML = `
            <div class="empty-state">

                <h3>
                    Unable to load donations
                </h3>

                <p>
                    ${escapeHtml(
                        error.message ||
                        "Please try again."
                    )}
                </p>

            </div>
        `;
    }
}


// ============================================================
// CLAIM REQUESTS
// ============================================================

async function loadClaimRequests() {

    const container =
        document.getElementById(
            "requests"
        );

    if (!container) {
        return;
    }

    container.innerHTML =
        `<div class="loading">Loading claim requests...</div>`;

    try {

        const data =
            await api(
                "/api/food?mode=claim-requests"
            );

        const claims =
            Array.isArray(data)
                ? data
                : [];

        if (!claims.length) {

            container.innerHTML = `
                <div class="empty-state">

                    <h3>
                        No claim requests
                    </h3>

                    <p>
                        New recipient requests will appear here.
                    </p>

                </div>
            `;

            return;
        }


        container.innerHTML = `

            <div>

                ${
                    claims.map(claim => {

                        const status =
                            String(
                                claim.status ||
                                "PENDING"
                            ).toUpperCase();

                        const accepted =
                            status === "ACCEPTED" ||
                            status === "APPROVED";

                        return `

                            <article class="request-card">

                                <div class="request-header">

                                    <div>

                                        <h3>
                                            ${escapeHtml(
                                                claim.foodName
                                            )}
                                        </h3>

                                        <p>
                                            Recipient:
                                            <strong>
                                                ${escapeHtml(
                                                    claim.recipientName
                                                )}
                                            </strong>
                                        </p>

                                    </div>


                                    <span
                                        class="status ${statusClass(
                                            status
                                        )}"
                                    >
                                        ${escapeHtml(
                                            statusLabel(status)
                                        )}
                                    </span>

                                </div>


                                <div class="delivery-details">

                                    <div class="delivery-detail">

                                        <span class="label">
                                            Email
                                        </span>

                                        <span class="value">
                                            ${escapeHtml(
                                                claim.recipientEmail
                                            )}
                                        </span>

                                    </div>


                                    <div class="delivery-detail">

                                        <span class="label">
                                            Location
                                        </span>

                                        <span class="value">
                                            ${escapeHtml(
                                                claim.location
                                            )}
                                        </span>

                                    </div>


                                    <div class="delivery-detail">

                                        <span class="label">
                                            Quantity
                                        </span>

                                        <span class="value">
                                            ${escapeHtml(
                                                claim.quantity
                                            )}
                                        </span>

                                    </div>


                                    <div class="delivery-detail">

                                        <span class="label">
                                            Category
                                        </span>

                                        <span class="value">
                                            ${escapeHtml(
                                                claim.category
                                            )}
                                        </span>

                                    </div>

                                </div>


                                <div class="request-actions">

                                    ${
                                        status === "PENDING"
                                            ? `

                                                <button
                                                    type="button"
                                                    class="btn btn-accept accept-btn"
                                                    data-claim-id="${claim.claimId}"
                                                >
                                                    Accept
                                                </button>


                                                <button
                                                    type="button"
                                                    class="btn btn-danger reject-btn"
                                                    data-claim-id="${claim.claimId}"
                                                >
                                                    Reject
                                                </button>

                                              `
                                            : ""
                                    }


                                    ${
                                        accepted
                                            ? `

                                                <button
                                                    type="button"
                                                    class="btn btn-blue assign-volunteer-btn"
                                                    data-claim-id="${claim.claimId}"
                                                >
                                                    Assign Volunteer
                                                </button>

                                              `
                                            : ""
                                    }

                                </div>

                            </article>

                        `;

                    }).join("")
                }

            </div>

        `;


        container
            .querySelectorAll(
                ".accept-btn"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    function () {

                        decideClaim(
                            this.dataset.claimId,
                            "ACCEPT"
                        );

                    }
                );

            });


        container
            .querySelectorAll(
                ".reject-btn"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    function () {

                        decideClaim(
                            this.dataset.claimId,
                            "REJECT"
                        );

                    }
                );

            });


        container
            .querySelectorAll(
                ".assign-volunteer-btn"
            )
            .forEach(button => {

                button.addEventListener(
                    "click",
                    function () {

                        if (
                            typeof window.openVolunteerAssignment ===
                            "function"
                        ) {

                            window.openVolunteerAssignment(
                                this.dataset.claimId
                            );

                        } else {

                            notify(
                                "Volunteer assignment is not available.",
                                "error"
                            );

                        }

                    }
                );

            });

    } catch (error) {

        console.error(error);

        container.innerHTML = `
            <div class="empty-state">

                <h3>
                    Unable to load requests
                </h3>

                <p>
                    ${escapeHtml(
                        error.message ||
                        "Please try again."
                    )}
                </p>

            </div>
        `;
    }
}


// ============================================================
// ACCEPT / REJECT CLAIM
// ============================================================

async function decideClaim(
    claimId,
    decision
) {

    try {

        const body =
            new URLSearchParams();

        body.append(
            "action",
            "claim-decision"
        );

        body.append(
            "claimId",
            claimId
        );

        body.append(
            "decision",
            decision
        );

        const data =
            await api(
                "/api/food",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/x-www-form-urlencoded"
                    },

                    body
                }
            );

        if (data.ok === false) {

            throw new Error(
                data.message ||
                "Unable to update claim."
            );
        }


        if (
            decision.toUpperCase() ===
            "ACCEPT"
        ) {

            notify(
                "Claim accepted. Choose an available volunteer.",
                "success"
            );

        } else {

            notify(
                data.message ||
                "Claim rejected.",
                "success"
            );
        }


        await loadClaimRequests();


        if (
            typeof window.loadDonorFood ===
            "function"
        ) {

            await window.loadDonorFood();

        }


        if (
            decision.toUpperCase() ===
            "ACCEPT"
        ) {

            setTimeout(() => {

                if (
                    typeof window.openVolunteerAssignment ===
                    "function"
                ) {

                    window.openVolunteerAssignment(
                        claimId
                    );

                }

            }, 250);

        }

    } catch (error) {

        console.error(error);

        notify(
            error.message ||
            "Unable to update claim.",
            "error"
        );
    }
}


// ============================================================
// DELETE FOOD
// ============================================================

async function deleteFood(foodId) {

    if (!foodId) {
        return;
    }

    try {

        const body =
            new URLSearchParams();

        body.append(
            "action",
            "delete"
        );

        body.append(
            "foodId",
            foodId
        );

        const data =
            await api(
                "/api/food",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/x-www-form-urlencoded"
                    },

                    body
                }
            );

        if (data.ok === false) {

            throw new Error(
                data.message ||
                "Unable to delete food."
            );
        }


        notify(
            data.message ||
            "Food deleted successfully.",
            "success"
        );


        await loadDonorFood();


        if (
            typeof window.loadDashboardStats ===
            "function"
        ) {

            await window.loadDashboardStats();

        }

    } catch (error) {

        console.error(error);

        notify(
            error.message ||
            "Unable to delete food.",
            "error"
        );
    }
}


// ============================================================
// RECIPIENT CLAIMS
// ============================================================

async function loadMyClaims() {

    const container =
        document.getElementById(
            "myClaims"
        );

    if (!container) {
        return;
    }

    container.innerHTML =
        `<div class="loading">Loading your claims...</div>`;

    try {

        const data =
            await api(
                "/api/food?mode=my-claims"
            );

        const claims =
            Array.isArray(data)
                ? data
                : [];

        if (!claims.length) {

            container.innerHTML = `
                <div class="empty-state">

                    <h3>
                        No claims yet
                    </h3>

                    <p>
                        Food you claim will appear here.
                    </p>

                </div>
            `;

            return;
        }


        container.innerHTML = `

            <div>

                ${
                    claims.map(claim => {

                        const status =
                            String(
                                claim.status ||
                                "PENDING"
                            ).toUpperCase();

                        return `

                            <article class="claim-card">

                                <div class="claim-header">

                                    <div>

                                        <h3>
                                            ${escapeHtml(
                                                claim.foodName
                                            )}
                                        </h3>

                                    </div>


                                    <span
                                        class="status ${statusClass(
                                            status
                                        )}"
                                    >
                                        ${escapeHtml(
                                            statusLabel(status)
                                        )}
                                    </span>

                                </div>


                                <p>
                                    Quantity:
                                    <strong>
                                        ${escapeHtml(
                                            claim.quantity
                                        )}
                                    </strong>
                                </p>


                                <p>
                                    Donor:
                                    <strong>
                                        ${escapeHtml(
                                            claim.donorName
                                        )}
                                    </strong>
                                </p>


                                <p class="location">
                                    ${escapeHtml(
                                        claim.location
                                    )}
                                </p>

                            </article>

                        `;

                    }).join("")
                }

            </div>

        `;

    } catch (error) {

        console.error(error);

        container.innerHTML = `
            <div class="empty-state">

                <h3>
                    Unable to load claims
                </h3>

                <p>
                    ${escapeHtml(
                        error.message ||
                        "Please try again."
                    )}
                </p>

            </div>
        `;
    }
}


// ============================================================
// DASHBOARD STATS
// ============================================================

async function loadDashboardStats() {

    try {

        const data =
            await api(
                "/api/food?mode=stats"
            );


        const food =
            document.getElementById(
                "s-food"
            );

        const claims =
            document.getElementById(
                "s-claims"
            );

        const deliveries =
            document.getElementById(
                "s-deliveries"
            );


        if (food) {

            food.textContent =
                data.foodCount ?? 0;

        }


        if (claims) {

            claims.textContent =
                data.claimCount ?? 0;

        }


        if (deliveries) {

            deliveries.textContent =
                data.deliveryCount ?? 0;

        }

    } catch (error) {

        console.error(
            "Stats error:",
            error
        );
    }
}


// ============================================================
// VOLUNTEERS
// ============================================================

async function loadVolunteers() {

    try {

        const data =
            await api(
                "/api/food?mode=volunteers"
            );

        return Array.isArray(data)
            ? data
            : [];

    } catch (error) {

        console.error(
            "Volunteer loading error:",
            error
        );

        return [];
    }
}


// ============================================================
// DONATION FORM
// ============================================================

function setupDonationForm() {

    const form =
        document.getElementById(
            "donationForm"
        );

    if (!form) {
        return;
    }


    form.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();


            const formData =
                new FormData(form);


            formData.set(
                "action",
                "donate"
            );


            if (
                !formData.get("expiryDate") &&
                formData.get("expiryAt")
            ) {

                formData.set(
                    "expiryDate",
                    formData.get("expiryAt")
                );

            }


            const image =
                formData.get("foodImage");


            if (
                image instanceof File &&
                image.size > 0
            ) {

                formData.set(
                    "image",
                    image
                );

            }


            try {

                const data =
                    await api(
                        "/api/food",
                        {
                            method: "POST",
                            body: formData
                        }
                    );


                if (data.ok === false) {

                    throw new Error(
                        data.message ||
                        "Donation failed."
                    );

                }


                notify(
                    data.message ||
                    "Food donated successfully.",
                    "success"
                );


                form.reset();

            } catch (error) {

                console.error(error);

                notify(
                    error.message ||
                    "Unable to donate food.",
                    "error"
                );
            }

        }
    );
}


// ============================================================
// LOGIN FORM
// ============================================================

function setupLoginForm() {

    const form =
        document.getElementById(
            "loginForm"
        );

    if (!form) {
        return;
    }


    form.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();


            const formData =
                new FormData(form);


            const body =
                new URLSearchParams();


            body.append(
                "email",
                formData.get("email") || ""
            );


            body.append(
                "password",
                formData.get("password") || ""
            );


            body.append(
                "role",
                formData.get("role") || ""
            );


            if (
                formData.get("remember")
            ) {

                body.append(
                    "remember",
                    "true"
                );

            }


            try {

                const data =
                    await api(
                        "/api/auth/login",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/x-www-form-urlencoded"
                            },

                            body
                        }
                    );


                if (data.ok === false) {

                    throw new Error(
                        data.message ||
                        "Login failed."
                    );

                }


                window.location.href =
                    "/dashboard.html";

            } catch (error) {

                notify(
                    error.message ||
                    "Login failed.",
                    "error"
                );
            }

        }
    );
}


// ============================================================
// REGISTER FORM
// ============================================================

function setupRegisterForm() {

    const form =
        document.getElementById(
            "registerForm"
        );

    if (!form) {
        return;
    }


    form.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();


            const formData =
                new FormData(form);


            const body =
                new URLSearchParams();


            for (
                const [key, value]
                of formData.entries()
            ) {

                body.append(
                    key,
                    value
                );

            }


            try {

                const data =
                    await api(
                        "/api/auth/register",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/x-www-form-urlencoded"
                            },

                            body
                        }
                    );


                if (data.ok === false) {

                    throw new Error(
                        data.message ||
                        "Registration failed."
                    );

                }


                notify(
                    data.message ||
                    "Account created successfully.",
                    "success"
                );


                setTimeout(() => {

                    window.location.href =
                        "/index.html";

                }, 1200);


            } catch (error) {

                notify(
                    error.message ||
                    "Registration failed.",
                    "error"
                );
            }

        }
    );
}


// ============================================================
// GLOBAL EXPORTS
// ============================================================

window.api =
    api;

window.escapeHtml =
    escapeHtml;

window.getImageUrl =
    getImageUrl;

window.notify =
    notify;

window.user =
    user;

window.logout =
    logout;

window.loadAvailableFood =
    loadAvailableFood;

window.renderAvailableFood =
    renderAvailableFood;

window.filterAvailableFood =
    filterAvailableFood;

window.claimFood =
    claimFood;

window.loadDonorFood =
    loadDonorFood;

window.loadClaimRequests =
    loadClaimRequests;

window.decideClaim =
    decideClaim;

window.deleteFood =
    deleteFood;

window.loadMyClaims =
    loadMyClaims;

window.loadDashboardStats =
    loadDashboardStats;

window.loadVolunteers =
    loadVolunteers;

window.setupDonationForm =
    setupDonationForm;

window.setupLoginForm =
    setupLoginForm;

window.setupRegisterForm =
    setupRegisterForm;

window.setupNavigation =
    setupNavigation;


// ============================================================
// AUTO INITIALIZATION
// ============================================================

document.addEventListener(
    "DOMContentLoaded",
    function () {

        setupDonationForm();

        setupLoginForm();

        setupRegisterForm();

    }
);