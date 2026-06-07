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
        // TODO: Future implementation for removeUser
        return false;
    }

    public User searchUserById(int userId) {
        // TODO: Future implementation for searchUserById
        int index = getIndexById(userId);
        if (index != -1) {
            return vertices.get(index);
        }
        return null;
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
        // TODO: Future implementation for removeFriendship
        return false;
    }

    public LinkedList<User> getMutualFriends(int userId1, int userId2) {
        // TODO: Future implementation for getMutualFriends
        LinkedList<User> mutualFriends = new LinkedList<>();
        LinkedList<Integer> list1 = getFriendsOf(userId1);
        LinkedList<Integer> list2 = getFriendsOf(userId2);

        java.util.Iterator<Integer> it1 = list1.iterator();
        java.util.Iterator<Integer> it2 = list2.iterator();

        if (!it1.hasNext() || !it2.hasNext()) {
            return mutualFriends;
        }

        Integer id1 = it1.next();
        Integer id2 = it2.next();

        while (true) {
            if (id1.equals(id2)) {
                User user = searchUserById(id1);
                if (user != null) {
                    mutualFriends.add(user);
                }
                if (!it1.hasNext() || !it2.hasNext()) {
                    break;
                }
                id1 = it1.next();
                id2 = it2.next();
            } else if (id1 < id2) {
                if (!it1.hasNext()) {
                    break;
                }
                id1 = it1.next();
            } else {
                if (!it2.hasNext()) {
                    break;
                }
                id2 = it2.next();
            }
        }
        return mutualFriends;
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
