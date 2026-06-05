package dispatcher;
import model.SuggestedFriend;
import utils.DataSynchronizer;
import java.util.ArrayList;
import controller.SocialNetworkController;

public class Main {
    public static void main(String[] args) {

        // 1. Khởi tạo Controller (Chứa Đồ thị Rỗng)
        SocialNetworkController controller = new SocialNetworkController();
        // 2. Kích hoạt Module tự động kéo dữ liệu (Cold Start Hydration)
        DataSynchronizer.loadDataOnStartup(controller);
        // 3. Mở Server cho Client gọi (Ví dụ: Hiển thị Menu Console)
        System.out.println("\n[SERVER READY] Hệ thống Mạng xã hội đã sẵn sàng hoạt   động!");
        // SocialNetworkController controller = new SocialNetworkController();

        // 1. Khởi tạo dữ liệu (Trong thực tế sẽ dùng Utils.FileHandler để đọc)
        controller.registerUser(1, "Alice");
        controller.registerUser(2, "Bob");
        controller.registerUser(3, "Charlie");
        controller.registerUser(4, "David");

        // 2. Tạo lập các mối quan hệ đồ thị
        // Alice(1) quen Bob(2) và Charlie(3)
        controller.makeFriend(1, 2);
        controller.makeFriend(1, 3);

        // David(4) quen Bob(2) và Charlie(3) -> David có 2 bạn chung với Alice
        controller.makeFriend(4, 2);
        controller.makeFriend(4, 3);

        // 3. Test tính năng Gợi ý bạn bè cho Alice (ID: 1)
        System.out.println("\n--- Gợi ý bạn bè cho Alice ---");
        ArrayList<SuggestedFriend> suggestions = controller.suggestMutualFriends(1);

        for (SuggestedFriend sf : suggestions) {
            System.out.println("Gợi ý User ID " + sf.getUserId() + " | Số bạn chung: " + sf.getMutualFriendsCount());
        }
    }
}
