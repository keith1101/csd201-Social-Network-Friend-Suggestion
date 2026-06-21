package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DataGenerator {

    public static void main(String[] args) {
        int totalUsers = 2000;
        int totalEdges = 10000;
        String filePath = "data_2000.sql"; // Lưu ý đuôi file đã đổi thành .sql

        generateSQLData(totalUsers, totalEdges, filePath);
    }

    public static void generateSQLData(int totalUsers, int totalEdges, String filePath) {
        Random random = new Random();
        Set<String> generatedEdges = new HashSet<>();

        System.out.println("Start generating SQL data...");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            // 1. Xóa dữ liệu cũ (nếu có) để tránh lỗi trùng lặp khi Phú chạy lại file
            writer.write("DELETE FROM Friendships;\n");
            writer.write("DELETE FROM Users;\n\n");

            // 2. GHI CÂU LỆNH INSERT USER
            writer.write("-- Insert User list\n");
            for (int i = 1; i <= totalUsers; i++) {
                // Cú pháp: INSERT INTO Users (user_id, full_name) VALUES (1, 'User_1');
                writer.write("INSERT INTO Users (user_id, full_name) VALUES (" + i + ", 'User_" + i + "');\n");
            }

            writer.write("\n-- Insert Friendships list (Graph edge)\n");

            // 3. GHI CÂU LỆNH INSERT BẠN BÈ
            int edgeCount = 0;
            while (edgeCount < totalEdges) {
                int userA = random.nextInt(totalUsers) + 1;
                int userB = random.nextInt(totalUsers) + 1;

                if (userA == userB)
                    continue;

                int minId = Math.min(userA, userB);
                int maxId = Math.max(userA, userB);
                String edgeKey = minId + "-" + maxId;

                if (!generatedEdges.contains(edgeKey)) {
                    generatedEdges.add(edgeKey);
                    // Phú quy định Ràng buộc CHECK (user_id1 < user_id2) nên minId phải đứng trước
                    // maxId
                    // Cú pháp: INSERT INTO Friendships (user_id1, user_id2) VALUES (1, 5);
                    writer.write(
                            "INSERT INTO Friendships (user_id1, user_id2) VALUES (" + minId + ", " + maxId + ");\n");
                    edgeCount++;
                }
            }

            System.out.println("File created successfully:" + filePath);

        } catch (IOException e) {
            System.out.println("Error writing file: " + e.getMessage());
        }
    }
}