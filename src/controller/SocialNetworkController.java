package controller;

import model.SocialGraph;
import model.SuggestedFriend;
import model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class SocialNetworkController {
    private SocialGraph friendGraph;

    public SocialNetworkController() {
        this.friendGraph = new SocialGraph();
    }

    // Register a user
    public void registerUser(int id, String fullName) {
        User newUser = new User(id, fullName);
        friendGraph.addUser(newUser);
        System.out.println("Registered: " + fullName);
    }

    // Create a friendship
    public void makeFriend(int uId, int vId) {
        boolean success = friendGraph.addEdge(uId, vId);
        if (success) {
            System.out.println("Friendship created successfully between " + uId + " and " + vId);
        } else {
            System.out.println("Friendship creation failed (invalid ID or already friends).");
        }
    }

    // Suggest mutual friends (core algorithm)
    public ArrayList<SuggestedFriend> suggestMutualFriends(int targetUserId) {
        LinkedList<Integer> myFriends = friendGraph.getFriendsOf(targetUserId);
        ArrayList<SuggestedFriend> suggestions = new ArrayList<>();

        // Find mutual friends by scanning every user in the graph
        for (User u : friendGraph.getVertices()) {
            int currentId = u.getId();

            // Skip the target user and users who are already friends
            if (currentId == targetUserId || myFriends.contains(currentId)) {
                continue;
            }

            LinkedList<Integer> theirFriends = friendGraph.getFriendsOf(currentId);
            int mutualCount = 0;

            // Count mutual friends (intersection of two sets)
            for (int fId : myFriends) {
                if (theirFriends.contains(fId)) {
                    mutualCount++;
                }
            }

            // Only suggest users with at least one mutual friend
            if (mutualCount > 0) {
                suggestions.add(new SuggestedFriend(currentId, mutualCount));
            }
        }

        // Use Comparable to sort in O(N log N)
        Collections.sort(suggestions);
        return suggestions;
    }
}
