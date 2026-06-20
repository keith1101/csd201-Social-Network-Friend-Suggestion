/* ============================================================
   friend-search.js — client-side filters for user.jsp's two
   growable lists: current friends and add-friend candidates.
   No fetch/AJAX: rows are already server-rendered in the DOM,
   this just shows/hides them as the user types.
   ============================================================ */
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
