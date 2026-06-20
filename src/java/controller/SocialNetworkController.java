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
        initializeGraph();
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
        if (friendGraph.searchUserById(userId) != null || socialGraphDAO.isUserExists(userId)) {
            ConsoleView.displayMessage("User ID already exists.");
            return;
        }

        User user = new User(userId, normalizedName);
        if (!socialGraphDAO.insertUser(user)) {
            ConsoleView.displayMessage("Failed to register user in the database.");
            return;
        }

        boolean addedToGraph = friendGraph.addUser(user);
        if (!addedToGraph) {
            socialGraphDAO.deleteUser(userId);
            ConsoleView.displayMessage("Failed to add user to the social graph.");
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
        User user = friendGraph.searchUserById(Integer.parseInt(id));
        removeUser(user);
    }

    public void removeUser(User user) {
        if (user == null) {
            ConsoleView.displayMessage("User cannot be null.");
            return;
        }

        boolean removeUserFromGraph = friendGraph.removeUser(user.getId());
        if (removeUserFromGraph) {
            socialGraphDAO.deleteUser(user.getId());
            ConsoleView.displayMessage("Delete user successfully!");
        } else {
            ConsoleView.displayMessage("Cannot delete user!");
        }
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

        if (friendGraph.searchUserById(id1) == null || friendGraph.searchUserById(id2) == null) {
            ConsoleView.displayMessage("Both users must exist before creating a friendship.");
            return;
        }

        if (friendGraph.isFriendshipExists(id1, id2)
                || socialGraphDAO.isFriendshipExists(id1, id2)) {
            ConsoleView.displayMessage("The users are already friends.");
            return;
        }

        Friendship friendship = new Friendship(id1, id2);
        if (!socialGraphDAO.insertFriendship(friendship)) {
            ConsoleView.displayMessage("Failed to save friendship in the database.");
            return;
        }

        boolean addedToGraph = friendGraph.addFriendship(id1, id2);
        if (!addedToGraph) {
            socialGraphDAO.deleteFriendship(id1, id2);
            ConsoleView.displayMessage("Failed to add friendship to the social graph.");
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
        if (!Validator.isValidFriendship(id1, id2)
                || !friendGraph.isFriendshipExists(id1, id2)) {
            ConsoleView.displayMessage("Friendship does not exist.");
            return;
        }

        if (!socialGraphDAO.deleteFriendship(id1, id2)) {
            ConsoleView.displayMessage("Failed to remove friendship from the database.");
            return;
        }

        if (!friendGraph.removeFriendship(id1, id2)) {
            initializeGraph();
            ConsoleView.displayMessage(
                    "Friendship was removed from the database; graph data was reloaded.");
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
        if (friendGraph.searchUserById(userId) == null) {
            ConsoleView.displayMessage("User does not exist.");
            return;
        }

        ArrayList<SuggestedFriend> suggestions =
                friendGraph.suggestFriends(userId, DEFAULT_TOP_K);
        ConsoleView.displaySuggestedFriends(suggestions);
    }

    public void showUsers() {
        ConsoleView.displayUserList(new ArrayList<>(friendGraph.getVertices()));
    }

    public void showRelationshipGraph() {
        ConsoleView.displayRelationshipGraph(
                new ArrayList<>(friendGraph.getVertices()),
                friendGraph.getRelationshipGraph());
    }
}
