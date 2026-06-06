package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import model.domain.Graph;
import model.entity.Friendship;
import model.entity.User;
import view.ConsoleView;

public class SocialGraphDAO {
    private static final String DB_NAME = "SocialNetworkFriendSuggestion";
    private static final String USER_NAME = "sa";
    private static final String PASSWORD = "12345";

    private static final String GET_USERS = "SELECT user_id, full_name FROM Users";
    private static final String GET_FRIENDSHIPS = "SELECT user_id1, user_id2 FROM Friendships";
    private static final String INSERT_FRIENDSHIP = "INSERT INTO Friendships(user_id1, user_id2) VALUES (?, ?)";
    private static final String DELETE_FRIENDSHIP = "DELETE FROM Friendships WHERE user_id1 = ? AND user_id2 = ?";

    private final String connectionString;

    public SocialGraphDAO() {
        this.connectionString = "jdbc:sqlserver://localhost:1433;databaseName=" + DB_NAME;
    }

    public Graph loadGraphFromDatabase() {
        Graph graph = new Graph();

        try {
            ConsoleView.displayMessage("[INFO] Database connection successful. Loading data...");

            for (User user : loadUsers()) {
                graph.addUser(user);
            }

            for (Friendship friendship : loadFriendships()) {
                graph.addFriendship(friendship.getUserId1(), friendship.getUserId2());
            }

            return graph;
        } catch (Exception exception) {
            ConsoleView.displayMessage("[ERROR] Database connection error: " + exception.getMessage());
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
        // TODO: Future implementation for insertUser
        return false;
    }

    public boolean updateUser(User user) {
        // TODO: Future implementation for updateUser
        return false;
    }

    public boolean deleteUser(int userId) {
        // TODO: Future implementation for deleteUser
        return false;
    }

    public boolean insertFriendship(Friendship friendship) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_FRIENDSHIP)) {

            statement.setInt(1, friendship.getUserId1());
            statement.setInt(2, friendship.getUserId2());
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            ConsoleView.displayMessage("[ERROR] Database connection error: " + exception.getMessage());
            return false;
        }
    }

    public boolean deleteFriendship(int userId1, int userId2) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_FRIENDSHIP)) {

            statement.setInt(1, userId1);
            statement.setInt(2, userId2);
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            ConsoleView.displayMessage("[ERROR] Database connection error: " + exception.getMessage());
            return false;
        }
    }

    public boolean isUserExists(int userId) {
        // TODO: Future implementation for isUserExists
        return false;
    }

    public boolean isFriendshipExists(int userId1, int userId2) {
        // TODO: Future implementation for isFriendshipExists
        return false;
    }

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        return DriverManager.getConnection(connectionString, USER_NAME, PASSWORD);
    }
}
