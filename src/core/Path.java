package core;

import java.util.List;

public class Path {
    private final List<Cell> cells;
    private final double totalCost;
    private final boolean found;
    public Path(List<Cell> cells, double totalCost) {
        this.cells = cells;
        this.totalCost = totalCost;
        this.found = true;
    }
    public static Path notFound() {
        return new Path(List.of(), 0.0, false);
    }
    private Path(List<Cell> cells, double totalCost, boolean found) {
        this.cells = cells;
        this.totalCost = totalCost;
        this.found = found;
    }
    public List<Cell> getCells() { return cells; }
    public double getTotalCost() { return totalCost; }
    public boolean isFound() { return found; }

    @Override
    public String toString() {
        if (!found) {
            return "Path not found.";
        }
        return String.format("Path found (Length: %d, Cost: %.2f)", cells.size(), totalCost);
    }
}
