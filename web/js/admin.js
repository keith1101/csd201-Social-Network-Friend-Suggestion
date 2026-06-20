/* ============================================================
   admin.js — searchable user pickers for the Admin Console.

   Replaces the old <select> dropdowns on admin.jsp with
   type-to-filter inputs. Unlike friend-search.js these pickers
   do NOT auto-submit: the admin picks A, then B, then clicks the
   button. User B's candidate list depends on User A and the
   friendship graph (ADMIN_DATA, injected inline by admin.jsp).
   No fetch/AJAX.
   ============================================================ */
(function () {
    "use strict";

    const DATA = window.ADMIN_DATA || { users: [], friends: {} };

    function escapeHtml(s) {
        return String(s == null ? "" : s)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }

    // ---- graph helpers ------------------------------------------
    function allUsers() {
        return DATA.users;
    }
    function friendIdsOf(id) {
        return DATA.friends[id] || [];
    }
    function isFriend(a, b) {
        return friendIdsOf(a).indexOf(Number(b)) !== -1;
    }
    function userById(id) {
        return DATA.users.find(u => u.id === Number(id)) || null;
    }

    // ============================================================
    //  Reusable searchable picker (no auto-submit)
    // ============================================================
    // opts: { inputId, dropdownId, hiddenId, getCandidates, onSelect }
    //   getCandidates() -> array of { id, name }, re-evaluated every
    //   keystroke so dependent lists stay current.
    function initUserSearchPicker(opts) {
        const input = document.getElementById(opts.inputId);
        const dropdown = document.getElementById(opts.dropdownId);
        const hidden = document.getElementById(opts.hiddenId);
        if (!input || !dropdown || !hidden) return null;

        function render(query) {
            const q = query.trim().toLowerCase();
            const candidates = opts.getCandidates();
            dropdown.innerHTML = "";

            const matched = candidates.filter(u =>
                (u.name.toLowerCase() + " " + u.id).indexOf(q) !== -1);

            matched.forEach(u => {
                const li = document.createElement("li");
                li.setAttribute("data-search", u.name.toLowerCase() + " " + u.id);
                li.innerHTML = "<strong>[" + u.id + "]</strong> " + escapeHtml(u.name);
                li.addEventListener("click", () => {
                    hidden.value = u.id;
                    input.value = "[" + u.id + "] " + u.name;
                    dropdown.style.display = "none";
                    if (typeof opts.onSelect === "function") opts.onSelect(u);
                });
                dropdown.appendChild(li);
            });

            dropdown.style.display = matched.length > 0 ? "" : "none";
        }

        // Typing in the box invalidates any previous concrete selection.
        input.addEventListener("input", (e) => {
            hidden.value = "";
            render(e.target.value);
        });
        input.addEventListener("focus", (e) => render(e.target.value));

        document.addEventListener("click", (e) => {
            if (!input.contains(e.target) && !dropdown.contains(e.target)) {
                dropdown.style.display = "none";
            }
        });

        return {
            clear() {
                hidden.value = "";
                input.value = "";
                dropdown.innerHTML = "";
                dropdown.style.display = "none";
            },
            get value() { return hidden.value; }
        };
    }

    // ============================================================
    //  Wire up the five pickers
    // ============================================================

    // Remove user — any user.
    initUserSearchPicker({
        inputId: "rmUser-search",
        dropdownId: "rmUser-dropdown",
        hiddenId: "rmUser-id",
        getCandidates: allUsers
    });

    // Make friend — B excludes A and A's existing friends (no self/duplicate edge).
    let mfB;
    const mfA = initUserSearchPicker({
        inputId: "mfA-search",
        dropdownId: "mfA-dropdown",
        hiddenId: "mfA-id",
        getCandidates: allUsers,
        onSelect: () => { if (mfB) mfB.clear(); }
    });
    mfB = initUserSearchPicker({
        inputId: "mfB-search",
        dropdownId: "mfB-dropdown",
        hiddenId: "mfB-id",
        getCandidates: () => {
            const a = mfA && mfA.value;
            if (!a) return [];
            return allUsers().filter(u => u.id !== Number(a) && !isFriend(a, u.id));
        }
    });

    // Unfriend — B shows only A's current friends.
    let ufB;
    const ufA = initUserSearchPicker({
        inputId: "ufA-search",
        dropdownId: "ufA-dropdown",
        hiddenId: "ufA-id",
        getCandidates: allUsers,
        onSelect: () => { if (ufB) ufB.clear(); }
    });
    ufB = initUserSearchPicker({
        inputId: "ufB-search",
        dropdownId: "ufB-dropdown",
        hiddenId: "ufB-id",
        getCandidates: () => {
            const a = ufA && ufA.value;
            if (!a) return [];
            return friendIdsOf(a).map(userById).filter(Boolean);
        }
    });

    // ============================================================
    //  Submit guards (hidden inputs are exempt from HTML5 `required`)
    // ============================================================
    function guardSingle(formId, hiddenId, message) {
        const form = document.getElementById(formId);
        if (!form) return;
        form.addEventListener("submit", (e) => {
            if (!document.getElementById(hiddenId).value) {
                e.preventDefault();
                alert(message);
            }
        });
    }

    function guardPair(formId, hiddenAId, hiddenBId, message) {
        const form = document.getElementById(formId);
        if (!form) return;
        form.addEventListener("submit", (e) => {
            const a = document.getElementById(hiddenAId).value;
            const b = document.getElementById(hiddenBId).value;
            if (!a || !b || a === b) {
                e.preventDefault();
                alert(message);
            }
        });
    }

    guardSingle("removeUserForm", "rmUser-id", "Please select a user to remove.");
    guardPair("makeFriendForm", "mfA-id", "mfB-id",
        "Please select two different users for the friendship.");
    guardPair("unfriendForm", "ufA-id", "ufB-id",
        "Please select User A and one of their friends.");
})();
