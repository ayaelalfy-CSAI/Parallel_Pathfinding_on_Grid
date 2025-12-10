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

    private final PathFinder finder;

    public ParallelPathfindingEngine(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;

        this.finder = new DijkstraPathFinder();
    }


    public List<Path> processRequests(List<PathRequest> requests) {

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        List<Future<Path>> futures = new ArrayList<>();
        List<Path> results = new ArrayList<>();

        System.out.printf("  [Engine] Starting parallel processing with %d threads for %d requests...\n",
                threadPoolSize, requests.size());


        for (PathRequest request : requests) {

            Callable<Path> task = () -> {

                return finder.findPath(request);
            };


            futures.add(executor.submit(task));
        }


        for (Future<Path> future : futures) {
            try {

                results.add(future.get());
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                System.err.println("  [Engine] Task interrupted: " + e.getMessage());
                results.add(Path.notFound());
            } catch (Exception e) {

                System.err.println("  [Engine] Error during pathfinding: " + e.getMessage());
                results.add(Path.notFound());
            }
        }


        executor.shutdown();
        try {

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
