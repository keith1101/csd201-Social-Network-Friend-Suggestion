package model;

import java.util.ArrayList;
import java.util.LinkedList;
import utils.DataSynchronizer;

public class SocialGraph {
    // Store the vertex information
    private ArrayList<User> vertices;

    // Adjacency list for friendships (edges)
    // The index in adjList corresponds to the index in vertices
    private ArrayList<LinkedList<Integer>> adjList;

    public SocialGraph() {
        this.vertices = new ArrayList<>();
        this.adjList = new ArrayList<>();
    }

    // Add a new user to the graph
    public void addUser(User user) {
        vertices.add(user);
        adjList.add(new LinkedList<>()); // Initialize an empty friend list
    }

    // Get the index of a user in the vertices list based on ID
    private int getIndexById(int userId) {
        for (int i = 0; i < vertices.size(); i++) {
            if (vertices.get(i).getId() == userId) {
                return i;
            }
        }
        return -1; // Not found
    }

    // Add an undirected edge (friendship)
    public boolean addEdge(int uId, int vId) {
        int uIndex = getIndexById(uId);
        int vIndex = getIndexById(vId);

        if (uIndex == -1 || vIndex == -1) return false; // Error: user does not exist

        // Get the adjacency lists for both users
        LinkedList<Integer> uFriends = adjList.get(uIndex);
        LinkedList<Integer> vFriends = adjList.get(vIndex);

        // Check for duplicates
        if (!uFriends.contains(vId)) {
            uFriends.add(vId);
            vFriends.add(uId);
            return true;
        }
        return false; // Already friends
    }

    // Get the list of friend IDs for one user
    public LinkedList<Integer> getFriendsOf(int userId) {
        int index = getIndexById(userId);
        if (index != -1) return adjList.get(index);
        return new LinkedList<>();
    }

    public ArrayList<User> getVertices() {
        return vertices;
    }
}
