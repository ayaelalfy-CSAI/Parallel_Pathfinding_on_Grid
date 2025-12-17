package engine;

import core.Cell;
import core.Grid;
import core.Path;
import core.PathRequest;
import algorithm.DijkstraPathFinder;
import algorithm.PathFinder;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.SwingUtilities;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;

public class PathfindingExperiment {

    public static final int BENCHMARK_GRID_SIZE = 150;
    public static final int VIS_GRID_SIZE = 15;
    public static final int MAX_WEIGHT = 10;
    public static final double OBSTACLE_DENSITY = 0.15;
    public static final int NUM_REQUESTS = 50;
    public static void main(String[] args) {
        System.out.println("--- Starting Project 6 Interactive Pathfinding Application ---");
        System.out.println("Application launched. Use the GUI for pathfinding and benchmarking.");
        SwingUtilities.invokeLater(() -> {
            new PathfindingVisualizer().setVisible(true);
        });
    }
    public static Path runSinglePathfinding(Grid grid, int startR, int startC, int goalR, int goalC) {
        Cell startCell = grid.getCell(startR, startC);
        Cell goalCell = grid.getCell(goalR, goalC);
        if (startCell == null || goalCell == null || !grid.isWalkable(startCell) || !grid.isWalkable(goalCell)) {
            System.err.println("Error: Start or Goal cell is invalid or blocked.");
            return Path.notFound();
        }
        PathFinder finder = new DijkstraPathFinder();
        PathRequest request = new PathRequest(1, grid, startCell, goalCell);
        return finder.findPath(request);
    }

    public static BenchmarkResults runFullBenchmark(int threadCount) {
        Grid sharedGrid = new Grid(BENCHMARK_GRID_SIZE, BENCHMARK_GRID_SIZE, MAX_WEIGHT, OBSTACLE_DENSITY);
        List<PathRequest> requests = generatePathRequests(sharedGrid, NUM_REQUESTS);
        System.out.printf("\n--- Running Benchmark (%d Threads) ---\n", threadCount);
        PathFinder sequentialFinder = new DijkstraPathFinder();
        long seqStartTime = System.nanoTime();
        for (PathRequest request : requests) {
            sequentialFinder.findPath(request);
        }
        double sequentialTimeMs = (System.nanoTime() - seqStartTime) / 1_000_000.0;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Path>> futures = new ArrayList<>();
        long parStartTime = System.nanoTime();
        for (PathRequest request : requests) {
            Callable<Path> task = () -> sequentialFinder.findPath(request);
            futures.add(executor.submit(task));
        }
        long foundCount = 0;
        for (Future<Path> future : futures) {
            try {
                if (future.get().isFound()) foundCount++;
            } catch (Exception e) {
            }
        }
        double parallelTimeMs = (System.nanoTime() - parStartTime) / 1_000_000.0;
        executor.shutdownNow(); // Ensure cleanup
        System.out.printf("Finished. Sequential: %.3f ms, Parallel: %.3f ms\n", sequentialTimeMs, parallelTimeMs);
        return new BenchmarkResults(
                sequentialTimeMs,
                parallelTimeMs,
                foundCount,
                NUM_REQUESTS,
                threadCount
        );
    }
    public static Grid createVisualizationGrid() {
        return new Grid(VIS_GRID_SIZE, VIS_GRID_SIZE, MAX_WEIGHT, OBSTACLE_DENSITY);
    }
    private static List<PathRequest> generatePathRequests(Grid grid, int count) {
        List<PathRequest> requests = new ArrayList<>(count);
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            Cell start = findWalkableCell(grid, rand);
            Cell goal = findWalkableCell(grid, rand);
            while (start.equals(goal)) {
                goal = findWalkableCell(grid, rand);
            }
            requests.add(new PathRequest(i + 1, grid, start, goal));
        }
        return requests;
    }
    public static Cell findWalkableCell(Grid grid, Random rand) {
        Cell cell;
        int maxAttempts = 1000;
        int r, c;
        for (int i = 0; i < maxAttempts; i++) {
            r = rand.nextInt(grid.getRows());
            c = rand.nextInt(grid.getCols());
            cell = grid.getCell(r, c);
            if (grid.isWalkable(cell)) {
                return cell;
            }
        }
        return grid.getCell(0,0);
    }
}
