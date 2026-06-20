<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Entry point: hand off to the front controller's default (User) view. --%>
<% response.sendRedirect(request.getContextPath() + "/social-network?action=user"); %>
