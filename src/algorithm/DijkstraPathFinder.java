package algorithm;

import core.*;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.*;

public class DijkstraPathFinder implements PathFinder {
    @Override
    public Path findPath(PathRequest request) {
        Grid grid = request.getGrid();
        Cell start = request.getStartCell();
        Cell goal = request.getGoalCell();
        Map<Cell, Double> dist = new HashMap<>();
        Map<Cell, Cell> prev = new HashMap<>();
        Set<Cell> visited = new HashSet<>();
        TreeSet<Cell> pq = new TreeSet<>(
                Comparator.comparingDouble((Cell c) -> dist.getOrDefault(c, Double.POSITIVE_INFINITY))
                        .thenComparingInt(Cell::getRow)
                        .thenComparingInt(Cell::getCol)
        );

        dist.put(start, 0.0);
        pq.add(start);
        while (!pq.isEmpty()) {
            Cell u = pq.pollFirst();
            double uDist = dist.get(u);
            if (visited.contains(u)) continue;
            visited.add(u);
            if (u.equals(goal)) break;
            List<Cell> neighbors = new ArrayList<>(grid.getNeighbors(u));
            neighbors.sort((a, b) -> {
                int rowCmp = Integer.compare(a.getRow(), b.getRow());
                if (rowCmp != 0) return rowCmp;
                return Integer.compare(a.getCol(), b.getCol());
            });

            for (Cell v : neighbors) {
                if (visited.contains(v)) continue;
                double alt = uDist + v.getWeight();
                double oldDist = dist.getOrDefault(v, Double.POSITIVE_INFINITY);
                if (alt < oldDist) {
                    pq.remove(v);
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.add(v);
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
    @Override
    public String getFinderName() {
        return "Dijkstra (Stable Deterministic)";
    }
}


