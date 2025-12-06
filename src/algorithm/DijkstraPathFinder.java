package algorithm;

import core.Cell;
import core.Grid;
import core.Path;
import core.PathRequest;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;

public class DijkstraPathFinder implements PathFinder {

    @Override
    public Path findPath(PathRequest request) {
        Grid grid = request.getGrid();
        Cell startCell = request.getStartCell();
        Cell goalCell = request.getGoalCell();
        grid.resetAllCells();
        PriorityQueue<Cell> openSet = new PriorityQueue<>();
        startCell.setPathCost(0);
        openSet.add(startCell);
        while (!openSet.isEmpty()) {
            Cell current = openSet.poll();
            if (current.equals(goalCell)) {
                return reconstructPath(startCell, goalCell);
            }
            for (Cell neighbor : grid.getNeighbors(current)) {
                double newCost = current.getPathCost() + neighbor.getWeight();
                if (newCost < neighbor.getPathCost()) {
                    neighbor.setPathCost(newCost);
                    neighbor.setPredecessor(current);
                    if (openSet.contains(neighbor)) {
                        openSet.remove(neighbor);
                    }
                    openSet.add(neighbor);
                }
            }
        }
        return Path.notFound();
    }
    @Override
    public String getFinderName() {
        return "Dijkstra's Algorithm (Sequential)";
    }

    private Path reconstructPath(Cell start, Cell goal) {
        List<Cell> path = new ArrayList<>();
        Cell current = goal;
        double totalCost = goal.getPathCost();
        while (current != null) {
            path.add(current);
            if (current.equals(start)) break;
            current = current.getPredecessor();
        }
        Collections.reverse(path);
        if (path.isEmpty() || !path.get(0).equals(start)) {
            System.err.println("Path reconstruction failed. Start not found in path.");
            return Path.notFound();
        }

        return new Path(path, totalCost);
    }
}

