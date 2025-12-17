package core;

public class NodeCell implements Comparable<NodeCell> {
    public final Cell cell;
    public final double cost;
    public NodeCell(Cell cell, double cost) {
        this.cell = cell;
        this.cost = cost;
    }
    @Override
    public int compareTo(NodeCell other) {
        int cmp = Double.compare(this.cost, other.cost);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(this.cell.getRow(), other.cell.getRow());
        if (cmp != 0) return cmp;
        return Integer.compare(this.cell.getCol(), other.cell.getCol());
    }
}

