package engine;

import core.*;
import algorithm.PathFinder;
import algorithm.DijkstraPathFinder;
import java.util.*;
import java.util.concurrent.*;

public class ParallelPathfindingEngine {
    private final int threadPoolSize;
    public ParallelPathfindingEngine(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
    public List<Path> processRequests(List<PathRequest> requests) {
        return processRequestsWithVisualization(requests, null);
    }

    public List<Path> processRequestsWithVisualization(List<PathRequest> requests, VisualizationCallback callback) {
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<PathResult>> futures = new ArrayList<>();
        Map<Integer, Path> results = new ConcurrentHashMap<>();
        System.out.printf("  [Engine] Starting parallel processing with %d threads for %d requests...\n",
                threadPoolSize, requests.size());

        for (int i = 0; i < requests.size(); i++) {
            final int pathIndex = i;
            final PathRequest request = requests.get(i);
            Future<PathResult> future = executor.submit(() -> {
                if (callback != null) {
                    Path path = findPathWithVisualization(request, pathIndex, callback);
                    return new PathResult(pathIndex, path);
                } else {
                    PathFinder finder = new DijkstraPathFinder();
                    Path path = finder.findPath(request);
                    return new PathResult(pathIndex, path);
                }
            });

            futures.add(future);
        }

        for (Future<PathResult> future : futures) {
            try {
                PathResult result = future.get();
                results.put(result.index, result.path);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("  [Engine] Task interrupted: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("  [Engine] Error during pathfinding: " + e.getMessage());
                e.printStackTrace();
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("  [Engine] Thread pool did not terminate gracefully.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        List<Path> orderedResults = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            orderedResults.add(results.getOrDefault(i, Path.notFound()));
        }
        System.out.println("  [Engine] Parallel processing complete.");
        return orderedResults;
    }

    private Path findPathWithVisualization(PathRequest request, int pathIndex, VisualizationCallback callback) throws InterruptedException {
        Grid grid = request.getGrid();
        Cell start = request.getStartCell();
        Cell goal = request.getGoalCell();
        Map<Cell, Double> dist = new HashMap<>();
        Map<Cell, Cell> prev = new HashMap<>();
        Set<Cell> visited = new HashSet<>();
        TreeSet<NodeCell> pq = new TreeSet<>();
        dist.put(start, 0.0);
        pq.add(new NodeCell(start, 0.0));
        while (!pq.isEmpty()) {
            NodeCell currentNode = pq.pollFirst();
            Cell u = currentNode.cell;
            double uDist = currentNode.cost;
            if (visited.contains(u)) continue;
            visited.add(u);
            if (callback != null) {
                callback.onCellExplored(pathIndex, u, true);
            }
            if (u.equals(goal)) {
                break;
            }
            List<Cell> neighbors = new ArrayList<>(grid.getNeighbors(u));
            neighbors.sort((a, b) -> {
                int rowCmp = Integer.compare(a.getRow(), b.getRow());
                if (rowCmp != 0) return rowCmp;
                return Integer.compare(a.getCol(), b.getCol());
            });
            for (Cell v : neighbors) {
                if (visited.contains(v)) continue;
                double weight = v.getWeight();
                double alt = uDist + weight;
                double oldDist = dist.getOrDefault(v, Double.POSITIVE_INFINITY);
                if (alt < oldDist) {
                    // Remove old entry
                    if (dist.containsKey(v)) {
                        pq.remove(new NodeCell(v, oldDist));
                    }
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.add(new NodeCell(v, alt));
                    if (callback != null) {
                        callback.onCellExplored(pathIndex, v, false);
                    }
                }
            }
        }
        if (!dist.containsKey(goal)) {
            return Path.notFound();
        }
        List<Cell> path = new ArrayList<>();
        Cell cur = goal;
        while (cur != null) {
            path.add(cur);
            if (cur.equals(start)) break;
            cur = prev.get(cur);
        }
        Collections.reverse(path);
        double totalCost = dist.get(goal);
        return new Path(path, totalCost);
    }
    private static class PathResult {
        final int index;
        final Path path;
        PathResult(int index, Path path) {
            this.index = index;
            this.path = path;
        }
    }
    private static class NodeCell implements Comparable<NodeCell> {
        final Cell cell;
        final double cost;
        NodeCell(Cell cell, double cost) {
            this.cell = cell;
            this.cost = cost;
        }
        @Override
        public int compareTo(NodeCell other) {
            int costCmp = Double.compare(this.cost, other.cost);
            if (costCmp != 0) return costCmp;

            int rowCmp = Integer.compare(this.cell.getRow(), other.cell.getRow());
            if (rowCmp != 0) return rowCmp;

            return Integer.compare(this.cell.getCol(), other.cell.getCol());
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            NodeCell other = (NodeCell) obj;
            return cell.equals(other.cell);
        }
        @Override
        public int hashCode() {
            return cell.hashCode();
        }
    }

}