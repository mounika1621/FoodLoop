// ============================================================
// FOODLOOP DASHBOARD
// ============================================================

"use strict";


// ============================================================
// BASIC HELPERS
// ============================================================

const dashboardApi =
    window.api ||
    async function (url, options = {}) {

        options.credentials = "same-origin";

        const response =
            await fetch(url, options);

        const text =
            await response.text();

        let data = {};

        try {
            data =
                text
                    ? JSON.parse(text)
                    : {};

        } catch (e) {

            data = {
                ok: false,
                message: text
            };
        }

        if (!response.ok) {

            throw new Error(
                data.message ||
                `Request failed (${response.status})`
            );
        }

        return data;
    };


// ============================================================
// HTML ESCAPE
// ============================================================

function dashEscape(value) {

    if (
        value === null ||
        value === undefined
    ) {
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
// NOTIFICATION
// ============================================================

function dashNotify(
    message,
    type = "success"
) {

    if (
        typeof window.notify ===
        "function"
    ) {

        window.notify(
            message,
            type
        );

        return;
    }

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
            "foodloop-notifications";

        document.body.appendChild(
            container
        );
    }

    const item =
        document.createElement("div");

    item.className =
        `foodloop-notification ${type}`;

    item.innerHTML = `
        <div class="notification-icon">
            ${type === "error" ? "!" : "✓"}
        </div>

        <div class="notification-message">
            ${dashEscape(message)}
        </div>

        <button
            type="button"
            class="notification-close"
        >
            ×
        </button>
    `;

    container.appendChild(item);

    item
        .querySelector(".notification-close")
        .addEventListener(
            "click",
            () => item.remove()
        );

    setTimeout(() => {

        if (item.parentElement) {
            item.remove();
        }

    }, 4500);
}


// ============================================================
// USER
// ============================================================

async function getDashboardUser() {

    if (
        typeof window.user ===
        "function"
    ) {

        return await window.user();
    }

    try {

        const data =
            await dashboardApi(
                "/api/auth/me"
            );

        return data.authenticated
            ? data.user
            : null;

    } catch (error) {

        console.error(error);

        return null;
    }
}


// ============================================================
// HIDE ALL ROLE PANELS
// ============================================================

function hideRolePanels() {

    const ids = [
        "recipientPanel",
        "donorPanel",
        "claimsPanel",
        "claimRequestsPanel",
        "deliveryPanel",
        "adminPanel",
        "volunteerPanel"
    ];

    ids.forEach(id => {

        const element =
            document.getElementById(id);

        if (element) {

            element.style.display =
                "none";
        }
    });
}


// ============================================================
// SHOW ELEMENT
// ============================================================

function showElement(id) {

    const element =
        document.getElementById(id);

    if (element) {

        element.style.display =
            "";
    }
}


// ============================================================
// ROLE SETUP
// ============================================================

function setupRolePanels(user) {

    hideRolePanels();

    if (!user) {
        return;
    }

    const role =
        String(
            user.role || ""
        ).toUpperCase();

    if (role === "DONOR") {

        showElement(
            "donorPanel"
        );

        showElement(
            "claimRequestsPanel"
        );

        showElement(
            "deliveryPanel"
        );

    } else if (
        role === "RECIPIENT"
    ) {

        showElement(
            "recipientPanel"
        );

        showElement(
            "claimsPanel"
        );

        showElement(
            "deliveryPanel"
        );

    } else if (
        role === "VOLUNTEER"
    ) {

        showElement(
            "deliveryPanel"
        );

        showElement(
            "volunteerPanel"
        );

    } else if (
        role === "ADMIN"
    ) {

        showElement(
            "adminPanel"
        );

        showElement(
            "deliveryPanel"
        );
    }
}


// ============================================================
// STATS
// ============================================================

async function dashboardStats() {

    if (
        typeof window.loadDashboardStats ===
        "function"
    ) {

        await window.loadDashboardStats();

        return;
    }

    try {

        const data =
            await dashboardApi(
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
                data.foodCount || 0;
        }

        if (claims) {
            claims.textContent =
                data.claimCount || 0;
        }

        if (deliveries) {
            deliveries.textContent =
                data.deliveryCount || 0;
        }

    } catch (error) {

        console.error(
            "Stats error:",
            error
        );
    }
}


// ============================================================
// VOLUNTEER ASSIGNMENT MODAL
// ============================================================

function createVolunteerModal() {

    let modal =
        document.getElementById(
            "volunteerAssignmentModal"
        );

    if (modal) {
        return modal;
    }

    modal =
        document.createElement("div");

    modal.id =
        "volunteerAssignmentModal";

    modal.className =
        "foodloop-modal-overlay";

    modal.innerHTML = `

        <div class="foodloop-modal">

            <div class="foodloop-modal-header">

                <div>
                    <h2>
                        Choose Volunteer
                    </h2>

                    <p>
                        Select an available volunteer
                        for this delivery.
                    </p>
                </div>

                <button
                    type="button"
                    class="modal-close-btn"
                    id="closeVolunteerModal"
                >
                    ×
                </button>

            </div>

            <div
                id="volunteerModalBody"
                class="foodloop-modal-body"
            >
                <div class="loading-state">
                    Loading volunteers...
                </div>
            </div>

        </div>
    `;

    document.body.appendChild(
        modal
    );

    document
        .getElementById(
            "closeVolunteerModal"
        )
        .addEventListener(
            "click",
            closeVolunteerModal
        );

    modal.addEventListener(
        "click",
        function (event) {

            if (
                event.target === modal
            ) {

                closeVolunteerModal();
            }
        }
    );

    return modal;
}


// ============================================================
// CLOSE VOLUNTEER MODAL
// ============================================================

function closeVolunteerModal() {

    const modal =
        document.getElementById(
            "volunteerAssignmentModal"
        );

    if (modal) {

        modal.classList.remove(
            "show"
        );
    }
}


// ============================================================
// OPEN VOLUNTEER ASSIGNMENT
// ============================================================

async function openVolunteerAssignment(
    claimId
) {

    if (!claimId) {
        return;
    }

    const modal =
        createVolunteerModal();

    const body =
        document.getElementById(
            "volunteerModalBody"
        );

    modal.classList.add(
        "show"
    );

    body.innerHTML = `
        <div class="loading-state">
            Loading available volunteers...
        </div>
    `;

    try {

        let volunteers = [];

        if (
            typeof window.loadVolunteers ===
            "function"
        ) {

            volunteers =
                await window.loadVolunteers();

        } else {

            const data =
                await dashboardApi(
                    "/api/food?mode=volunteers"
                );

            volunteers =
                Array.isArray(data)
                    ? data
                    : [];
        }

        const available =
            volunteers.filter(
                volunteer =>
                    String(
                        volunteer.availability ||
                        ""
                    ).toUpperCase() ===
                    "AVAILABLE"
            );

        if (!available.length) {

            body.innerHTML = `

                <div class="empty-state volunteer-empty">

                    <div class="empty-icon">
                        🚚
                    </div>

                    <h3>
                        No volunteer available
                    </h3>

                    <p>
                        The claim was accepted,
                        but there is currently no
                        available volunteer.
                    </p>

                    <button
                        type="button"
                        class="secondary-btn"
                        id="closeNoVolunteer"
                    >
                        Close
                    </button>

                </div>
            `;

            document
                .getElementById(
                    "closeNoVolunteer"
                )
                .addEventListener(
                    "click",
                    closeVolunteerModal
                );

            dashNotify(
                "Claim accepted, but no volunteer is currently available.",
                "error"
            );

            return;
        }

        body.innerHTML = `

            <div class="volunteer-selection-intro">

                <p>
                    Select a volunteer and enter
                    the delivery details.
                </p>

            </div>

            <div class="volunteer-list">

                ${available.map(
                    volunteer => `

                    <label
                        class="volunteer-option"
                    >

                        <input
                            type="radio"
                            name="selectedVolunteer"
                            value="${volunteer.id}"
                        >

                        <div class="volunteer-option-content">

                            <div class="volunteer-avatar">
                                ${dashEscape(
                                    (
                                        volunteer.name ||
                                        "V"
                                    )
                                    .charAt(0)
                                    .toUpperCase()
                                )}
                            </div>

                            <div class="volunteer-info">

                                <strong>
                                    ${dashEscape(
                                        volunteer.name
                                    )}
                                </strong>

                                <span>
                                    ${dashEscape(
                                        volunteer.phone ||
                                        volunteer.email ||
                                        "Contact available"
                                    )}
                                </span>

                                <span>
                                    ${dashEscape(
                                        volunteer.location ||
                                        "Location not provided"
                                    )}
                                </span>

                                <span>
                                    Vehicle:
                                    ${dashEscape(
                                        volunteer.vehicle ||
                                        "Not specified"
                                    )}
                                </span>

                            </div>

                            <span class="available-badge">
                                AVAILABLE
                            </span>

                        </div>

                    </label>
                `
                ).join("")}

            </div>

            <div class="delivery-input-grid">

                <div class="form-group">

                    <label>
                        Distance (km)
                    </label>

                    <input
                        type="number"
                        id="deliveryDistance"
                        min="0.1"
                        step="0.1"
                        placeholder="Example: 5"
                    >

                </div>

                <div class="form-group">

                    <label>
                        Rate per km (₹)
                    </label>

                    <input
                        type="number"
                        id="deliveryRate"
                        min="0"
                        step="0.01"
                        value="10"
                        placeholder="Example: 10"
                    >

                </div>

            </div>

            <div
                id="deliveryFeePreview"
                class="delivery-fee-preview"
            >
                Volunteer payout:
                <strong>₹0.00</strong>
            </div>

            <div class="modal-actions">

                <button
                    type="button"
                    class="secondary-btn"
                    id="cancelVolunteerAssignment"
                >
                    Cancel
                </button>

                <button
                    type="button"
                    class="accept-btn pay-assign-btn"
                    id="payAssignVolunteer"
                >
                    Pay & Assign
                </button>

            </div>
        `;

        const distanceInput =
            document.getElementById(
                "deliveryDistance"
            );

        const rateInput =
            document.getElementById(
                "deliveryRate"
            );

        const feePreview =
            document.getElementById(
                "deliveryFeePreview"
            );

        function updateFee() {

            const distance =
                parseFloat(
                    distanceInput.value
                ) || 0;

            const rate =
                parseFloat(
                    rateInput.value
                ) || 0;

            const fee =
                Math.round(
                    distance *
                    rate *
                    100
                ) / 100;

            feePreview.innerHTML = `
                Volunteer payout:
                <strong>
                    ₹${fee.toFixed(2)}
                </strong>
            `;
        }

        distanceInput.addEventListener(
            "input",
            updateFee
        );

        rateInput.addEventListener(
            "input",
            updateFee
        );

        document
            .getElementById(
                "cancelVolunteerAssignment"
            )
            .addEventListener(
                "click",
                closeVolunteerModal
            );

        document
            .getElementById(
                "payAssignVolunteer"
            )
            .addEventListener(
                "click",
                async function () {

                    const selected =
                        document.querySelector(
                            "input[name='selectedVolunteer']:checked"
                        );

                    if (!selected) {

                        dashNotify(
                            "Please select a volunteer.",
                            "error"
                        );

                        return;
                    }

                    const distance =
                        parseFloat(
                            distanceInput.value
                        );

                    const rate =
                        parseFloat(
                            rateInput.value
                        );

                    if (
                        !distance ||
                        distance <= 0
                    ) {

                        dashNotify(
                            "Please enter a valid delivery distance.",
                            "error"
                        );

                        return;
                    }

                    if (
                        Number.isNaN(rate) ||
                        rate < 0
                    ) {

                        dashNotify(
                            "Please enter a valid rate.",
                            "error"
                        );

                        return;
                    }

                    await assignVolunteer(
                        claimId,
                        selected.value,
                        distance,
                        rate
                    );
                }
            );

        updateFee();

    } catch (error) {

        console.error(error);

        body.innerHTML = `
            <div class="empty-state">
                Unable to load volunteers.
            </div>
        `;

        dashNotify(
            error.message ||
            "Unable to load volunteers.",
            "error"
        );
    }
}


// ============================================================
// ASSIGN VOLUNTEER / PAY
// ============================================================

async function assignVolunteer(
    claimId,
    volunteerId,
    distanceKm,
    ratePerKm
) {

    const button =
        document.getElementById(
            "payAssignVolunteer"
        );

    if (button) {

        button.disabled = true;

        button.textContent =
            "Processing...";
    }

    try {

        const body =
            new URLSearchParams();

        body.append(
            "action",
            "assign"
        );

        body.append(
            "claimId",
            claimId
        );

        body.append(
            "volunteerId",
            volunteerId
        );

        body.append(
            "distanceKm",
            distanceKm
        );

        body.append(
            "ratePerKm",
            ratePerKm
        );

        const data =
            await dashboardApi(
                "/api/delivery",
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
                "Unable to assign volunteer."
            );
        }

        closeVolunteerModal();

        dashNotify(
            data.message ||
            "Volunteer assigned and payout recorded successfully.",
            "success"
        );

        await dashboardStats();

        if (
            typeof window.loadClaimRequests ===
            "function"
        ) {

            await window.loadClaimRequests();
        }

        await loadDeliveries();

    } catch (error) {

        console.error(error);

        dashNotify(
            error.message ||
            "Unable to assign volunteer.",
            "error"
        );

    } finally {

        if (button) {

            button.disabled = false;

            button.textContent =
                "Pay & Assign";
        }
    }
}


