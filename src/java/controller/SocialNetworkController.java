package controller;

import java.util.ArrayList;

import model.Friendship;
import model.Graph;
import model.SocialGraphDAO;
import model.SuggestedFriend;
import model.User;
import utils.TextUtils;
import utils.Validator;
import view.ConsoleView;

public class SocialNetworkController {
    private static final int DEFAULT_TOP_K = 5;

    private Graph friendGraph;
    private final SocialGraphDAO socialGraphDAO;

    public SocialNetworkController() {
        this.socialGraphDAO = new SocialGraphDAO();
    }

    private Graph requireGraph() {
        if (friendGraph == null) {
            initializeGraph();
        }
        return friendGraph;
    }

    public void initializeGraph() {
        this.friendGraph = socialGraphDAO.loadGraphFromDatabase();
        if (friendGraph == null) {
            this.friendGraph = new Graph();
        }
    }

    public void registerUser(String rawId, String rawName) {
        if (!Validator.isValidId(rawId)) {
            ConsoleView.displayMessage("Invalid user ID. Enter a positive integer.");
            return;
        }

        String normalizedName = TextUtils.normalizeName(rawName);
        if (!Validator.isValidName(normalizedName)) {
            ConsoleView.displayMessage(
                    "Invalid user name. Use letters, spaces, apostrophes, or hyphens.");
            return;
        }

        int userId = Integer.parseInt(rawId.trim());
        Graph graph = requireGraph();

        if (socialGraphDAO.isUserExists(userId)) {
            ConsoleView.displayMessage("User ID already exists.");
            return;
        }

        User user = new User(userId, normalizedName);
        if (!socialGraphDAO.insertUser(user)) {
            ConsoleView.displayMessage("Failed to register user in the database.");
            return;
        }

        if (!graph.addUser(user)) {
            initializeGraph();
            ConsoleView.displayMessage("User registered in the database; graph cache refreshed.");
            return;
        }

        ConsoleView.displayMessage("Registered: " + user.getFullName());
    }

    public void registerUser(User user) {
        if (user == null) {
            ConsoleView.displayMessage("User cannot be null.");
            return;
        }

        registerUser(String.valueOf(user.getId()), user.getFullName());
    }

    public void removeUser(String id) {
        if (!Validator.isValidId(id)) {
            ConsoleView.displayMessage("Invalid user ID.");
            return;
        }

        removeUser(new User(Integer.parseInt(id.trim()), null));
    }

    public void removeUser(User user) {
        if (user == null) {
            ConsoleView.displayMessage("User cannot be null.");
            return;
        }

        Graph graph = requireGraph();
        if (!socialGraphDAO.isUserExists(user.getId())) {
            ConsoleView.displayMessage("User [" + user.getId() + "] does not exist.");
            return;
        }

        if (!socialGraphDAO.deleteUser(user.getId())) {
            ConsoleView.displayMessage("Failed to remove user from the database.");
            return;
        }

        if (!graph.removeUser(user.getId())) {
            initializeGraph();
            ConsoleView.displayMessage("User removed from the database; graph cache refreshed.");
            return;
        }

        ConsoleView.displayMessage("Delete user successfully!");
    }

    public void makeFriend(String rawId1, String rawId2) {
        if (!Validator.isValidFriendship(rawId1, rawId2)) {
            ConsoleView.displayMessage(
                    "Invalid friendship. IDs must be positive, different integers.");
            return;
        }

        makeFriend(Integer.parseInt(rawId1.trim()), Integer.parseInt(rawId2.trim()));
    }

    public void makeFriend(int id1, int id2) {
        if (!Validator.isValidFriendship(id1, id2)) {
            ConsoleView.displayMessage("Invalid friendship.");
            return;
        }

        Graph graph = requireGraph();
        if (!socialGraphDAO.isUserExists(id1) || !socialGraphDAO.isUserExists(id2)) {
            ConsoleView.displayMessage("Both users must exist before creating a friendship.");
            return;
        }

        if (socialGraphDAO.isFriendshipExists(id1, id2)) {
            ConsoleView.displayMessage("The users are already friends.");
            return;
        }

        Friendship friendship = new Friendship(id1, id2);
        if (!socialGraphDAO.insertFriendship(friendship)) {
            ConsoleView.displayMessage("Failed to save friendship in the database.");
            return;
        }

        if (!graph.addFriendship(id1, id2)) {
            initializeGraph();
            ConsoleView.displayMessage("Friendship saved in the database; graph cache refreshed.");
            return;
        }

        ConsoleView.displayMessage(
                "Friendship created successfully between " + id1 + " and " + id2 + ".");
    }

    public void unFriend(String rawId1, String rawId2) {
        if (!Validator.isValidFriendship(rawId1, rawId2)) {
            ConsoleView.displayMessage(
                    "Invalid friendship. IDs must be positive, different integers.");
            return;
        }

        unFriend(Integer.parseInt(rawId1.trim()), Integer.parseInt(rawId2.trim()));
    }

    public void unFriend(int id1, int id2) {
        if (!Validator.isValidFriendship(id1, id2)) {
            ConsoleView.displayMessage("Invalid friendship.");
            return;
        }

        Graph graph = requireGraph();
        if (!socialGraphDAO.isFriendshipExists(id1, id2)) {
            ConsoleView.displayMessage("Friendship does not exist.");
            return;
        }

        if (!socialGraphDAO.deleteFriendship(id1, id2)) {
            ConsoleView.displayMessage("Failed to remove friendship from the database.");
            return;
        }

        if (!graph.removeFriendship(id1, id2)) {
            initializeGraph();
            ConsoleView.displayMessage("Friendship removed from the database; graph cache refreshed.");
            return;
        }

        ConsoleView.displayMessage("Friendship removed successfully.");
    }

    public void suggestMutualFriends(String rawUserId) {
        if (!Validator.isValidId(rawUserId)) {
            ConsoleView.displayMessage("Invalid user ID. Enter a positive integer.");
            return;
        }

        suggestMutualFriends(Integer.parseInt(rawUserId.trim()));
    }

    public void suggestMutualFriends(int userId) {
        if (!socialGraphDAO.isUserExists(userId)) {
            ConsoleView.displayMessage("User does not exist.");
            return;
        }

        ArrayList<SuggestedFriend> suggestions =
                socialGraphDAO.loadSuggestedFriends(userId, DEFAULT_TOP_K);
        ConsoleView.displaySuggestedFriends(suggestions);
    }

    public void showUsers() {
        ConsoleView.displayUserList(requireGraph().getVertices());
    }

    public void showRelationshipGraph() {
        ConsoleView.displayRelationshipGraph(
                requireGraph().getVertices(),
                socialGraphDAO.loadRelationshipGraph());
    }
}
