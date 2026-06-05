package utils;

import controller.SocialNetworkController;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DataSynchronizer {

    // Database connection settings
    private static final String DB_URL = "jdbc:mysql://localhost:3306/social_network";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "12345";
    private static final String FALLBACK_FILE_PATH = "data/users.txt";

    /**
     * Main entry point called when the server starts.
     */
    public static void loadDataOnStartup(SocialNetworkController controller) {
        System.out.println("--- STARTING SERVER DATA INITIALIZATION ---");

        boolean isDbSuccess = fetchFromDatabase(controller);

        if (!isDbSuccess) {
            System.err.println("[WARNING] Database is unavailable or not responding. Activating fallback mode...");
            fetchFromFallbackFile(controller);
        }

        System.out.println("--- MEMORY GRAPH INITIALIZATION COMPLETE ---");
    }

    /**
     * 1. Try to load data from the database using JDBC.
     */
    private static boolean fetchFromDatabase(SocialNetworkController controller) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            System.out.println("[INFO] Database connection successful. Loading data...");

            // Step 1.1: Load all users
            ResultSet rsUsers = stmt.executeQuery("SELECT id, full_name FROM users");
            while (rsUsers.next()) {
                controller.registerUser(rsUsers.getInt("id"), rsUsers.getString("full_name"));
            }

            // Step 1.2: Load all relationships (edges)
            ResultSet rsFriends = stmt.executeQuery("SELECT user_id_1, user_id_2 FROM friendships");
            while (rsFriends.next()) {
                // Use the makeFriend method already implemented in the controller
                controller.makeFriend(rsFriends.getInt("user_id_1"), rsFriends.getInt("user_id_2"));
            }

            return true; // Success

        } catch (Exception e) {
            System.err.println("[ERROR] Database connection error: " + e.getMessage());
            return false; // Failure, signal that fallback is needed
        }
    }

    /**
     * 2. Read data from the fallback txt file.
     * File format example:
     * USER:1:Alice
     * USER:2:Bob
     * FRIEND:1:2
     */
    private static void fetchFromFallbackFile(SocialNetworkController controller) {
        System.out.println("[INFO] Reading fallback data file: " + FALLBACK_FILE_PATH);

        try (BufferedReader br = new BufferedReader(new FileReader(FALLBACK_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // Skip empty lines or comments

                String[] parts = line.split(":");
                String type = parts[0];

                if (type.equals("USER") && parts.length == 3) {
                    int id = Integer.parseInt(parts[1]);
                    String name = parts[2];
                    controller.registerUser(id, name);
                }
                else if (type.equals("FRIEND") && parts.length == 3) {
                    int uId = Integer.parseInt(parts[1]);
                    int vId = Integer.parseInt(parts[2]);
                    controller.makeFriend(uId, vId);
                }
            }
            System.out.println("[INFO] Fallback file data loaded successfully.");

        } catch (Exception e) {
            System.err.println("[FATAL] Catastrophic error: both the database and the fallback file could not be read!");
            e.printStackTrace();
            // In practice, if both fail, the server may have to shut down (System.exit(1))
        }
    }
}
