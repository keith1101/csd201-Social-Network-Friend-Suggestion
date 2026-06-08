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
        // TODO: Future implementation for suggestMutualFriends
        ArrayList<SuggestedFriend> result = new ArrayList<>();
        ArrayList<User> allUsers = friendGraph.getVertices();
        java.util.LinkedList<Integer> directFriends = friendGraph.getFriendsOf(userId);

        model.MyMaxHeap heap = new model.MyMaxHeap(allUsers.size());

        for (User candidate : allUsers) {
            int candidateId = candidate.getId();
            if (candidateId == userId || directFriends.contains(candidateId)) {
                continue;
            }
            
            int mutualCount = friendGraph.getMutualFriends(userId, candidateId).size();
            
            if (mutualCount > 0) {
                heap.insert(new SuggestedFriend(candidateId, mutualCount));
            }
        }

        int topK = 5;
        while (!heap.isEmpty() && topK > 0) {
            result.add(heap.extractMax());
            topK--;
        }

        return result;
    }
}
