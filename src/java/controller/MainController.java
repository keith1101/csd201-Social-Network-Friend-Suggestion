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
 * Front controller (MVC Model-2) for the Social Network Friend-Suggestion web app.
 *
 * <p>A single servlet orchestrates the in-memory {@link Graph} (adjacency-list data
 * structure + BFS / mutual-friend / top-K heap algorithms) and the SQL Server
 * {@link SocialGraphDAO}. There is no JSON/REST layer: {@code doGet} populates request
 * attributes (the "data contract") and forwards to a JSP under {@code /WEB-INF/views};
 * {@code doPost} performs mutations using the Post-Redirect-Get pattern so a browser
 * refresh never repeats a write.</p>
 *
 * <p><b>Consistency rule:</b> every mutation writes to the database first, then mutates
 * the in-memory graph under {@link #graphLock}; if the graph step fails the database
 * change is rolled back (or the graph is reloaded) so the two never diverge.</p>
 */
@WebServlet(name = "MainController", urlPatterns = {"/social-network"})
public class MainController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    /** Number of friend suggestions surfaced per user (top-K of the max-heap). */
    private static final int DEFAULT_TOP_K = 5;

    /** Single shared DAO and graph for the whole application. */
    private SocialGraphDAO dao;
    private Graph graph;

    /** Guards every read-snapshot and mutation of {@link #graph}. */
    private final Object graphLock = new Object();

    @Override
    public void init() throws ServletException {
        this.dao = new SocialGraphDAO();
        Graph loaded = dao.loadGraphFromDatabase();
        this.graph = (loaded != null) ? loaded : new Graph();
        LOGGER.log(Level.INFO, "MainController initialised with {0} users.",
                graph.getVertices().size());
    }

    // ------------------------------------------------------------------
    //  GET — read-only routing (admin / user / dashboard)
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
                populateGraphSnapshot(request);
                populateSelectedUser(request, request.getParameter("userId"));
                forward(request, response, "dashboard");
                break;

            case "user":
                populateGraphSnapshot(request);
                populateSelectedUser(request, request.getParameter("userId"));
                forward(request, response, "user");
                break;

            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Unknown action: " + action);
        }
    }

    // ------------------------------------------------------------------
    //  POST — mutations (Post-Redirect-Get)
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

        // PRG: redirect so a browser refresh never re-submits. Forms on the User page
        // carry returnUserId so the user stays on their own page; Admin forms omit it.
        String returnUserId = request.getParameter("returnUserId");
        String target = Validator.isValidId(returnUserId)
                ? "/social-network?action=user&userId=" + returnUserId.trim()
                : "/social-network?action=admin";
        response.sendRedirect(request.getContextPath() + target);
    }

    // ==================================================================
    //  Mutation handlers — DB write first, then graph under graphLock.
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
            if (graph.searchUserById(userId) != null || dao.isUserExists(userId)) {
                flashError(request, "User ID " + userId + " already exists.");
                return;
            }

            User user = new User(userId, name);
            if (!dao.insertUser(user)) {
                flashError(request, "Failed to register user in the database.");
                return;
            }

            if (!graph.addUser(user)) {
                dao.deleteUser(userId); // roll back the DB write
                flashError(request, "Failed to add user to the social graph.");
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
            if (graph.searchUserById(userId) == null) {
                flashError(request, "User [" + userId + "] does not exist.");
                return;
            }

            // DELETE drops the user and all their friendship rows in one go.
            if (!dao.deleteUser(userId)) {
                flashError(request, "Failed to remove user from the database.");
                return;
            }

            if (!graph.removeUser(userId)) {
                reloadGraph(); // resync graph with the now-authoritative DB
                flashMessage(request, "User removed from the database; graph reloaded.");
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
            if (graph.searchUserById(id1) == null || graph.searchUserById(id2) == null) {
                flashError(request, "Both users must exist before creating a friendship.");
                return;
            }

            if (graph.isFriendshipExists(id1, id2) || dao.isFriendshipExists(id1, id2)) {
                flashError(request, "Users [" + id1 + "] and [" + id2 + "] are already friends.");
                return;
            }

            if (!dao.insertFriendship(new Friendship(id1, id2))) {
                flashError(request, "Failed to save friendship in the database.");
                return;
            }

            if (!graph.addFriendship(id1, id2)) {
                dao.deleteFriendship(id1, id2); // roll back the DB write
                flashError(request, "Failed to add friendship to the social graph.");
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
            if (!graph.isFriendshipExists(id1, id2)) {
                flashError(request, "Friendship between [" + id1 + "] and [" + id2 + "] does not exist.");
                return;
            }

            if (!dao.deleteFriendship(id1, id2)) {
                flashError(request, "Failed to remove friendship from the database.");
                return;
            }

            if (!graph.removeFriendship(id1, id2)) {
                reloadGraph();
                flashMessage(request, "Friendship removed from the database; graph reloaded.");
                return;
            }

            flashMessage(request, "Disconnected [" + id1 + "] and [" + id2 + "].");
        }
    }

    // ==================================================================
    //  Data-contract builders (request attributes — no JSON)
    // ==================================================================

    /**
     * Sets {@code users} and {@code relationships} from a consistent snapshot of the
     * graph. All collections are defensive copies so the JSP can never mutate state.
     */
    private void populateGraphSnapshot(HttpServletRequest request) {
        synchronized (graphLock) {
            request.setAttribute("users", new ArrayList<>(graph.getVertices()));
            request.setAttribute("relationships", graph.getRelationshipGraph());
        }
    }

    /**
     * If {@code rawUserId} identifies a real user, sets {@code selectedUserId},
     * {@code suggestions} (top-K from the max-heap) and {@code mutualsBySuggested}
     * (suggestedId &rarr; mutual-friend IDs) — the data that lets the dashboard show
     * <em>why</em> each suggestion was made.
     */
    private void populateSelectedUser(HttpServletRequest request, String rawUserId) {
        if (!Validator.isValidId(rawUserId)) {
            return;
        }
        int userId = Integer.parseInt(rawUserId.trim());

        synchronized (graphLock) {
            if (graph.searchUserById(userId) == null) {
                request.setAttribute("error", "User [" + userId + "] does not exist.");
                return;
            }

            ArrayList<SuggestedFriend> suggestions = graph.suggestFriends(userId, DEFAULT_TOP_K);

            Map<Integer, ArrayList<Integer>> mutualsBySuggested = new LinkedHashMap<>();
            for (SuggestedFriend suggestion : suggestions) {
                ArrayList<Integer> mutualIds = new ArrayList<>();
                for (User mutual : graph.getMutualFriends(userId, suggestion.getSuggestedId())) {
                    mutualIds.add(mutual.getId());
                }
                mutualsBySuggested.put(suggestion.getSuggestedId(), mutualIds);
            }

            request.setAttribute("selectedUserId", userId);
            request.setAttribute("suggestions", suggestions);
            request.setAttribute("mutualsBySuggested", mutualsBySuggested);
        }
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private void reloadGraph() {
        Graph reloaded = dao.loadGraphFromDatabase();
        this.graph = (reloaded != null) ? reloaded : new Graph();
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(request, response);
    }

    /** Moves a one-shot flash message/error from the session onto the request, then clears it. */
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
