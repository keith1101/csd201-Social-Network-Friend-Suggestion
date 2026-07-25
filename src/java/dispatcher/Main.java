package dispatcher;

import controller.SocialNetworkController;
import view.ConsoleView;

public class Main {

    private static final SocialNetworkController CONTROLLER = new SocialNetworkController();

    private Main() {
    }

    public static void main(String[] args) {
        ConsoleView.displayMessage("Social Network Friend Suggestion");

        boolean running = true;
        while (running) {
            ConsoleView.displayMenu();
            String choice = ConsoleView.promptMenuChoice().trim();

            switch (choice) {
                case "1":
                    CONTROLLER.registerUser(
                            ConsoleView.promptUserId("Enter user ID"),
                            ConsoleView.promptUserName("Enter full name"));
                    break;
                case "2":
                    CONTROLLER.makeFriend(
                            ConsoleView.promptUserId("Enter first user ID"),
                            ConsoleView.promptUserId("Enter second user ID"));
                    break;
                case "3":
                    CONTROLLER.unFriend(
                            ConsoleView.promptUserId("Enter first user ID"),
                            ConsoleView.promptUserId("Enter second user ID"));
                    break;
                case "4":
                    CONTROLLER.suggestMutualFriends(
                            ConsoleView.promptUserId("Enter user ID"));
                    break;
                case "5":
                    CONTROLLER.showUsers();
                    break;
                case "6":
                    CONTROLLER.showRelationshipGraph();
                    break;
                case "7":
                    CONTROLLER.removeUser(ConsoleView.promptUserId("Enter user ID"));
                    break;
                // THÊM ĐOẠN CODE NÀY DÀNH RIÊNG CHO LÚC DEMO BENCHMARK
                case "9":
                    ConsoleView.displayMessage("\n[DEV ONLY] INITIATING PERFORMANCE BENCHMARK...");
                    int testUserId = Integer.parseInt(ConsoleView.promptUserId("Enter target User ID for benchmark"));

                    // Khởi tạo DAO độc lập cho bài test
                    model.SocialGraphDAO testDao = new model.SocialGraphDAO();

                    // Chạy ngầm thuật toán, không gọi ConsoleView để tránh độ trễ in ấn (I/O latency)
                    utils.PerformanceProfiler.AlgorithmTask maxHeapTask = () -> {
                        testDao.loadSuggestedFriends(testUserId, 5);
                    };

                    // Gọi Profiler (100 vòng Warm-up sẽ chạy hoàn toàn im lặng)
                    utils.PerformanceProfiler.measureExecutionTime(maxHeapTask, "Max-Heap Top-K Bounded BFS");

                    // Sau khi đo xong, mới in kết quả 1 lần duy nhất ra màn hình cho giảng viên xem
                    ConsoleView.displaySuggestedFriends(testDao.loadSuggestedFriends(testUserId, 5));
                    break;
                case "0":
                    running = false;
                    ConsoleView.displayMessage("Goodbye.");
                    break;
                default:
                    ConsoleView.displayMessage("Invalid menu option.");
                    break;
            }
        }
    }
}
