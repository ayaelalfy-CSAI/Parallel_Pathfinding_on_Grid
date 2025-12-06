package core;

import java.util.Objects;

public class Cell implements Comparable<Cell> {
    private final int row;
    private final int col;
    private final int weight;
    private double pathCost = Double.MAX_VALUE;
    private Cell predecessor = null;

    public Cell(int row, int col, int weight) {
        this.row = row;
        this.col = col;
        this.weight = Math.max(1, weight); // Ensure weight is at least 1
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getWeight() { return weight; }
    public double getPathCost() { return pathCost; }
    public Cell getPredecessor() { return predecessor; }
    public void setPathCost(double pathCost) { this.pathCost = pathCost; }
    public void setPredecessor(Cell predecessor) { this.predecessor = predecessor; }
    public void reset() {
        this.pathCost = Double.MAX_VALUE;
        this.predecessor = null;
    }

    @Override
    public int compareTo(Cell other) {
        return Double.compare(this.pathCost, other.pathCost);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return row == cell.row && col == cell.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, W=%d)", row, col, weight);
    }
}
