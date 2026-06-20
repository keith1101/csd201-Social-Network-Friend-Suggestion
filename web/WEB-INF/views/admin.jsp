<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin &middot; BookFace</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<c:set var="active" value="admin" />
<%@ include file="_nav.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<%-- Degree sum (directed) and undirected edge count, kept as integers --%>
<c:set var="degreeSum" value="0" />
<c:set var="edges" value="0" />
<c:forEach var="entry" items="${relationships}">
    <c:set var="degreeSum" value="${degreeSum + entry.value.size()}" />
    <c:forEach var="fid" items="${entry.value}">
        <c:if test="${fid > entry.key}"><c:set var="edges" value="${edges + 1}" /></c:if>
    </c:forEach>
</c:forEach>

<div class="container">
    <div class="page-head">
        <h1>Admin Console</h1>
        <p>Manage the vertices (users) and edges (friendships) of the social graph. Every change is
           written to SQL Server first, then applied to the in-memory adjacency list.</p>
    </div>

    <c:if test="${not empty message}">
        <div class="banner ok"><span><c:out value="${message}" /></span></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="banner error"><span><c:out value="${error}" /></span></div>
    </c:if>

    <div class="stats">
        <div class="stat a"><div class="n">${users.size()}</div><div class="l">Users (vertices)</div></div>
        <div class="stat d"><div class="n">${edges}</div><div class="l">Friendships (edges)</div></div>
        <div class="stat b"><div class="n">${degreeSum}</div><div class="l">Directed degree sum</div></div>
    </div>

    <div class="grid cols-2">
        <!-- Register -->
        <div class="card">
            <h2><span class="tag">addUser</span> Register user</h2>
            <p class="hint">Inserts a new vertex. ID must be a positive integer and unique.</p>
            <form method="post" action="${ctx}/social-network">
                <input type="hidden" name="action" value="register">
                <label>User ID</label>
                <input type="text" name="userId" placeholder="e.g. 7" required>
                <label>Full name</label>
                <input type="text" name="fullName" placeholder="e.g. Ada Lovelace" required>
                <button class="btn success" type="submit">Register user</button>
            </form>
        </div>

        <!-- Remove -->
        <div class="card">
            <h2><span class="tag">removeUser</span> Remove user</h2>
            <p class="hint">Deletes the vertex and every incident edge (their friendships).</p>
            <form method="post" action="${ctx}/social-network" id="removeUserForm"
                  onsubmit="return confirm('Remove this user and all their friendships?');">
                <input type="hidden" name="action" value="removeUser">
                <label>User</label>
                <div class="search-wrapper">
                    <input type="hidden" name="userId" id="rmUser-id">
                    <input type="text" id="rmUser-search" class="search-input"
                           placeholder="Search name or ID…" autocomplete="off">
                    <ul class="search-dropdown" id="rmUser-dropdown" style="display:none;"></ul>
                </div>
                <button class="btn danger" type="submit">Remove user</button>
            </form>
        </div>

        <!-- Make friend -->
        <div class="card">
            <h2><span class="tag">addFriendship</span> Make friend</h2>
            <p class="hint">Adds an undirected edge between two distinct, existing users.</p>
            <form method="post" action="${ctx}/social-network" id="makeFriendForm">
                <input type="hidden" name="action" value="makeFriend">
                <div class="row">
                    <div>
                        <label>User A</label>
                        <div class="search-wrapper">
                            <input type="hidden" name="userId1" id="mfA-id">
                            <input type="text" id="mfA-search" class="search-input"
                                   placeholder="Search name or ID…" autocomplete="off">
                            <ul class="search-dropdown" id="mfA-dropdown" style="display:none;"></ul>
                        </div>
                    </div>
                    <div>
                        <label>User B</label>
                        <div class="search-wrapper">
                            <input type="hidden" name="userId2" id="mfB-id">
                            <input type="text" id="mfB-search" class="search-input"
                                   placeholder="Pick User A first…" autocomplete="off">
                            <ul class="search-dropdown" id="mfB-dropdown" style="display:none;"></ul>
                        </div>
                    </div>
                </div>
                <button class="btn primary" type="submit">Connect</button>
            </form>
        </div>

        <!-- Unfriend -->
        <div class="card">
            <h2><span class="tag">removeFriendship</span> Unfriend</h2>
            <p class="hint">Removes the undirected edge between two users.</p>
            <form method="post" action="${ctx}/social-network" id="unfriendForm">
                <input type="hidden" name="action" value="unfriend">
                <div class="row">
                    <div>
                        <label>User A</label>
                        <div class="search-wrapper">
                            <input type="hidden" name="userId1" id="ufA-id">
                            <input type="text" id="ufA-search" class="search-input"
                                   placeholder="Search name or ID…" autocomplete="off">
                            <ul class="search-dropdown" id="ufA-dropdown" style="display:none;"></ul>
                        </div>
                    </div>
                    <div>
                        <label>User B</label>
                        <div class="search-wrapper">
                            <input type="hidden" name="userId2" id="ufB-id">
                            <input type="text" id="ufB-search" class="search-input"
                                   placeholder="Pick User A first…" autocomplete="off">
                            <ul class="search-dropdown" id="ufB-dropdown" style="display:none;"></ul>
                        </div>
                    </div>
                </div>
                <button class="btn secondary" type="submit">Unfriend</button>
            </form>
        </div>
    </div>
</div>

<footer class="app">BookFace Admin &middot; MVC-2 single controller &middot; Tomcat 9 / Java EE 7</footer>
<script>
  window.ADMIN_DATA = {
    users: [
      <c:forEach var="u" items="${users}">{ id: ${u.id}, name: "<c:out value="${u.fullName}" />" },
      </c:forEach>
    ],
    friends: {
      <c:forEach var="e" items="${relationships}">"${e.key}": [<c:forEach var="m" items="${e.value}" varStatus="ms">${m}<c:if test="${not ms.last}">, </c:if></c:forEach>],
      </c:forEach>
    }
  };
</script>
<script src="${ctx}/js/admin.js"></script>
</body>
</html>
