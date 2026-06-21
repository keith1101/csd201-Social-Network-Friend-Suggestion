<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BookFace</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<c:set var="active" value="user" />
<%@ include file="_nav.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<%-- Resolve selected user's display name --%>
<c:set var="selectedName" value="" />
<c:forEach var="u" items="${users}">
    <c:if test="${u.id == selectedUserId}"><c:set var="selectedName" value="${u.fullName}" /></c:if>
</c:forEach>

<div class="container">
    <div class="page-head">
        <h1>Find friends</h1>
        <p>Pick a profile to manage its connections and see who BookFace suggests — ranked by mutual friends.</p>
    </div>

    <div class="card pick-card">
        <form method="get" action="${ctx}/social-network" class="pick-form" id="viewingAsForm">
            <input type="hidden" name="action" value="user">
            <input type="hidden" name="userId" id="viewingAsUserId" value="${empty selectedUserId ? '' : selectedUserId}">
            <label>Viewing as</label>
            <div class="search-wrapper">
                <input type="text" id="viewingAsSearch" class="search-input" placeholder="Search for a profile…" autocomplete="off">
                <ul class="search-dropdown" id="viewingAsDropdown" style="display: none;"></ul>
            </div>
        </form>
    </div>

    <c:if test="${not empty message}">
        <div class="banner ok"><span><c:out value="${message}" /></span></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="banner error"><span><c:out value="${error}" /></span></div>
    </c:if>

    <c:if test="${not empty selectedUserId}">
        <!-- Profile header -->
        <div class="card profile">
            <div class="avatar"><c:out value="${fn:toUpperCase(fn:substring(selectedName, 0, 1))}" /></div>
            <div class="profile-meta">
                <h2><c:out value="${selectedName}" /></h2>
                <p>User ID ${selectedUserId} · ${relationships[selectedUserId].size()} friend<c:if test="${relationships[selectedUserId].size() != 1}">s</c:if></p>
            </div>
            <a class="btn ghost slim" href="${ctx}/social-network?action=dashboard&userId=${selectedUserId}">Open dashboard</a>
        </div>

        <div class="grid cols-2">
            <!-- Friends + unfriend -->
            <div class="card">
                <h2>Friends</h2>
                <p class="hint">People <c:out value="${selectedName}" /> is connected to.</p>
                <c:choose>
                    <c:when test="${empty relationships[selectedUserId]}">
                        <p class="empty">No friends yet — add some on the right.</p>
                    </c:when>
                    <c:otherwise>
                        <input type="text" id="friends-search" class="search-input"
                               placeholder="Search your friends…" autocomplete="off"
                               oninput="filterFriendList(this.value)">
                        <ul class="people scroll" id="friends-list">
                            <c:forEach var="fid" items="${relationships[selectedUserId]}">
                                <c:set var="fname" value="" />
                                <c:forEach var="u" items="${users}">
                                    <c:if test="${u.id == fid}"><c:set var="fname" value="${u.fullName}" /></c:if>
                                </c:forEach>
                                <li class="friend-row" data-search="${fn:toLowerCase(fname)} ${fid}">
                                    <span class="ava sm"><c:out value="${fn:toUpperCase(fn:substring(fname, 0, 1))}" /></span>
                                    <span class="who"><c:out value="${fname}" /><small>ID ${fid}</small></span>
                                    <form method="post" action="${ctx}/social-network">
                                        <input type="hidden" name="action" value="unfriend">
                                        <input type="hidden" name="userId1" value="${selectedUserId}">
                                        <input type="hidden" name="userId2" value="${fid}">
                                        <input type="hidden" name="returnUserId" value="${selectedUserId}">
                                        <button class="btn secondary slim" type="submit">Unfriend</button>
                                    </form>
                                </li>
                            </c:forEach>
                        </ul>
                        <p class="empty" id="friends-no-match" style="display:none;">No friends match that search.</p>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Add friend (search, not a dropdown) -->
            <div class="card">
                <h2>Add a friend</h2>
                <p class="hint">Search by name or ID — only people who aren’t already friends are listed.</p>

                <input type="text" id="friend-search" class="search-input"
                       placeholder="Search people…" autocomplete="off"
                       oninput="filterFriendCandidates(this.value)">

                <c:set var="candidateCount" value="0" />
                <ul class="people scroll" id="friend-candidates">
                    <c:forEach var="u" items="${users}">
                        <c:if test="${u.id != selectedUserId}">
                            <c:set var="alreadyFriend" value="false" />
                            <c:forEach var="fid" items="${relationships[selectedUserId]}">
                                <c:if test="${fid == u.id}"><c:set var="alreadyFriend" value="true" /></c:if>
                            </c:forEach>
                            <c:if test="${not alreadyFriend}">
                                <c:set var="candidateCount" value="${candidateCount + 1}" />
                                <li class="candidate-row"
                                    data-search="${fn:toLowerCase(u.fullName)} ${u.id}">
                                    <span class="ava sm"><c:out value="${fn:toUpperCase(fn:substring(u.fullName,0,1))}" /></span>
                                    <span class="who"><c:out value="${u.fullName}" /><small>ID ${u.id}</small></span>
                                    <form method="post" action="${ctx}/social-network">
                                        <input type="hidden" name="action" value="makeFriend">
                                        <input type="hidden" name="userId1" value="${selectedUserId}">
                                        <input type="hidden" name="userId2" value="${u.id}">
                                        <input type="hidden" name="returnUserId" value="${selectedUserId}">
                                        <button class="btn primary slim" type="submit">Add friend</button>
                                    </form>
                                </li>
                            </c:if>
                        </c:if>
                    </c:forEach>
                </ul>
                <c:if test="${candidateCount == 0}">
                    <p class="empty">No one left to add — everyone is already a friend.</p>
                </c:if>
                <p class="empty" id="friend-no-match" style="display:none;">No one matches that search.</p>
            </div>
        </div>

        <!-- Suggestions with one-click add -->
        <div class="card">
            <h2>People you may know</h2>
            <p class="hint">Non-friends ranked by mutual-friend count (max-heap, top 5). One click to connect.</p>
            <c:choose>
                <c:when test="${empty suggestions}">
                    <p class="empty">No suggestions yet — not enough mutual connections.</p>
                </c:when>
                <c:otherwise>
                    <div class="suggest-grid">
                        <c:forEach var="s" items="${suggestions}">
                            <c:set var="sname" value="" />
                            <c:forEach var="u" items="${users}">
                                <c:if test="${u.id == s.suggestedId}"><c:set var="sname" value="${u.fullName}" /></c:if>
                            </c:forEach>
                            <div class="suggest">
                                <span class="ava"><c:out value="${fn:toUpperCase(fn:substring(sname, 0, 1))}" /></span>
                                <span class="who"><c:out value="${sname}" /></span>
                                <span class="badge">${s.mutualCount} mutual</span>
                                <form method="post" action="${ctx}/social-network">
                                    <input type="hidden" name="action" value="makeFriend">
                                    <input type="hidden" name="userId1" value="${selectedUserId}">
                                    <input type="hidden" name="userId2" value="${s.suggestedId}">
                                    <input type="hidden" name="returnUserId" value="${selectedUserId}">
                                    <button class="btn primary slim" type="submit">Add friend</button>
                                </form>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>
</div>

<footer class="app">BookFace · server-rendered JSP/JSTL · adjacency list + max-heap suggestions</footer>
<script>
  const USERS = [
    <c:forEach var="u" items="${users}">{ id: ${u.id}, fullName: "<c:out value="${u.fullName}" />" },
    </c:forEach>
  ];
</script>
<script src="${pageContext.request.contextPath}/js/friend-search.js"></script>
</body>
</html>
