package model;

public class SuggestedFriend implements Comparable<SuggestedFriend> {
    private int userId;
    private int mutualFriendsCount;

    public SuggestedFriend(int userId, int mutualFriendsCount) {
        this.userId = userId;
        this.mutualFriendsCount = mutualFriendsCount;
    }

    public int getUserId() { return userId; }
    public int getMutualFriendsCount() { return mutualFriendsCount; }

    // Ghi đè hàm so sánh để sắp xếp giảm dần theo số lượng bạn chung
    @Override
    public int compareTo(SuggestedFriend other) {
        return Integer.compare(other.mutualFriendsCount, this.mutualFriendsCount);
    }
}
