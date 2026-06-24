package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocialGraphDAO {
    private static final Logger LOGGER = Logger.getLogger(SocialGraphDAO.class.getName());

    private static final String DB_NAME = "SocialNetworkFriendSuggestion";
    private static final String USER_NAME = "sa";
    private static final String PASSWORD = "12345";

    private static final String GET_USERS = "SELECT user_id, full_name FROM Users ORDER BY user_id";
    private static final String GET_FRIENDSHIPS =
            "SELECT user_id1, user_id2 FROM Friendships ORDER BY user_id1, user_id2";
    private static final String GET_FRIEND_IDS =
            "SELECT CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END AS friend_id "
                    + "FROM Friendships WHERE user_id1 = ? OR user_id2 = ? ORDER BY friend_id";
    private static final String COUNT_FRIENDSHIPS = "SELECT COUNT(*) AS friendship_count FROM Friendships";
    private static final String GET_SELECTED_USER_SUGGESTION_ROWS =
            "WITH direct_friends AS ("
                    + " SELECT DISTINCT CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END AS friend_id"
                    + " FROM Friendships WHERE user_id1 = ? OR user_id2 = ?"
                    + "), candidate_pairs AS ("
                    + " SELECT f.user_id2 AS candidate_id, d.friend_id AS mutual_id"
                    + " FROM direct_friends d"
                    + " JOIN Friendships f ON f.user_id1 = d.friend_id"
                    + " WHERE f.user_id2 <> ?"
                    + " UNION ALL"
                    + " SELECT f.user_id1 AS candidate_id, d.friend_id AS mutual_id"
                    + " FROM direct_friends d"
                    + " JOIN Friendships f ON f.user_id2 = d.friend_id"
                    + " WHERE f.user_id1 <> ?"
                    + ")"
                    + " SELECT 0 AS row_type, friend_id AS item_id, CAST(NULL AS INT) AS mutual_id"
                    + " FROM direct_friends"
                    + " UNION ALL"
                    + " SELECT 1 AS row_type, candidate_id AS item_id, mutual_id"
                    + " FROM candidate_pairs"
                    + " ORDER BY row_type, item_id, mutual_id";
    private static final String INSERT_USER = "INSERT INTO Users(user_id, full_name) VALUES (?, ?)";
    private static final String UPDATE_USER = "UPDATE Users SET full_name = ? WHERE user_id = ?";
    private static final String DELETE_USER = "DELETE FROM Users WHERE user_id = ?";
    private static final String DELETE_USER_FRIENDSHIPS = "DELETE FROM Friendships WHERE user_id1 = ? OR user_id2 = ?";
    private static final String INSERT_FRIENDSHIP = "INSERT INTO Friendships(user_id1, user_id2) VALUES (?, ?)";
    private static final String DELETE_FRIENDSHIP =
            "DELETE FROM Friendships WHERE (user_id1 = ? AND user_id2 = ?) OR (user_id1 = ? AND user_id2 = ?)";
    private static final String CHECK_USER_EXISTS = "SELECT 1 FROM Users WHERE user_id = ?";
    private static final String CHECK_FRIENDSHIP_EXISTS =
            "SELECT 1 FROM Friendships WHERE (user_id1 = ? AND user_id2 = ?) OR (user_id1 = ? AND user_id2 = ?)";

    private final String connectionString;

    public SocialGraphDAO() {
        this.connectionString = "jdbc:sqlserver://localhost:1433;databaseName=" + DB_NAME;
    }

    public static final class SuggestionBundle {
        private final ArrayList<Integer> directFriendIds;
        private final ArrayList<SuggestedFriend> suggestions;
        private final Map<Integer, ArrayList<Integer>> mutualsBySuggested;

        public SuggestionBundle(
                ArrayList<Integer> directFriendIds,
                ArrayList<SuggestedFriend> suggestions,
                Map<Integer, ArrayList<Integer>> mutualsBySuggested) {

            this.directFriendIds = directFriendIds;
            this.suggestions = suggestions;
            this.mutualsBySuggested = mutualsBySuggested;
        }

        public ArrayList<Integer> getDirectFriendIds() {
            return directFriendIds;
        }

        public ArrayList<SuggestedFriend> getSuggestions() {
            return suggestions;
        }

        public Map<Integer, ArrayList<Integer>> getMutualsBySuggested() {
            return mutualsBySuggested;
        }
    }

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(connectionString, USER_NAME, PASSWORD);
    }

    public Graph loadGraphFromDatabase() {
        Graph graph = new Graph();

        try {
            LOGGER.info("Database connection successful. Loading graph data...");

            int loadedUsers = loadUsers(graph);
            LOGGER.log(Level.INFO, "Loaded {0} users into the in-memory graph.", loadedUsers);
            return graph;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load graph from database", exception);
            return null;
        }
    }

    private int loadUsers(Graph graph) throws ClassNotFoundException, SQLException {
        int loadedUsers = 0;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_USERS);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                graph.addUser(new User(resultSet.getInt("user_id"), resultSet.getString("full_name")));
                loadedUsers++;
                if (loadedUsers % 10000 == 0) {
                    LOGGER.log(Level.INFO, "Loaded {0} user records from the database.", loadedUsers);
                }
            }
        }

        LOGGER.log(Level.INFO, "Finished reading {0} user records from the database.", loadedUsers);
        return loadedUsers;
    }


    public Map<Integer, ArrayList<Integer>> loadRelationshipGraph() {
        Map<Integer, ArrayList<Integer>> relationships = new LinkedHashMap<>();
        int loadedFriendships = 0;

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_FRIENDSHIPS);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                int userId1 = resultSet.getInt("user_id1");
                int userId2 = resultSet.getInt("user_id2");

                relationships.computeIfAbsent(userId1, key -> new ArrayList<>()).add(userId2);
                relationships.computeIfAbsent(userId2, key -> new ArrayList<>()).add(userId1);
                loadedFriendships++;
                if (loadedFriendships % 100000 == 0) {
                    LOGGER.log(Level.INFO, "Loaded {0} friendship rows from the database.", loadedFriendships);
                }
            }

            LOGGER.log(Level.INFO, "Finished reading {0} friendship rows from the database.",
                    loadedFriendships);
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load relationship graph from database", exception);
        }

        return relationships;
    }

    public int countFriendships() {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_FRIENDSHIPS);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt("friendship_count");
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to count friendships", exception);
        }

        return 0;
    }

    public Map<Integer, ArrayList<Integer>> loadRelationshipGraphForUsers(ArrayList<Integer> userIds) {
        Map<Integer, ArrayList<Integer>> relationships = new LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return relationships;
        }

        HashSet<Integer> userSet = new HashSet<>(userIds);
        StringBuilder placeholders = new StringBuilder();
        for (int index = 0; index < userIds.size(); index++) {
            if (index > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }

        String sql = "SELECT user_id1, user_id2 FROM Friendships "
                + "WHERE user_id1 IN (" + placeholders + ") OR user_id2 IN (" + placeholders + ") "
                + "ORDER BY user_id1, user_id2";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int parameterIndex = 1;
            for (int userId : userIds) {
                statement.setInt(parameterIndex++, userId);
            }
            for (int userId : userIds) {
                statement.setInt(parameterIndex++, userId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int userId1 = resultSet.getInt("user_id1");
                    int userId2 = resultSet.getInt("user_id2");

                    if (userSet.contains(userId1)) {
                        relationships.computeIfAbsent(userId1, key -> new ArrayList<>()).add(userId2);
                    }
                    if (userSet.contains(userId2)) {
                        relationships.computeIfAbsent(userId2, key -> new ArrayList<>()).add(userId1);
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE,
                    "Failed to load page-scoped relationship graph for users " + userIds, exception);
        }

        return relationships;
    }

    public SuggestionBundle loadSuggestionBundle(int userId, int topK) {
        ArrayList<Integer> directFriendIds = new ArrayList<>();
        HashSet<Integer> directFriendSet = new HashSet<>();
        HashMap<Integer, Integer> mutualCounts = new HashMap<>();
        Map<Integer, ArrayList<Integer>> mutualIdsByCandidate = new LinkedHashMap<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_SELECTED_USER_SUGGESTION_ROWS)) {

            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            statement.setInt(4, userId);
            statement.setInt(5, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int rowType = resultSet.getInt("row_type");
                    int itemId = resultSet.getInt("item_id");

                    if (rowType == 0) {
                        directFriendIds.add(itemId);
                        directFriendSet.add(itemId);
                        continue;
                    }

                    if (itemId == userId || directFriendSet.contains(itemId)) {
                        continue;
                    }

                    int mutualId = resultSet.getInt("mutual_id");
                    mutualCounts.merge(itemId, 1, Integer::sum);
                    mutualIdsByCandidate.computeIfAbsent(itemId, key -> new ArrayList<>()).add(mutualId);
                }
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE,
                    "Failed to load suggestion bundle for user " + userId, exception);
            return new SuggestionBundle(new ArrayList<>(), new ArrayList<>(), new LinkedHashMap<>());
        }

        ArrayList<SuggestedFriend> suggestions = new ArrayList<>();
        if (topK > 0) {
            PriorityQueue<SuggestedFriend> queue = new PriorityQueue<>();
            for (Map.Entry<Integer, Integer> entry : mutualCounts.entrySet()) {
                if (entry.getValue() > 0) {
                    queue.add(new SuggestedFriend(entry.getKey(), entry.getValue()));
                }
            }

            while (!queue.isEmpty() && suggestions.size() < topK) {
                suggestions.add(queue.poll());
            }
        }

        Map<Integer, ArrayList<Integer>> mutualsBySuggested = new LinkedHashMap<>();
        for (SuggestedFriend suggestion : suggestions) {
            ArrayList<Integer> mutualIds = mutualIdsByCandidate.get(suggestion.getSuggestedId());
            mutualsBySuggested.put(
                    suggestion.getSuggestedId(),
                    mutualIds != null ? new ArrayList<>(mutualIds) : new ArrayList<>());
        }

        return new SuggestionBundle(directFriendIds, suggestions, mutualsBySuggested);
    }

    public ArrayList<Integer> loadFriendIds(int userId) {
        ArrayList<Integer> friendIds = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_FRIEND_IDS)) {

            statement.setInt(1, userId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    friendIds.add(resultSet.getInt("friend_id"));
                }
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load friends for user " + userId, exception);
        }

        return friendIds;
    }

    public ArrayList<Integer> loadMutualFriendIds(int userId1, int userId2) {
        return loadMutualFriendIds(loadFriendIds(userId1), userId2);
    }

    public ArrayList<Integer> loadMutualFriendIds(ArrayList<Integer> user1FriendIds, int userId2) {
        ArrayList<Integer> mutualFriendIds = new ArrayList<>();
        HashSet<Integer> firstFriendSet = new HashSet<>();

        if (user1FriendIds != null) {
            firstFriendSet.addAll(user1FriendIds);
        }

        for (int friendId : loadFriendIds(userId2)) {
            if (firstFriendSet.contains(friendId)) {
                mutualFriendIds.add(friendId);
            }
        }

        return mutualFriendIds;
    }

    public ArrayList<SuggestedFriend> loadSuggestedFriends(int userId, int topK) {
        if (topK <= 0) {
            return new ArrayList<>();
        }

        return loadSuggestionBundle(userId, topK).getSuggestions();
    }

    public ArrayList<SuggestedFriend> loadSuggestedFriends(
            int userId, ArrayList<Integer> directFriendIds, int topK) {
        if (topK <= 0) {
            return new ArrayList<>();
        }

        return loadSuggestionBundle(userId, topK).getSuggestions();
    }

    public boolean insertUser(User user) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_USER)) {

            statement.setInt(1, user.getId());
            statement.setString(2, user.getFullName());
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to insert user " + user.getId(), exception);
            return false;
        }
    }

    public boolean updateUser(User user) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_USER)) {

            statement.setString(1, user.getFullName());
            statement.setInt(2, user.getId());
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to update user " + user.getId(), exception);
            return false;
        }
    }

    public boolean deleteUser(int userId) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement deleteFriendships = connection.prepareStatement(DELETE_USER_FRIENDSHIPS)) {
                deleteFriendships.setInt(1, userId);
                deleteFriendships.setInt(2, userId);
                deleteFriendships.executeUpdate();
            }

            try (PreparedStatement deleteUser = connection.prepareStatement(DELETE_USER)) {
                deleteUser.setInt(1, userId);
                return deleteUser.executeUpdate() > 0;
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to delete user " + userId, exception);
            return false;
        }
    }

    public boolean insertFriendship(Friendship friendship) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_FRIENDSHIP)) {

            statement.setInt(1, friendship.getUserId1());
            statement.setInt(2, friendship.getUserId2());
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE,
                    "Failed to insert friendship " + friendship.getUserId1() + "-" + friendship.getUserId2(),
                    exception);
            return false;
        }
    }

    public boolean deleteFriendship(int userId1, int userId2) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_FRIENDSHIP)) {

            statement.setInt(1, userId1);
            statement.setInt(2, userId2);
            statement.setInt(3, userId2);
            statement.setInt(4, userId1);
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE,
                    "Failed to delete friendship " + userId1 + "-" + userId2, exception);
            return false;
        }
    }

    public boolean isUserExists(int userId) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(CHECK_USER_EXISTS)) {

            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to check existence of user " + userId, exception);
            return false;
        }
    }

    public boolean isFriendshipExists(int userId1, int userId2) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(CHECK_FRIENDSHIP_EXISTS)) {

            statement.setInt(1, userId1);
            statement.setInt(2, userId2);
            statement.setInt(3, userId2);
            statement.setInt(4, userId1);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE,
                    "Failed to check existence of friendship " + userId1 + "-" + userId2, exception);
            return false;
        }
    }
}
