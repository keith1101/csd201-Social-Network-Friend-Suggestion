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
            <form method="post" action="${ctx}/social-network"
                  onsubmit="return confirm('Remove this user and all their friendships?');">
                <input type="hidden" name="action" value="removeUser">
                <label>User</label>
                <select name="userId" required>
                    <option value="" disabled selected>Select a user…</option>
                    <c:forEach var="u" items="${users}">
                        <option value="${u.id}">[${u.id}] <c:out value="${u.fullName}" /></option>
                    </c:forEach>
                </select>
                <button class="btn danger" type="submit">Remove user</button>
            </form>
        </div>

        <!-- Make friend -->
        <div class="card">
            <h2><span class="tag">addFriendship</span> Make friend</h2>
            <p class="hint">Adds an undirected edge between two distinct, existing users.</p>
            <form method="post" action="${ctx}/social-network">
                <input type="hidden" name="action" value="makeFriend">
                <div class="row">
                    <div>
                        <label>User A</label>
                        <select name="userId1" required>
                            <option value="" disabled selected>Select…</option>
                            <c:forEach var="u" items="${users}">
                                <option value="${u.id}">[${u.id}] <c:out value="${u.fullName}" /></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div>
                        <label>User B</label>
                        <select name="userId2" required>
                            <option value="" disabled selected>Select…</option>
                            <c:forEach var="u" items="${users}">
                                <option value="${u.id}">[${u.id}] <c:out value="${u.fullName}" /></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <button class="btn primary" type="submit">Connect</button>
            </form>
        </div>

        <!-- Unfriend -->
        <div class="card">
            <h2><span class="tag">removeFriendship</span> Unfriend</h2>
            <p class="hint">Removes the undirected edge between two users.</p>
            <form method="post" action="${ctx}/social-network">
                <input type="hidden" name="action" value="unfriend">
                <div class="row">
                    <div>
                        <label>User A</label>
                        <select name="userId1" required>
                            <option value="" disabled selected>Select…</option>
                            <c:forEach var="u" items="${users}">
                                <option value="${u.id}">[${u.id}] <c:out value="${u.fullName}" /></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div>
                        <label>User B</label>
                        <select name="userId2" required>
                            <option value="" disabled selected>Select…</option>
                            <c:forEach var="u" items="${users}">
                                <option value="${u.id}">[${u.id}] <c:out value="${u.fullName}" /></option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <button class="btn secondary" type="submit">Unfriend</button>
            </form>
        </div>
    </div>

    <div class="grid cols-2" style="margin-top:20px;">
        <!-- Users table -->
        <div class="card">
            <h2>Users <span class="tag">vertices</span></h2>
            <p class="hint">All registered users, ordered by insertion.</p>
            <table>
                <thead><tr><th>ID</th><th>Full name</th><th>Degree</th></tr></thead>
                <tbody>
                    <c:choose>
                        <c:when test="${empty users}">
                            <tr><td colspan="3" class="empty">No users yet — register one above.</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="u" items="${users}">
                                <tr>
                                    <td><span class="id-pill">${u.id}</span></td>
                                    <td><c:out value="${u.fullName}" /></td>
                                    <td>${relationships[u.id].size()}</td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>

        <!-- Adjacency list -->
        <div class="card">
            <h2>Adjacency list <span class="tag">Graph</span></h2>
            <p class="hint">The raw representation behind every algorithm: each user mapped to its
               neighbour list.</p>
            <div class="adj">
                <c:choose>
                    <c:when test="${empty users}">
                        <span class="none">Graph is empty.</span>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="u" items="${users}">
                            <div>
                                <span class="node">[${u.id}]&nbsp;<c:out value="${u.fullName}" /></span>
                                <span class="arrow">→</span>
                                <c:choose>
                                    <c:when test="${empty relationships[u.id]}">
                                        <span class="none">∅</span>
                                    </c:when>
                                    <c:otherwise>
                                        <c:forEach var="fid" items="${relationships[u.id]}" varStatus="st">
                                            <span class="nb">[${fid}]</span><c:if test="${not st.last}"><span class="arrow">·</span></c:if>
                                        </c:forEach>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<footer class="app">BookFace Admin &middot; MVC-2 single controller &middot; Tomcat 9 / Java EE 7</footer>
</body>
</html>
