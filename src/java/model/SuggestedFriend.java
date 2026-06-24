package model;

public class SuggestedFriend implements Comparable<SuggestedFriend> {
    private final int suggestedUserId;
    private final int mutualFriendsCount;

    public SuggestedFriend(int id, int count) {
        this.suggestedUserId = id;
        this.mutualFriendsCount = count;
    }

    public int getSuggestedId() {
        return suggestedUserId;
    }

    public int getMutualCount() {
        return mutualFriendsCount;
    }

    @Override
    public int compareTo(SuggestedFriend other) {
        int countCompare = Integer.compare(other.mutualFriendsCount, this.mutualFriendsCount);
        if (countCompare != 0) {
            return countCompare;
        }
        return Integer.compare(this.suggestedUserId, other.suggestedUserId);
    }
}
