<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard &middot; BookFace</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <script src="https://d3js.org/d3.v7.min.js"></script>
</head>
<body>
<c:set var="active" value="dashboard" />
<%@ include file="_nav.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<div class="container wide">
    <div class="page-head dash-head">
        <div>
            <h1>Algorithm Dashboard</h1>
            <p>A force-directed view of the friendship graph. Pick a user to see how
               <code>suggestFriends()</code> ranks non-friends by shared connections.</p>
        </div>
        <form method="get" action="${ctx}/social-network" class="user-picker" id="focusUserForm">
            <input type="hidden" name="action" value="dashboard">
            <input type="hidden" name="userId" id="selectedUserId" value="${empty selectedUserId ? '' : selectedUserId}">
            <div class="search-wrapper">
                <input type="text" id="focusUserSearch" class="search-input" placeholder="Search for focus user..." autocomplete="off">
                <ul class="search-dropdown" id="focusUserDropdown" style="display: none;"></ul>
            </div>
        </form>
    </div>

    <div class="stats" id="metrics">
        <div class="stat a"><div class="n" id="m-nodes">-</div><div class="l">Nodes</div></div>
        <div class="stat d"><div class="n" id="m-edges">-</div><div class="l">Edges</div></div>
        <div class="stat b"><div class="n" id="m-deg">-</div><div class="l">Avg degree</div></div>
        <div class="stat c"><div class="n" id="m-density">-</div><div class="l">Density</div></div>
    </div>

    <div class="dash-grid">
        <!-- Graph canvas -->
        <div class="card viz-card">
            <h2>Friendship graph <span class="tag">d3-force</span></h2>
            <p class="hint" id="viz-caption">Hover a suggested (orange) node to light up the mutual-friend
               paths that justify it.</p>
            <div id="viz"></div>
        </div>

        <!-- Side panel -->
        <aside class="side">
            <div class="card">
                <h2>Legend</h2>
                <ul class="legend">
                    <li><span class="dot" style="background:#1877f2"></span> Selected user</li>
                    <li><span class="dot" style="background:#42b72a"></span> Direct friend</li>
                    <li><span class="dot" style="background:#f7b928"></span> Suggested (sized by mutuals)</li>
                    <li><span class="dot" style="background:#8a3ffc"></span> Mutual-friend bridge</li>
                    <li><span class="dot" style="background:#bcc0c4"></span> Other user</li>
                </ul>
            </div>

            <div class="card">
                <h2>Top suggestions <span class="tag">max-heap</span></h2>
                <p class="hint" id="rank-hint">Ranked by mutual-friend count.</p>
                <ol class="ranklist" id="ranklist"></ol>
            </div>

            <div class="card explainer">
                <h2>How it works</h2>
                <p><strong>Suggestions = non-friends ranked by number of mutual friends.</strong></p>
                <ol class="steps">
                    <li>Graph stored as an <b>adjacency list</b> (each user -> neighbour IDs).</li>
                    <li>For every non-friend candidate, intersect the two friend sets with a
                        <b>HashSet</b> -> mutual count in <code>O(deg)</code>.</li>
                    <li>Candidates pushed into a <b>max-heap</b>; the top-K are extracted as the
                        ranked suggestions.</li>
                </ol>
                <p class="cx">Complexity: <code>O(V * deg + K * log V)</code> per query.</p>
            </div>
        </aside>
    </div>
</div>

<%-- ============================================================
     Data contract -> inline JS object (no JSON API, no AJAX).
     The controller's request attributes are emitted here so
     dashboard.js can drive the D3 simulation directly.
     ============================================================ --%>
<script>
  window.GRAPH = {
    nodes: [
      <c:forEach var="u" items="${users}">{ id: ${u.id}, name: "${u.fullName}" },
      </c:forEach>
    ],
    links: [
      <c:forEach var="entry" items="${relationships}">
        <c:forEach var="fid" items="${entry.value}">
          <c:if test="${fid > entry.key}">{ source: ${entry.key}, target: ${fid} },
          </c:if>
        </c:forEach>
      </c:forEach>
    ],
    selectedId: <c:choose><c:when test="${empty selectedUserId}">null</c:when><c:otherwise>${selectedUserId}</c:otherwise></c:choose>,
    directFriends: [
      <c:if test="${not empty selectedUserId}">
        <c:forEach var="fid" items="${relationships[selectedUserId]}">${fid}, </c:forEach>
      </c:if>
    ],
    suggestions: [
      <c:forEach var="s" items="${suggestions}">{ id: ${s.suggestedId}, mutual: ${s.mutualCount} },
      </c:forEach>
    ],
    mutualsBySuggested: {
      <c:forEach var="e" items="${mutualsBySuggested}">"${e.key}": [<c:forEach var="m" items="${e.value}" varStatus="ms">${m}<c:if test="${not ms.last}">, </c:if></c:forEach>],
      </c:forEach>
    }
  };
</script>
<script src="${ctx}/js/dashboard.js"></script>

<footer class="app">BookFace &middot; D3 v7 force simulation &middot; data injected server-side by MainController</footer>
</body>
</html>
