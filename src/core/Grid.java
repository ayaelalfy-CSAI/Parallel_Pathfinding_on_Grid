package core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Grid {
    private final int rows;
    private final int cols;
    private final Cell[][] cells;
    private final double obstacleDensity; // 0.0 to 1.0
    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    public Grid(int rows, int cols, int maxWeight, double obstacleDensity) {
        this.rows = rows;
        this.cols = cols;
        this.obstacleDensity = obstacleDensity;
        this.cells = new Cell[rows][cols];
        initializeRandomGrid(maxWeight);
    }

    public Grid(Grid original) {
        this.rows = original.rows;
        this.cols = original.cols;
        this.obstacleDensity = original.obstacleDensity;
        this.cells = new Cell[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell originalCell = original.getCell(r, c);
                this.cells[r][c] = new Cell(originalCell.getRow(), originalCell.getCol(), originalCell.getWeight());
            }
        }
    }


    private void initializeRandomGrid(int maxWeight) {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int weight;
                if (rand.nextDouble() < obstacleDensity) {
                    weight = 0; // Obstacle/Blocked Cell
                } else {
                    // Weighted cell, 1 being the minimum cost
                    weight = rand.nextInt(maxWeight) + 1;
                }
                cells[r][c] = new Cell(r, c, weight);
            }
        }
    }

    public void resetAllCells() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c].reset();
            }
        }
    }
    public Cell getCell(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            return cells[row][col];
        }
        return null;
    }

    public boolean isWalkable(Cell cell) {
        return cell != null && cell.getWeight() > 0;
    }

    public List<Cell> getNeighbors(Cell cell) {
        List<Cell> neighbors = new ArrayList<>();

        for (int[] direction : DIRECTIONS) {
            int newRow = cell.getRow() + direction[0];
            int newCol = cell.getCol() + direction[1];

            Cell neighbor = getCell(newRow, newCol);

            if (isWalkable(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Grid (%dx%d, Density: %.2f)\n", rows, cols, obstacleDensity));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int w = cells[r][c].getWeight();
                // O for Obstacle, W for Weight
                sb.append(w == 0 ? " O " : String.format("%2d ", w));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
