package utils;

import controller.SocialNetworkController;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import model.SocialGraph;
import model.User;
import view.View;

public class DataSynchronizer {

    //Querry setting
    private static final String GET_USER = "SELECT user_id, full_name FROM Users";
    private static final String GET_FRIENDSHIP = "SELECT user_id1, user_id2 FROM Friendships";
    private static final String INSERT_FRIENDSHIP = "INSERT INTO Friendships(user_id1, user_id2) VALUES (?,?)";
    private static final String DELETE_FRIENDSHIP = "DELETE FROM Friendships where user_id1=?, user_id2=?";

    //Load data from sql server
    public static SocialGraph fetchFromDatabase() {
        SocialGraph Graph = new SocialGraph();
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {

            conn = DBUtils.getConnection();
            if (conn != null) {
                View.showMessage("[INFO] Database connection successful. Loading data...");
                ptm = conn.prepareStatement(GET_USER);
                //Load all users v
                rs = ptm.executeQuery();
                //parse all the data from the querry
                while (rs.next()) {
                   Graph.addUser(new User (rs.getInt("user_id"),rs.getString("full_name")));
                }

                //set new querry
                ptm = conn.prepareStatement(GET_FRIENDSHIP);
                //Load all relationships e
                rs = ptm.executeQuery();
                //parse all the data from the querry
                while (rs.next()) {
                    // Use the makeFriend method already implemented in the controller
                    Graph.addEdge(rs.getInt("user_id1"), rs.getInt("user_id2"));
                }
            }

            return Graph; // Success to load data

        } catch (Exception e) {
            View.showMessage("[ERROR] Database connection error: " + e.getMessage());
            return null; //cant load data from database
        }
    }
    
    private boolean insertFriendships(int user_id1, int user_id2){
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            boolean check = false;
            conn = DBUtils.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(INSERT_FRIENDSHIP);
                ptm.setInt(1,user_id1);
                ptm.setInt(2,user_id2);
                check= (ptm.executeUpdate()>0) ? true: false;                           
            }
            return check;//return the result of the queryy

        } catch (Exception e) {
            View.showMessage("[ERROR] Database connection error: " + e.getMessage());
            return false; //cant connect database
        }
    }
    
    private boolean deleteFriendships(int user_id1, int user_id2){
        Connection conn = null;
        PreparedStatement ptm = null;
        ResultSet rs = null;
        try {
            boolean check = false;
            conn = DBUtils.getConnection();
            if (conn != null) {
                ptm = conn.prepareStatement(DELETE_FRIENDSHIP);
                ptm.setInt(1,user_id1);
                ptm.setInt(2,user_id2);
                check= (ptm.executeUpdate()>0) ? true: false;                           
            }
            return check;//return the result of the queryy

        } catch (Exception e) {
            View.showMessage("[ERROR] Database connection error: " + e.getMessage());
            return false; //cant connect database
        }
    }

}
