package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Graph;
import model.Friendship;
import model.User;

public class SocialGraphDAO {
    private static final Logger LOGGER = Logger.getLogger(SocialGraphDAO.class.getName());

    private static final String DB_NAME = "SocialNetworkFriendSuggestion";
    private static final String USER_NAME = "sa";
    private static final String PASSWORD = "12345";

    private static final String GET_USERS = "SELECT user_id, full_name FROM Users";
    private static final String GET_FRIENDSHIPS = "SELECT user_id1, user_id2 FROM Friendships";
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
        private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(connectionString, USER_NAME, PASSWORD);
    }

    public Graph loadGraphFromDatabase() {
        Graph graph = new Graph();

        try {
            LOGGER.info("Database connection successful. Loading graph data...");

            for (User user : loadUsers()) {
                graph.addUser(user);
            }

            for (Friendship friendship : loadFriendships()) {
                graph.addFriendship(friendship.getUserId1(), friendship.getUserId2());
            }

            return graph;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load graph from database", exception);
            return null;
        }
    }

    private ArrayList<User> loadUsers() throws ClassNotFoundException, SQLException {
        ArrayList<User> users = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_USERS);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(new User(resultSet.getInt("user_id"), resultSet.getString("full_name")));
            }
        }

        return users;
    }

    private ArrayList<Friendship> loadFriendships() throws ClassNotFoundException, SQLException {
        ArrayList<Friendship> friendships = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_FRIENDSHIPS);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                friendships.add(new Friendship(resultSet.getInt("user_id1"), resultSet.getInt("user_id2")));
            }
        }

        return friendships;
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
