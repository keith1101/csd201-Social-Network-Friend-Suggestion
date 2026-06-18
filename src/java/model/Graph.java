package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import model.User;

public class Graph {
    private final ArrayList<User> vertices;
    private final ArrayList<ArrayList<Integer>> adjList;

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
        adjList.add(new ArrayList<>());
    }

    public boolean removeUser(int userId) {
        int userIndex = getIndexById(userId);
        if (userIndex == -1) {
            return false;
        }

        for (ArrayList<Integer> friends : adjList) {
            friends.remove(Integer.valueOf(userId));
        }

        vertices.remove(userIndex);
        adjList.remove(userIndex);
        return true;
    }

    public User searchUserById(int userId) {
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

        ArrayList<Integer> userFriends1 = adjList.get(userIndex1);
        ArrayList<Integer> userFriends2 = adjList.get(userIndex2);

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

    public ArrayList<User> getMutualFriends(int userId1, int userId2) {
        ArrayList<User> mutualFriends = new ArrayList<>();
        ArrayList<Integer> list1 = getFriendsOf(userId1);
        ArrayList<Integer> list2 = getFriendsOf(userId2);

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
        int V = vertices.size();
        boolean[] visited = new boolean[V];
        ArrayList<Integer> result = new ArrayList<>();

        int src = startUserId;
        Queue<Integer> q = new LinkedList<>();
        visited[src] = true;
        q.add(src);

        while (!q.isEmpty()) {
            int curr = q.poll();
            result.add(curr);

            for (int x : adjList.get(curr)) {
                if (!visited[x]) {
                    visited[x] = true;
                    q.add(x);
                }
            }
        }

        for (int item : result) {
            System.out.print(item);
        }
        
    }

    public ArrayList<Integer> getFriendsOf(int userId) {
        int index = getIndexById(userId);
        if (index != -1) {
            return adjList.get(index);
        }
        return new ArrayList<>();
    }

    public ArrayList<User> getVertices() {
        return vertices;
    }
}