// ============================================================
// DELIVERIES
// ============================================================

async function loadDeliveries() {

    const container =
        document.getElementById(
            "deliveries"
        );

    if (!container) {
        return;
    }

    container.innerHTML =
        `<div class="loading-state">Loading deliveries...</div>`;

    try {

        const data =
            await dashboardApi(
                "/api/delivery?mode=mine"
            );

        const deliveries =
            Array.isArray(data)
                ? data
                : [];

        if (!deliveries.length) {

            container.innerHTML = `
                <div class="empty-state">
                    No deliveries yet.
                </div>
            `;

            return;
        }

        container.innerHTML =
            deliveries.map(
                delivery => {

                    return `

                        <div class="delivery-card">

                            <div class="delivery-main">

                                <h3>
                                    ${dashEscape(
                                        delivery.foodName
                                    )}
                                </h3>

                                <p>
                                    Donor:
                                    ${dashEscape(
                                        delivery.donorName
                                    )}
                                </p>

                                <p>
                                    Recipient:
                                    ${dashEscape(
                                        delivery.recipientName
                                    )}
                                </p>

                                <p>
                                    Volunteer:
                                    ${dashEscape(
                                        delivery.volunteerName ||
                                        "Not assigned"
                                    )}
                                </p>

                            </div>

                            <div class="delivery-meta">

                                <span class="claim-status">
                                    ${dashEscape(
                                        delivery.status
                                    )}
                                </span>

                                <span>
                                    Payout:
                                    ₹${Number(
                                        delivery.volunteerFee || 0
                                    ).toFixed(2)}
                                </span>

                                <span>
                                    Payment:
                                    ${dashEscape(
                                        delivery.paymentStatus ||
                                        "PENDING"
                                    )}
                                </span>

                            </div>

                        </div>
                    `;
                }
            ).join("");

    } catch (error) {

        console.error(error);

        container.innerHTML = `
            <div class="empty-state">
                Unable to load deliveries.
            </div>
        `;
    }
}


