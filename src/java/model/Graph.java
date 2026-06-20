package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

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

    public boolean addUser(User user) {
        if (user == null || getIndexById(user.getId()) != -1) {
            return false;
        }

        vertices.add(user);
        adjList.add(new ArrayList<>());
        return true;
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
        return index == -1 ? null : vertices.get(index);
    }

    public boolean isFriendshipExists(int userId1, int userId2) {
        int userIndex1 = getIndexById(userId1);
        int userIndex2 = getIndexById(userId2);

        if (userIndex1 == -1 || userIndex2 == -1) {
            return false;
        }

        return adjList.get(userIndex1).contains(userId2);
    }

    public boolean addFriendship(int userId1, int userId2) {
        int userIndex1 = getIndexById(userId1);
        int userIndex2 = getIndexById(userId2);

        if (userIndex1 == -1 || userIndex2 == -1 || userId1 == userId2) {
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

        boolean removedFromFirst =
                adjList.get(userIndex1).remove(Integer.valueOf(userId2));
        boolean removedFromSecond =
                adjList.get(userIndex2).remove(Integer.valueOf(userId1));
        return removedFromFirst && removedFromSecond;
    }

    public ArrayList<User> getMutualFriends(int userId1, int userId2) {
        ArrayList<User> mutualFriends = new ArrayList<>();
        HashSet<Integer> firstFriends = new HashSet<>(getFriendsOf(userId1));

        for (int friendId : getFriendsOf(userId2)) {
            if (firstFriends.contains(friendId)) {
                User mutual = searchUserById(friendId);
                if (mutual != null) {
                    mutualFriends.add(mutual);
                }
            }
        }

        return mutualFriends;
    }

    public ArrayList<SuggestedFriend> suggestFriends(int userId, int topK) {
        ArrayList<SuggestedFriend> result = new ArrayList<>();
        if (searchUserById(userId) == null || topK <= 0) {
            return result;
        }

        ArrayList<Integer> directFriends = getFriendsOf(userId);
        PriorityQueue<SuggestedFriend> heap = new PriorityQueue<>();

        for (User candidate : vertices) {
            int candidateId = candidate.getId();
            if (candidateId == userId || directFriends.contains(candidateId)) {
                continue;
            }

            int mutualCount = getMutualFriends(userId, candidateId).size();
            if (mutualCount > 0) {
                heap.add(new SuggestedFriend(candidateId, mutualCount));
            }
        }

        while (!heap.isEmpty() && result.size() < topK) {
            result.add(heap.poll());
        }

        return result;
    }

    public ArrayList<SuggestedFriend> suggestedFriends(int userId, int topK) {
        return suggestFriends(userId, topK);
    }

    public ArrayList<Integer> bfsTraverse(int startUserId) {
        ArrayList<Integer> result = new ArrayList<>();
        int sourceIndex = getIndexById(startUserId);
        if (sourceIndex == -1) {
            return result;
        }

        boolean[] visited = new boolean[vertices.size()];
        Queue<Integer> queue = new LinkedList<>();
        visited[sourceIndex] = true;
        queue.add(startUserId);

        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            result.add(currentId);

            int currentIndex = getIndexById(currentId);
            for (int friendId : adjList.get(currentIndex)) {
                int friendIndex = getIndexById(friendId);
                if (friendIndex != -1 && !visited[friendIndex]) {
                    visited[friendIndex] = true;
                    queue.add(friendId);
                }
            }
        }

        return result;
    }

    public ArrayList<Integer> getFriendsOf(int userId) {
        int index = getIndexById(userId);
        if (index == -1) {
            return new ArrayList<>();
        }
        return adjList.get(index);
    }


    public Map<Integer, ArrayList<Integer>> getRelationshipGraph() {
        Map<Integer, ArrayList<Integer>> relationships = new LinkedHashMap<>();

        for (int i = 0; i < vertices.size(); i++) {
            relationships.put(
                    vertices.get(i).getId(),
                    new ArrayList<>(adjList.get(i)));
        }

        return relationships;
    }
    public ArrayList<User> getVertices() {
        return vertices;
    }
}

