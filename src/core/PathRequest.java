package core;

public class PathRequest {
    private final Grid grid;
    private final Cell startCell;
    private final Cell goalCell;
    private final int requestId;

    public PathRequest(int requestId, Grid grid, Cell startCell, Cell goalCell) {
        this.requestId = requestId;
        this.grid = grid;
        this.startCell = startCell;
        this.goalCell = goalCell;
        if (startCell == null || goalCell == null) {
            throw new IllegalArgumentException("Start and Goal cells must not be null.");
        }
    }
    public int getRequestId() { return requestId; }
    public Grid getGrid() { return grid; }
    public Cell getStartCell() { return startCell; }
    public Cell getGoalCell() { return goalCell; }

    @Override
    public String toString() {
        return String.format("Request %d: %s -> %s", requestId, startCell, goalCell);
    }
}