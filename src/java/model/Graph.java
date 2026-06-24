package model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class Graph {
    private final LinkedHashMap<Integer, User> usersById;
    private final LinkedHashMap<Integer, LinkedHashSet<Integer>> friendsById;

    public Graph() {
        this.usersById = new LinkedHashMap<>();
        this.friendsById = new LinkedHashMap<>();
    }

    public boolean addUser(User user) {
        if (user == null || usersById.containsKey(user.getId())) {
            return false;
        }

        usersById.put(user.getId(), user);
        friendsById.put(user.getId(), new LinkedHashSet<>());
        return true;
    }

    public boolean removeUser(int userId) {
        User removedUser = usersById.remove(userId);
        if (removedUser == null) {
            return false;
        }

        LinkedHashSet<Integer> removedFriends = friendsById.remove(userId);
        if (removedFriends != null) {
            for (int friendId : removedFriends) {
                LinkedHashSet<Integer> friendSet = friendsById.get(friendId);
                if (friendSet != null) {
                    friendSet.remove(userId);
                }
            }
        }

        return true;
    }

    public User searchUserById(int userId) {
        return usersById.get(userId);
    }

    public boolean isFriendshipExists(int userId1, int userId2) {
        LinkedHashSet<Integer> friends = friendsById.get(userId1);
        return friends != null && friends.contains(userId2);
    }

    public boolean addFriendship(int userId1, int userId2) {
        if (userId1 == userId2 || !usersById.containsKey(userId1) || !usersById.containsKey(userId2)) {
            return false;
        }

        LinkedHashSet<Integer> friends1 = friendsById.computeIfAbsent(userId1, key -> new LinkedHashSet<>());
        LinkedHashSet<Integer> friends2 = friendsById.computeIfAbsent(userId2, key -> new LinkedHashSet<>());

        if (!friends1.add(userId2)) {
            return false;
        }

        if (!friends2.add(userId1)) {
            friends1.remove(userId2);
            return false;
        }

        return true;
    }

    public boolean removeFriendship(int userId1, int userId2) {
        if (userId1 == userId2) {
            return false;
        }

        LinkedHashSet<Integer> friends1 = friendsById.get(userId1);
        LinkedHashSet<Integer> friends2 = friendsById.get(userId2);
        if (friends1 == null || friends2 == null) {
            return false;
        }

        boolean removedFromFirst = friends1.remove(userId2);
        boolean removedFromSecond = friends2.remove(userId1);

        if (removedFromFirst != removedFromSecond) {
            if (removedFromFirst) {
                friends1.add(userId2);
            }
            if (removedFromSecond) {
                friends2.add(userId1);
            }
            return false;
        }

        return removedFromFirst;
    }

    public ArrayList<User> getMutualFriends(int userId1, int userId2) {
        ArrayList<User> mutualFriends = new ArrayList<>();

        Set<Integer> firstFriends = friendsById.get(userId1);
        if (firstFriends == null || firstFriends.isEmpty()) {
            return mutualFriends;
        }

        LinkedHashSet<Integer> secondFriends = friendsById.get(userId2);
        if (secondFriends == null || secondFriends.isEmpty()) {
            return mutualFriends;
        }

        for (int friendId : secondFriends) {
            if (firstFriends.contains(friendId)) {
                User mutual = usersById.get(friendId);
                if (mutual != null) {
                    mutualFriends.add(mutual);
                }
            }
        }

        return mutualFriends;
    }

    public ArrayList<SuggestedFriend> suggestFriends(int userId, int topK) {
        ArrayList<SuggestedFriend> result = new ArrayList<>();
        if (!usersById.containsKey(userId) || topK <= 0) {
            return result;
        }

        LinkedHashSet<Integer> directFriends = friendsById.get(userId);
        if (directFriends == null || directFriends.isEmpty()) {
            return result;
        }

        HashMap<Integer, Integer> mutualCounts = new HashMap<>();
        for (int friendId : directFriends) {
            LinkedHashSet<Integer> friendsOfFriend = friendsById.get(friendId);
            if (friendsOfFriend == null || friendsOfFriend.isEmpty()) {
                continue;
            }

            for (int candidateId : friendsOfFriend) {
                if (candidateId != userId && !directFriends.contains(candidateId)) {
                    mutualCounts.merge(candidateId, 1, Integer::sum);
                }
            }
        }

        PriorityQueue<SuggestedFriend> heap = new PriorityQueue<>();
        for (Map.Entry<Integer, Integer> entry : mutualCounts.entrySet()) {
            if (entry.getValue() > 0) {
                heap.add(new SuggestedFriend(entry.getKey(), entry.getValue()));
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
        if (!usersById.containsKey(startUserId)) {
            return result;
        }

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new ArrayDeque<>();
        visited.add(startUserId);
        queue.add(startUserId);

        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            result.add(currentId);

            LinkedHashSet<Integer> friends = friendsById.get(currentId);
            if (friends == null || friends.isEmpty()) {
                continue;
            }

            for (int friendId : friends) {
                if (visited.add(friendId)) {
                    queue.add(friendId);
                }
            }
        }

        return result;
    }

    public ArrayList<Integer> getFriendsOf(int userId) {
        LinkedHashSet<Integer> friends = friendsById.get(userId);
        if (friends == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(friends);
    }

    public Map<Integer, ArrayList<Integer>> getRelationshipGraph() {
        Map<Integer, ArrayList<Integer>> relationships = new LinkedHashMap<>();

        for (Map.Entry<Integer, User> entry : usersById.entrySet()) {
            LinkedHashSet<Integer> friends = friendsById.get(entry.getKey());
            relationships.put(
                    entry.getKey(),
                    friends == null ? new ArrayList<>() : new ArrayList<>(friends));
        }

        return relationships;
    }

    public ArrayList<User> getVertices() {
        return new ArrayList<>(usersById.values());
    }

    public ArrayList<User> getUsersPage(int page, int pageSize) {
        ArrayList<User> pageUsers = new ArrayList<>();
        if (pageSize <= 0 || usersById.isEmpty()) {
            return pageUsers;
        }

        int safePage = Math.max(1, page);
        int startIndex = (safePage - 1) * pageSize;
        int endIndex = startIndex + pageSize;

        int index = 0;
        for (User user : usersById.values()) {
            if (index >= endIndex) {
                break;
            }
            if (index >= startIndex) {
                pageUsers.add(user);
            }
            index++;
        }

        return pageUsers;
    }

    public int getUserPageCount(int pageSize) {
        if (pageSize <= 0 || usersById.isEmpty()) {
            return 0;
        }

        return (usersById.size() + pageSize - 1) / pageSize;
    }

    public int getUserCount() {
        return usersById.size();
    }
}