package utils;

public class PerformanceProfiler {

    // Functional interface for passing a callback into the profiler
    public interface AlgorithmTask {
        void execute();
    }

    // Measure execution time strictly following the 4-phase protocol
    public static double measureExecutionTime(AlgorithmTask task, String algorithmName) {
        System.out.println("[PROFILER] Starting protocol for: " + algorithmName + "...");

        // Phase 1 & 2: Setup & Warm-up (Filter JIT Compiler noise)
        // Chạy ẩn 100 lần để ép JVM biên dịch Hot Code
        for (int i = 0; i < 100; i++) {
            task.execute();
        }

        // Dọn dẹp rác bộ nhớ phát sinh từ quá trình Warm-up trước khi đo thật
        System.gc();

        // Phase 3: Measure (Using precise nanoTime)
        long startTime = System.nanoTime();
        task.execute(); // Chạy thuật toán để đo lường
        long endTime = System.nanoTime();

        // Tính toán thời gian
        long timeTakenNanos = endTime - startTime;
        double timeTakenMillis = timeTakenNanos / 1_000_000.0;

        // Phase 4: Cleanup (Garbage Collection sau khi đo)
        System.gc();

        System.out.println("[PROFILER] " + algorithmName + " completed in: " + timeTakenMillis + " ms.\n");
        return timeTakenMillis;
    }
}
