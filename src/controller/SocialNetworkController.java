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

    // Đăng ký user
    public void registerUser(int id, String fullName) {
        User newUser = new User(id, fullName);
        friendGraph.addUser(newUser);
        System.out.println("Đã đăng ký: " + fullName);
    }

    // Kết bạn
    public void makeFriend(int uId, int vId) {
        boolean success = friendGraph.addEdge(uId, vId);
        if (success) {
            System.out.println("Kết bạn thành công giữa " + uId + " và " + vId);
        } else {
            System.out.println("Kết bạn thất bại (sai ID hoặc đã là bạn).");
        }
    }

    // Gợi ý bạn chung (Thuật toán cốt lõi)
    public ArrayList<SuggestedFriend> suggestMutualFriends(int targetUserId) {
        LinkedList<Integer> myFriends = friendGraph.getFriendsOf(targetUserId);
        ArrayList<SuggestedFriend> suggestions = new ArrayList<>();

        // Logic tìm bạn chung: Duyệt qua tất cả user trong đồ thị
        for (User u : friendGraph.getVertices()) {
            int currentId = u.getId();
            
            // Bỏ qua chính mình và những người đã là bạn
            if (currentId == targetUserId || myFriends.contains(currentId)) {
                continue;
            }

            LinkedList<Integer> theirFriends = friendGraph.getFriendsOf(currentId);
            int mutualCount = 0;

            // Đếm số lượng bạn chung (Giao của 2 tập hợp)
            for (int fId : myFriends) {
                if (theirFriends.contains(fId)) {
                    mutualCount++;
                }
            }

            // Chỉ gợi ý nếu có ít nhất 1 bạn chung
            if (mutualCount > 0) {
                suggestions.add(new SuggestedFriend(currentId, mutualCount));
            }
        }

        // Tận dụng Comparable để sắp xếp cực nhanh O(N log N)
        Collections.sort(suggestions);
        return suggestions;
    }
}
