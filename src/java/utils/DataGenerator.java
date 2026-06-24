package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DataGenerator {

    private static final int DEFAULT_TOTAL_USERS = 1000;
    private static final int DEFAULT_TOTAL_EDGES = 10000;
    private static final String DEFAULT_OUTPUT_FILE = "data_1000_10000.sql";

    public static void main(String[] args) {
        int totalUsers = DEFAULT_TOTAL_USERS;
        int totalEdges = DEFAULT_TOTAL_EDGES;
        String filePath = DEFAULT_OUTPUT_FILE;

        if (args.length > 0) {
            totalUsers = parsePositiveInt(args[0], totalUsers);
        }
        if (args.length > 1) {
            totalEdges = parsePositiveInt(args[1], totalEdges);
        }
        if (args.length > 2 && !args[2].trim().isEmpty()) {
            filePath = args[2].trim();
        }

        generateSQLData(totalUsers, totalEdges, filePath);
    }

    private static int parsePositiveInt(String rawValue, int defaultValue) {
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    public static void generateSQLData(int totalUsers, int totalEdges, String filePath) {
        Random random = new Random();
        Set<String> generatedEdges = new HashSet<>();

        System.out.println("Start generating SQL data...");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            // 1. Delete old data (if any) to avoid duplicate errors when Phu runs the file
            // again.
            writer.write("DELETE FROM Friendships;\n");
            writer.write("DELETE FROM Users;\n\n");

            // 2. WRITE THE INSERT USER STATEMENTS
            writer.write("-- Insert User list\n");
            for (int i = 1; i <= totalUsers; i++) {
                // : INSERT INTO Users (user_id, full_name) VALUES (1, 'User_1');
                writer.write("INSERT INTO Users (user_id, full_name) VALUES (" + i + ", 'User_" + i + "');\n");
            }

            writer.write("\n-- Insert Friendships list (Graph edge)\n");

            // 3. WRITE THE INSERT FRIENDSHIP STATEMENTS
            int edgeCount = 0;
            while (edgeCount < totalEdges) {
                int userA = random.nextInt(totalUsers) + 1;
                int userB = random.nextInt(totalUsers) + 1;

                if (userA == userB) {
                    continue;
                }

                int minId = Math.min(userA, userB);
                int maxId = Math.max(userA, userB);
                String edgeKey = minId + "-" + maxId;

                if (!generatedEdges.contains(edgeKey)) {
                    generatedEdges.add(edgeKey);
                    // Phu stipulates CHECK constraint (user_id1 < user_id2) so minId must come
                    // before maxId
                    // Syntax: INSERT INTO Friendships (user_id1, user_id2) VALUES (1, 5);
                    writer.write(
                            "INSERT INTO Friendships (user_id1, user_id2) VALUES (" + minId + ", " + maxId + ");\n");
                    edgeCount++;
                }
            }

            System.out.println("File created successfully: " + filePath);

        } catch (IOException e) {
            System.out.println("There was an error while writing the file: " + e.getMessage());
        }
    }
}