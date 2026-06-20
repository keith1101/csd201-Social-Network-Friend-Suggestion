/* ============================================================
   friend-search.js — client-side filters for user.jsp's two
   growable lists: current friends and add-friend candidates.
   No fetch/AJAX: rows are already server-rendered in the DOM,
   this just shows/hides them as the user types.
   ============================================================ */
function escapeHtml(s) {
    return String(s == null ? "" : s)
        .replace(/&/g, "&amp;").replace(/</g, "&lt;")
        .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

function filterRows(listId, query, noMatchId, rowSelector) {
    "use strict";
    var q = query.trim().toLowerCase();
    var rows = document.querySelectorAll("#" + listId + " " + rowSelector);
    var visible = 0;

    rows.forEach(function (row) {
        var match = row.getAttribute("data-search").indexOf(q) !== -1;
        row.style.display = match ? "" : "none";
        if (match) visible++;
    });

    var noMatch = document.getElementById(noMatchId);
    if (noMatch) {
        noMatch.style.display = (rows.length > 0 && visible === 0) ? "" : "none";
    }
}

function filterFriendCandidates(query) {
    "use strict";
    filterRows("friend-candidates", query, "friend-no-match", ".candidate-row");
}

function filterFriendList(query) {
    "use strict";
    filterRows("friends-list", query, "friends-no-match", ".friend-row");
}

/* ============================================================
   Viewing-as profile search (replaces dropdown)
   ============================================================ */
(function () {
    "use strict";
    var searchInput = document.getElementById("viewingAsSearch");
    var dropdown = document.getElementById("viewingAsDropdown");
    var hiddenUserId = document.getElementById("viewingAsUserId");
    var viewingAsForm = document.getElementById("viewingAsForm");
    var userMap = {}; // Will be populated by inline JSP data

    if (!searchInput) return;

    // Build user map from embedded USERS data
    if (typeof USERS !== "undefined" && Array.isArray(USERS)) {
        USERS.forEach(function (u) {
            userMap[u.id] = { id: u.id, name: u.fullName || u.name };
        });
    }

    function updateSearchDropdown(query) {
        var q = query.trim().toLowerCase();
        var userList = Object.values(userMap);
        dropdown.innerHTML = "";
        var matched = [];

        userList.forEach(function (u) {
            var searchText = (u.name.toLowerCase() + " " + u.id).toString();
            if (searchText.indexOf(q) !== -1) {
                matched.push(u);
            }
        });

        matched.forEach(function (u) {
            var li = document.createElement("li");
            li.setAttribute("data-search", u.name.toLowerCase() + " " + u.id);
            li.innerHTML = "<strong>[" + u.id + "]</strong> " + escapeHtml(u.name);
            li.addEventListener("click", function () {
                hiddenUserId.value = u.id;
                searchInput.value = "[" + u.id + "] " + u.name;
                dropdown.style.display = "none";
                viewingAsForm.submit();
            });
            dropdown.appendChild(li);
        });

        dropdown.style.display = (q.length > 0 && matched.length > 0) ? "" : "none";
    }

    searchInput.addEventListener("input", function (e) {
        updateSearchDropdown(e.target.value);
    });

    searchInput.addEventListener("focus", function (e) {
        if (e.target.value.length > 0) {
            updateSearchDropdown(e.target.value);
        }
    });

    // Close dropdown when clicking outside
    document.addEventListener("click", function (e) {
        if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
            dropdown.style.display = "none";
        }
    });

    // Set initial search value if user is selected
    var currentUserId = hiddenUserId.value;
    if (currentUserId && userMap[currentUserId]) {
        searchInput.value = "[" + currentUserId + "] " + userMap[currentUserId].name;
    }
})();
