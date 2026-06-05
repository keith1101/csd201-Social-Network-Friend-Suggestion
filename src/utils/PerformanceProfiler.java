package utils;

public class PerformanceProfiler {

    // Interface hỗ trợ truyền hàm (Callback) vào để đo lường
    public interface AlgorithmTask {
        void execute();
    }

    // Hàm đo đạc thời gian thực thi (mili-giây)
    public static long measureExecutionTime(AlgorithmTask task, String algorithmName) {
        System.out.println("[PROFILER] Đang chạy thuật toán: " + algorithmName + "...");

        long startTime = System.currentTimeMillis(); // Bắt đầu bấm giờ

        task.execute(); // Thực thi thuật toán của Huy hoặc Hào

        long endTime = System.currentTimeMillis();   // Kết thúc bấm giờ
        long timeTaken = endTime - startTime;

        System.out.println("[PROFILER] " + algorithmName + " hoàn tất trong: " + timeTaken + " ms.\n");
        return timeTaken;
    }
}
