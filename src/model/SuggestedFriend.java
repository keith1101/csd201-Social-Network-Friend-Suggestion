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

    // Override the comparison method to sort in descending order by mutual friend count
    @Override
    public int compareTo(SuggestedFriend other) {
        return Integer.compare(other.mutualFriendsCount, this.mutualFriendsCount);
    }
}
