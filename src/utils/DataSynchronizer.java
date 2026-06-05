package utils;

import controller.SocialNetworkController;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DataSynchronizer {

    // Đường dẫn giả định
    private static final String DB_URL = "jdbc:mysql://localhost:3306/social_network";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "password";
    private static final String FALLBACK_FILE_PATH = "data/users.txt";

    /**
     * Hàm chính được gọi khi Server Start
     */
    public static void loadDataOnStartup(SocialNetworkController controller) {
        System.out.println("--- BẮT ĐẦU KHỞI TẠO DỮ LIỆU SERVER ---");
        
        boolean isDbSuccess = fetchFromDatabase(controller);
        
        if (!isDbSuccess) {
            System.err.println("[CẢNH BÁO] Database sập hoặc không phản hồi. Kích hoạt cơ chế Fallback...");
            fetchFromFallbackFile(controller);
        }
        
        System.out.println("--- HOÀN TẤT KHỞI TẠO ĐỒ THỊ TRÊN RAM ---");
    }

    /**
     * 1. Cố gắng lấy dữ liệu từ Database bằng JDBC
     */
    private static boolean fetchFromDatabase(SocialNetworkController controller) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("[INFO] Kết nối Database thành công. Đang tải dữ liệu...");

            // Bước 1.1: Tải toàn bộ Users
            ResultSet rsUsers = stmt.executeQuery("SELECT id, full_name FROM users");
            while (rsUsers.next()) {
                controller.registerUser(rsUsers.getInt("id"), rsUsers.getString("full_name"));
            }

            // Bước 1.2: Tải toàn bộ các mối quan hệ (Edges)
            ResultSet rsFriends = stmt.executeQuery("SELECT user_id_1, user_id_2 FROM friendships");
            while (rsFriends.next()) {
                // Sử dụng hàm makeFriend đã viết ở Controller
                controller.makeFriend(rsFriends.getInt("user_id_1"), rsFriends.getInt("user_id_2"));
            }

            return true; // Thành công

        } catch (Exception e) {
            System.err.println("[ERROR] Lỗi kết nối Database: " + e.getMessage());
            return false; // Thất bại, báo hiệu cần dùng Fallback
        }
    }

    /**
     * 2. Đọc dữ liệu từ file txt dự phòng (Fallback)
     * Định dạng file txt (Ví dụ):
     * USER:1:Alice
     * USER:2:Bob
     * FRIEND:1:2
     */
    private static void fetchFromFallbackFile(SocialNetworkController controller) {
        System.out.println("[INFO] Đang đọc dữ liệu từ file fallback: " + FALLBACK_FILE_PATH);
        
        try (BufferedReader br = new BufferedReader(new FileReader(FALLBACK_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; // Bỏ qua dòng trống hoặc comment

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
            System.out.println("[INFO] Tải dữ liệu từ file Fallback thành công.");
            
        } catch (Exception e) {
            System.err.println("[FATAL] Lỗi thảm họa: Cả Database và File Fallback đều không đọc được!");
            e.printStackTrace();
            // Trong thực tế, nếu cả 2 đều hỏng, Server có thể bị buộc tắt (System.exit(1))
        }
    }
}
