/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author hoadoan
 */
public class DBUtils {
    //database configure
    private static final String DB_NAME="SocialNetworkFriendSuggestion";
    private static final String USER_NAME="sa";
    private static final String PASSWORD="12345";
    public static Connection getConnection() throws ClassNotFoundException, SQLException{
    Connection conn= null;
    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    String url="jdbc:sqlserver://localhost:1433; databaseName="+ DB_NAME;
    conn= DriverManager.getConnection(url, USER_NAME, PASSWORD);
    return conn;
    }
}
