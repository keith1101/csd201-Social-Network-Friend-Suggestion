package controller;

import java.util.ArrayList;
import model.SocialGraphDAO;
import model.Graph;
import model.SuggestedFriend;
import model.User;
import view.ConsoleView;

public class SocialNetworkController {
    private Graph friendGraph;
    private SocialGraphDAO socialGraphDAO;

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

    public void registerUser(User user) {
        friendGraph.addUser(user);
        ConsoleView.displayMessage("Registered: " + user.getFullName());
    }

    public void makeFriend(int id1, int id2) {
        boolean success = friendGraph.addFriendship(id1, id2);
        if (success) {
            ConsoleView.displayMessage("Friendship created successfully between " + id1 + " and " + id2);
        } else {
            ConsoleView.displayMessage("Friendship creation failed (invalid ID or already friends).");
        }
    }

    public void unFriend(int id1, int id2) {
        // chua lam
    }

    public ArrayList<SuggestedFriend> suggestMutualFriends(int userId) {
        // chua lam
        return new ArrayList<>();
    }
}
