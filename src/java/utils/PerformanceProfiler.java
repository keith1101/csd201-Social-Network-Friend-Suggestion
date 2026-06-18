package utils;

public class PerformanceProfiler {

    // Functional interface for passing a callback into the profiler
    public interface AlgorithmTask {
        void execute();
    }

    // Measure execution time in milliseconds
    public static long measureExecutionTime(AlgorithmTask task, String algorithmName) {
        System.out.println("[PROFILER] Running algorithm: " + algorithmName + "...");

        long startTime = System.currentTimeMillis(); // Start timer

        task.execute(); // Execute the algorithm

        long endTime = System.currentTimeMillis();   // Stop timer
        long timeTaken = endTime - startTime;

        System.out.println("[PROFILER] " + algorithmName + " completed in: " + timeTaken + " ms.\n");
        return timeTaken;
    }
}