// ============================================================
// CATEGORY FILTER
// ============================================================

function setupCategoryFilter() {

    const filter =
        document.getElementById(
            "filter"
        );

    if (!filter) {
        return;
    }

    filter.addEventListener(
        "change",
        function () {

            if (
                typeof window.filterAvailableFood ===
                "function"
            ) {

                window.filterAvailableFood();
            }
        }
    );
}


// ============================================================
// LOGOUT
// ============================================================

function setupDashboardLogout() {

    const button =
        document.getElementById(
            "logoutBtn"
        );

    if (!button) {
        return;
    }

    button.addEventListener(
        "click",
        async function (event) {

            event.preventDefault();

            if (
                typeof window.logout ===
                "function"
            ) {

                await window.logout();

            } else {

                window.location.href =
                    "/index.html";
            }
        }
    );
}


// ============================================================
// MAIN DASHBOARD START
// ============================================================

async function startFoodLoopDashboard() {

    console.log(
        "FoodLoop dashboard starting..."
    );

    const currentUser =
        await getDashboardUser();

    if (!currentUser) {

        window.location.href =
            "/index.html";

        return;
    }

    // --------------------------------------------------------
    // User name
    // --------------------------------------------------------

    const name =
        document.getElementById(
            "userName"
        );

    if (name) {

        name.textContent =
            currentUser.name ||
            "User";
    }

    const navName =
        document.getElementById(
            "navUserName"
        );

    if (navName) {

        navName.textContent =
            currentUser.name ||
            "User";
    }

    const role =
        document.getElementById(
            "userRole"
        );

    if (role) {

        role.textContent =
            currentUser.role ||
            "";
    }

    const navRole =
        document.getElementById(
            "navUserRole"
        );

    if (navRole) {

        navRole.textContent =
            currentUser.role ||
            "";
    }

    // --------------------------------------------------------
    // Role panels
    // --------------------------------------------------------

    setupRolePanels(
        currentUser
    );

    // --------------------------------------------------------
    // Stats
    // --------------------------------------------------------

    await dashboardStats();

    // --------------------------------------------------------
    // Common setup
    // --------------------------------------------------------

    setupCategoryFilter();

    setupDashboardLogout();

    // --------------------------------------------------------
    // Role-specific loading
    // --------------------------------------------------------

    const currentRole =
        String(
            currentUser.role || ""
        ).toUpperCase();

    if (
        currentRole === "RECIPIENT"
    ) {

        if (
            typeof window.loadAvailableFood ===
            "function"
        ) {

            await window.loadAvailableFood();
        }

        if (
            typeof window.loadMyClaims ===
            "function"
        ) {

            await window.loadMyClaims();
        }

    } else if (
        currentRole === "DONOR"
    ) {

        if (
            typeof window.loadDonorFood ===
            "function"
        ) {

            await window.loadDonorFood();
        }

        if (
            typeof window.loadClaimRequests ===
            "function"
        ) {

            await window.loadClaimRequests();
        }
    }

    await loadDeliveries();

    console.log(
        "FoodLoop dashboard ready."
    );
}


// ============================================================
// GLOBAL EXPORTS
// ============================================================

window.openVolunteerAssignment =
    openVolunteerAssignment;

window.closeVolunteerModal =
    closeVolunteerModal;

window.assignVolunteer =
    assignVolunteer;

window.loadDeliveries =
    loadDeliveries;

window.startFoodLoopDashboard =
    startFoodLoopDashboard;


// ============================================================
// START
// ============================================================

document.addEventListener(
    "DOMContentLoaded",
    function () {

        startFoodLoopDashboard();

    }
);