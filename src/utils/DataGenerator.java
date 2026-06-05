//package utils;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.HashSet;
//import java.util.Random;
//import java.util.Set;
//
//public class DataGenerator {
//
//    public static void main(String[] args) {
//        // Configure how much sample data to generate
//        int totalUsers = 500;    // Generate 500 users
//        int totalEdges = 3000;   // Generate 3000 friendship relationships
//        String filePath = "src/data/users.txt"; // Output file path
//
//        generateData(totalUsers, totalEdges, filePath);
//    }
//
//    public static void generateData(int totalUsers, int totalEdges, String filePath) {
//        Random random = new Random();
//        // Use a Set to prevent duplicate friendships
//        Set<String> generatedEdges = new HashSet<>();
//
//        System.out.println("Starting data generation...");
//
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
//
//            // PART 1: WRITE THE USER LIST
//            writer.write(totalUsers + "\n"); // First line: number of users
//            for (int i = 1; i <= totalUsers; i++) {
//                // ID is i, name is "User_" + i
//                writer.write(i + " User_" + i + "\n");
//            }
//
//            // PART 2: WRITE THE FRIENDSHIP LIST (EDGES)
//            writer.write(totalEdges + "\n"); // Line that marks the number of edges
//            int edgeCount = 0;
//
//            // Continue until enough relationships have been generated
//            while (edgeCount < totalEdges) {
//                // Randomly pick two people from 1 to totalUsers
//                int userA = random.nextInt(totalUsers) + 1;
//                int userB = random.nextInt(totalUsers) + 1;
//
//                // Rule 1: Do not friend yourself
//                if (userA == userB) {
//                    continue;
//                }
//
//                // Trick to avoid duplicates: always put the smaller ID first
//                int minId = Math.min(userA, userB);
//                int maxId = Math.max(userA, userB);
//                String edgeKey = minId + " " + maxId; // Example: "1-5"
//
//                // Rule 2: Check whether this pair already exists
//                if (!generatedEdges.contains(edgeKey)) {
//                    // If not, add it to the Set as a marker
//                    generatedEdges.add(edgeKey);
//
//                    // Write to file
//                    writer.write(userA + " " + userB + "\n");
//
//                    // Increment the counter
//                    edgeCount++;
//                }
//            }
//
//            System.out.println("Great! Successfully created file: " + filePath);
//            System.out.println("Includes " + totalUsers + " users and " + totalEdges + " relationships.");
//
//        } catch (IOException e) {
//            System.out.println("Error writing file: " + e.getMessage());
//        }
//    }
//}
