package utils;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class DataGenerator {

    public static void main(String[] args) {
        // Cấu hình số lượng bạn muốn sinh ra
        int totalUsers = 500;    // Sinh ra 500 user
        int totalEdges = 3000;    // Sinh ra 3000 mối quan hệ kết bạn
        String filePath = "data/social_network_data_500.txt"; // Tên file xuất ra

        generateData(totalUsers, totalEdges, filePath);
    }

    public static void generateData(int totalUsers, int totalEdges, String filePath) {
        Random random = new Random();
        // Dùng Set để chống trùng lặp mối quan hệ kết bạn
        Set<String> generatedEdges = new HashSet<>();

        System.out.println("Bắt đầu sinh dữ liệu...");

        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            // PHẦN 1: GHI DANH SÁCH USER
            writer.write(totalUsers + "\n"); // Dòng đầu tiên: Số lượng user
            for (int i = 1; i <= totalUsers; i++) {
                // ID là i, Tên là "User_" + i
                writer.write(i + " User_" + i + "\n");
            }

            // PHẦN 2: GHI DANH SÁCH BẠN BÈ (EDGES)
            writer.write(totalEdges + "\n"); // Dòng đánh dấu số lượng cạnh
            int edgeCount = 0;

            // Vòng lặp chạy cho đến khi đủ 5000 mối quan hệ thì dừng
            while (edgeCount < totalEdges) {
                // Random ngẫu nhiên 2 người từ 1 đến totalUsers
                int userA = random.nextInt(totalUsers) + 1;
                int userB = random.nextInt(totalUsers) + 1;

                // Quy tắc 1: Không tự kết bạn với chính mình
                if (userA == userB) {
                    continue;
                }

                // Mẹo để chống trùng: Luôn đặt ID nhỏ lên trước, ID lớn ra sau
                int minId = Math.min(userA, userB);
                int maxId = Math.max(userA, userB);
                String edgeKey = minId + "-" + maxId; // Ví dụ: "1-5"

                // Quy tắc 2: Kiểm tra xem cặp này đã có chưa
                if (!generatedEdges.contains(edgeKey)) {
                    // Nếu chưa có thì đưa vào Set để đánh dấu
                    generatedEdges.add(edgeKey);

                    // Ghi ra file
                    writer.write(userA + " " + userB + "\n");

                    // Tăng biến đếm lên
                    edgeCount++;
                }
            }

            System.out.println("Tuyệt vời! Đã tạo thành công file: " + filePath);
            System.out.println("Bao gồm " + totalUsers + " users và " + totalEdges + " mối quan hệ.");

        } catch (IOException e) {
            System.out.println("Có lỗi khi ghi file: " + e.getMessage());
        }
    }
}
