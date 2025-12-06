package engine;

import core.Path;
import core.PathRequest;
import algorithm.PathFinder;
import algorithm.DijkstraPathFinder;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ParallelPathfindingEngine {

    private final int threadPoolSize;
    // We hold a reference to the PathFinder instance (Dijkstra's) which performs the actual work.
    private final PathFinder finder;

    public ParallelPathfindingEngine(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        // Instantiate the sequential algorithm to perform the actual search in each thread.
        this.finder = new DijkstraPathFinder();
    }


    public List<Path> processRequests(List<PathRequest> requests) {
        // Create a fixed-size thread pool
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        List<Future<Path>> futures = new ArrayList<>();
        List<Path> results = new ArrayList<>();

        System.out.printf("  [Engine] Starting parallel processing with %d threads for %d requests...\n",
                threadPoolSize, requests.size());

        // 1. Submit Callable Tasks
        for (PathRequest request : requests) {
            // A Callable task for each request, using the sequential PathFinder
            Callable<Path> task = () -> {
                // The PathRequest guarantees thread safety by providing a CLONED grid.
                return finder.findPath(request);
            };

            // Submit the task and store the Future
            futures.add(executor.submit(task));
        }

        // 2. Aggregate Results
        for (Future<Path> future : futures) {
            try {
                // Wait for the result using future.get()
                results.add(future.get());
            } catch (InterruptedException e) {
                // Handle thread interruption
                Thread.currentThread().interrupt();
                System.err.println("  [Engine] Task interrupted: " + e.getMessage());
                results.add(Path.notFound());
            } catch (Exception e) {
                // Handle general execution errors
                System.err.println("  [Engine] Error during pathfinding: " + e.getMessage());
                results.add(Path.notFound());
            }
        }

        // 3. Shutdown the ExecutorService
        executor.shutdown();
        try {
            // Wait for all threads to finish gracefully
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("  [Engine] Thread pool did not terminate gracefully. Forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("  [Engine] Parallel processing complete.");
        return results;
    }
}
