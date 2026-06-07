package model;

import java.util.ArrayList;
import java.util.LinkedList;
import model.User;

public class Graph {
    private final ArrayList<User> vertices;
    private final ArrayList<LinkedList<Integer>> adjList;

    public Graph() {
        this.vertices = new ArrayList<>();
        this.adjList = new ArrayList<>();
    }

    private int getIndexById(int userId) {
        for (int i = 0; i < vertices.size(); i++) {
            if (vertices.get(i).getId() == userId) {
                return i;
            }
        }
        return -1;
    }

    public void addUser(User user) {
        vertices.add(user);
        adjList.add(new LinkedList<>());
    }

    public boolean removeUser(int userId) {
        int userIndex = getIndexById(userId);
        if (userIndex == -1) {
            return false;
        }

        for (LinkedList<Integer> friends : adjList) {
            friends.remove(Integer.valueOf(userId));
        }

        vertices.remove(userIndex);
        adjList.remove(userIndex);
        return true;
    }

    public User searchUserById(int userId) {
        int index = getIndexById(userId);
        if (index == -1) {
            return null;
        }
        return vertices.get(index);
    }

    public boolean addFriendship(int userId1, int userId2) {
        int userIndex1 = getIndexById(userId1);
        int userIndex2 = getIndexById(userId2);

        if (userIndex1 == -1 || userIndex2 == -1) {
            return false;
        }

        LinkedList<Integer> userFriends1 = adjList.get(userIndex1);
        LinkedList<Integer> userFriends2 = adjList.get(userIndex2);

        if (userFriends1.contains(userId2)) {
            return false;
        }

        userFriends1.add(userId2);
        userFriends2.add(userId1);
        return true;
    }

    public boolean removeFriendship(int userId1, int userId2) {
        int userIndex1 = getIndexById(userId1);
        int userIndex2 = getIndexById(userId2);

        if (userIndex1 == -1 || userIndex2 == -1) {
            return false;
        }

        boolean removedFromFirst = adjList.get(userIndex1).remove(Integer.valueOf(userId2));
        boolean removedFromSecond = adjList.get(userIndex2).remove(Integer.valueOf(userId1));
        return removedFromFirst && removedFromSecond;
    }

    public LinkedList<User> getMutualFriends(int userId1, int userId2) {
        // TODO: Future implementation for getMutualFriends
        return new LinkedList<>();
    }

    public ArrayList<SuggestedFriend> suggestFriends(int userId) {
        // TODO: Future implementation for suggestFriends
        return new ArrayList<>();
    }

    public void bfsTraverse(int startUserId) {
        // TODO: Future implementation for bfsTraverse
    }

    public LinkedList<Integer> getFriendsOf(int userId) {
        int index = getIndexById(userId);
        if (index != -1) {
            return adjList.get(index);
        }
        return new LinkedList<>();
    }

    public ArrayList<User> getVertices() {
        return vertices;
    }
}
