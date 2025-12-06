package engine;

public class BenchmarkResults {
    public final double sequentialTimeMs;
    public final double parallelTimeMs;
    public final long pathsFound;
    public final int totalRequests;
    public final int threadCount;
    public BenchmarkResults(double sequentialTimeMs, double parallelTimeMs, long pathsFound, int totalRequests, int threadCount) {
        this.sequentialTimeMs = sequentialTimeMs;
        this.parallelTimeMs = parallelTimeMs;
        this.pathsFound = pathsFound;
        this.totalRequests = totalRequests;
        this.threadCount = threadCount;
    }

    public double calculateSpeedup() {
        if (parallelTimeMs == 0) return 0.0;
        return sequentialTimeMs / parallelTimeMs;
    }

    public double calculateSequentialThroughput() {
        // Convert ms to seconds: Time / 1000.0
        if (sequentialTimeMs == 0) return 0.0;
        return totalRequests / (sequentialTimeMs / 1000.0);
    }

    public double calculateParallelThroughput() {
        if (parallelTimeMs == 0) return 0.0;
        return totalRequests / (parallelTimeMs / 1000.0);
    }
}
