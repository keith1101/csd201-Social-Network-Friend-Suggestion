package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import model.Friendship;
import model.Graph;
import model.SocialGraphDAO;
import model.SuggestedFriend;
import model.User;
import utils.TextUtils;
import utils.Validator;

/**
 * Front controller for the servlet UI.
 *
 * <p>
 * Users are cached in memory for fast pickers and list views. Friendship-heavy
 * pages are queried on demand from SQL Server so startup does not preload the
 * entire edge set into RAM.
 * </p>
 */
@WebServlet(name = "MainController", urlPatterns = { "/social-network" })
public class MainController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    private static final int DEFAULT_TOP_K = 5;
    private static final int DEFAULT_ADMIN_PAGE_SIZE = 25;
    private static final int MAX_ADMIN_PAGE_SIZE = 100;

    private SocialGraphDAO dao;
    private Graph graph;

    private final Object graphLock = new Object();

    @Override
    public void init() throws ServletException {
        this.dao = new SocialGraphDAO();

        Graph loaded = dao.loadGraphFromDatabase();
        this.graph = (loaded != null) ? loaded : new Graph();
        LOGGER.log(Level.INFO, "MainController initialised with {0} users.",
                graph.getUserCount());
    }

    // ------------------------------------------------------------------
    // GET - read-only routing (admin / user)
    // ------------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            action = "user";
        }

        moveFlashToRequest(request);

        switch (action) {
            case "admin":
                populateGraphSnapshot(request);
                forward(request, response, "admin");
                break;

            case "dashboard":
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Dashboard is disabled.");
                break;
            case "user":
                populateUsersSnapshot(request);
                populateSelectedUser(request, request.getParameter("userId"), true);
                forward(request, response, "user");
                break;

            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    // POST - mutations (Post-Redirect-Get)
    // ------------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing action.");
            return;
        }

        switch (action) {
            case "register":
                registerUser(request);
                break;
            case "removeUser":
                removeUser(request);
                break;
            case "makeFriend":
                makeFriend(request);
                break;
            case "unfriend":
                unfriend(request);
                break;
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Unknown action: " + action);
                return;
        }

        String returnUserId = request.getParameter("returnUserId");
        String target = Validator.isValidId(returnUserId)
                ? "/social-network?action=user&userId=" + returnUserId.trim()
                : "/social-network?action=admin";
        response.sendRedirect(request.getContextPath() + target);
    }

    // ==================================================================
    // Mutation handlers - DB write first, then refresh the cache if needed.
    // ==================================================================

    private void registerUser(HttpServletRequest request) {
        String rawId = request.getParameter("userId");
        String rawName = request.getParameter("fullName");

        if (!Validator.isValidId(rawId)) {
            flashError(request, "Invalid user ID. Enter a positive integer.");
            return;
        }

        String name = TextUtils.normalizeName(rawName);
        if (!Validator.isValidName(name)) {
            flashError(request, "Invalid name. Use letters, spaces, apostrophes, or hyphens.");
            return;
        }

        int userId = Integer.parseInt(rawId.trim());

        synchronized (graphLock) {
            if (dao.isUserExists(userId)) {
                flashError(request, "User ID " + userId + " already exists.");
                return;
            }

            User user = new User(userId, name);
            if (!dao.insertUser(user)) {
                flashError(request, "Failed to register user in the database.");
                return;
            }

            if (!graph.addUser(user)) {
                reloadGraph();
                flashMessage(request, "User registered in the database; graph cache refreshed.");
                return;
            }

            flashMessage(request, "Registered user [" + userId + "] " + name + ".");
        }
    }

    private void removeUser(HttpServletRequest request) {
        String rawId = request.getParameter("userId");
        if (!Validator.isValidId(rawId)) {
            flashError(request, "Invalid user ID.");
            return;
        }

        int userId = Integer.parseInt(rawId.trim());

        synchronized (graphLock) {
            if (!dao.isUserExists(userId)) {
                flashError(request, "User [" + userId + "] does not exist.");
                return;
            }

            if (!dao.deleteUser(userId)) {
                flashError(request, "Failed to remove user from the database.");
                return;
            }

            if (!graph.removeUser(userId)) {
                reloadGraph();
                flashMessage(request, "User removed from the database; graph cache refreshed.");
                return;
            }

            flashMessage(request, "Removed user [" + userId + "] and their friendships.");
        }
    }

    private void makeFriend(HttpServletRequest request) {
        String rawId1 = request.getParameter("userId1");
        String rawId2 = request.getParameter("userId2");

        if (!Validator.isValidFriendship(rawId1, rawId2)) {
            flashError(request, "Invalid friendship. IDs must be positive and different.");
            return;
        }

        int id1 = Integer.parseInt(rawId1.trim());
        int id2 = Integer.parseInt(rawId2.trim());

        synchronized (graphLock) {
            if (!dao.isUserExists(id1) || !dao.isUserExists(id2)) {
                flashError(request, "Both users must exist before creating a friendship.");
                return;
            }

            if (dao.isFriendshipExists(id1, id2)) {
                flashError(request, "Users [" + id1 + "] and [" + id2 + "] are already friends.");
                return;
            }

            if (!dao.insertFriendship(new Friendship(id1, id2))) {
                flashError(request, "Failed to save friendship in the database.");
                return;
            }

            if (!graph.addFriendship(id1, id2)) {
                reloadGraph();
                flashMessage(request, "Friendship saved in the database; graph cache refreshed.");
                return;
            }

            flashMessage(request, "Connected [" + id1 + "] and [" + id2 + "].");
        }
    }

    private void unfriend(HttpServletRequest request) {
        String rawId1 = request.getParameter("userId1");
        String rawId2 = request.getParameter("userId2");

        if (!Validator.isValidFriendship(rawId1, rawId2)) {
            flashError(request, "Invalid friendship. IDs must be positive and different.");
            return;
        }

        int id1 = Integer.parseInt(rawId1.trim());
        int id2 = Integer.parseInt(rawId2.trim());

        synchronized (graphLock) {
            if (!dao.isFriendshipExists(id1, id2)) {
                flashError(request, "Friendship between [" + id1 + "] and [" + id2 + "] does not exist.");
                return;
            }

            if (!dao.deleteFriendship(id1, id2)) {
                flashError(request, "Failed to remove friendship from the database.");
                return;
            }

            if (!graph.removeFriendship(id1, id2)) {
                reloadGraph();
                flashMessage(request, "Friendship removed from the database; graph cache refreshed.");
                return;
            }

            flashMessage(request, "Disconnected [" + id1 + "] and [" + id2 + "].");
        }
    }

    // ==================================================================
    // Data-contract builders - request attributes, no JSON.
    // ==================================================================

    private void populateUsersSnapshot(HttpServletRequest request) {
        synchronized (graphLock) {
            request.setAttribute("users", graph.getVertices());
        }
    }

    /**
     * Sets a paginated user browser and page-scoped friendships for admin views.
     */
    private void populateGraphSnapshot(HttpServletRequest request) {
        int page = parsePositiveInt(request.getParameter("page"), 1);
        int pageSize = parsePositiveInt(request.getParameter("pageSize"), DEFAULT_ADMIN_PAGE_SIZE);
        pageSize = Math.max(1, Math.min(pageSize, MAX_ADMIN_PAGE_SIZE));

        ArrayList<User> pageUsers;
        int totalUsers;
        int totalPages;
        synchronized (graphLock) {
            totalUsers = graph.getUserCount();
            totalPages = Math.max(1, graph.getUserPageCount(pageSize));
            if (page > totalPages) {
                page = totalPages;
            }
            pageUsers = graph.getUsersPage(page, pageSize);
        }

        ArrayList<Integer> pageUserIds = new ArrayList<>(pageUsers.size());
        for (User user : pageUsers) {
            pageUserIds.add(user.getId());
        }

        Map<Integer, ArrayList<Integer>> pageRelationships = dao.loadRelationshipGraphForUsers(pageUserIds);
        ArrayList<User> pickerUsers;
        synchronized (graphLock) {
            pickerUsers = buildPickerUsers(pageUsers, pageRelationships);
        }

        int totalFriendships = dao.countFriendships();

        request.setAttribute("pageUsers", pageUsers);
        request.setAttribute("users", pickerUsers);
        request.setAttribute("relationships", pageRelationships);
        request.setAttribute("totalUsers", totalUsers);
        request.setAttribute("totalFriendships", totalFriendships);
        request.setAttribute("degreeSum", totalFriendships * 2L);
        request.setAttribute("currentPage", page);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("pageStart", totalUsers == 0 ? 0 : ((page - 1) * pageSize) + 1);
        request.setAttribute("pageEnd", totalUsers == 0 ? 0 : Math.min(page * pageSize, totalUsers));
    }

    /**
     * Populates the selected user context.
     *
     * <p>
     * When {@code includeSelectedRelationships} is true, the request receives a
     * single-entry {@code relationships} map containing only the selected user's
     * direct friends. That is enough for the user page. The admin page
     * call {@link #populateGraphSnapshot(HttpServletRequest)} instead.
     * </p>
     */
    private void populateSelectedUser(
            HttpServletRequest request,
            String rawUserId,
            boolean includeSelectedRelationships) {

        if (!Validator.isValidId(rawUserId)) {
            return;
        }

        int userId = Integer.parseInt(rawUserId.trim());
        if (!dao.isUserExists(userId)) {
            request.setAttribute("error", "User [" + userId + "] does not exist.");
            return;
        }

        SocialGraphDAO.SuggestionBundle suggestionBundle = dao.loadSuggestionBundle(userId, DEFAULT_TOP_K);
        if (includeSelectedRelationships) {
            Map<Integer, ArrayList<Integer>> selectedRelationships = new LinkedHashMap<>();
            selectedRelationships.put(userId, suggestionBundle.getDirectFriendIds());
            request.setAttribute("relationships", selectedRelationships);
        }

        request.setAttribute("selectedUserId", userId);
        request.setAttribute("suggestions", suggestionBundle.getSuggestions());
        request.setAttribute("mutualsBySuggested", suggestionBundle.getMutualsBySuggested());
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private int parsePositiveInt(String rawValue, int defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(trimmed);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private ArrayList<User> buildPickerUsers(
            ArrayList<User> pageUsers,
            Map<Integer, ArrayList<Integer>> pageRelationships) {

        LinkedHashMap<Integer, User> pickerUsers = new LinkedHashMap<>();
        if (pageUsers != null) {
            for (User user : pageUsers) {
                if (user != null) {
                    pickerUsers.put(user.getId(), user);
                }
            }
        }

        if (pageRelationships != null) {
            for (ArrayList<Integer> friendIds : pageRelationships.values()) {
                if (friendIds == null) {
                    continue;
                }

                for (int friendId : friendIds) {
                    if (pickerUsers.containsKey(friendId)) {
                        continue;
                    }

                    User friend = graph.searchUserById(friendId);
                    if (friend != null) {
                        pickerUsers.put(friendId, friend);
                    }
                }
            }
        }

        return new ArrayList<>(pickerUsers.values());
    }

    private void reloadGraph() {
        Graph reloaded = dao.loadGraphFromDatabase();
        this.graph = (reloaded != null) ? reloaded : new Graph();
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    private void moveFlashToRequest(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Object message = session.getAttribute("flashMessage");
        Object error = session.getAttribute("flashError");
        if (message != null) {
            request.setAttribute("message", message);
            session.removeAttribute("flashMessage");
        }
        if (error != null) {
            request.setAttribute("error", error);
            session.removeAttribute("flashError");
        }
    }

    private void flashMessage(HttpServletRequest request, String text) {
        request.getSession().setAttribute("flashMessage", text);
    }

    private void flashError(HttpServletRequest request, String text) {
        request.getSession().setAttribute("flashError", text);
    }
}
