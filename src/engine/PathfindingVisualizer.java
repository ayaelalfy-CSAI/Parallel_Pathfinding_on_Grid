package engine;

import core.Cell;
import core.Grid;
import core.Path;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Collections;

public class PathfindingVisualizer extends JFrame {
    private Grid currentGrid;
    private Path currentPath = Path.notFound();
    private Cell startCell;
    private Cell goalCell;
    private GridPanel gridPanel;
    private JTextField startRField, startCField, goalRField, goalCField;
    private JLabel statusLabel;
    private DefaultTableModel benchmarkTableModel;
    private Set<Cell> visitedCells = new HashSet<>();
    private Set<Cell> currentlyExploring = new HashSet<>();
    private boolean isAnimating = false;

    private static final int[] THREAD_COUNTS = {2, 4, 6, 8};
    private static final String[] BENCHMARK_TABLE_COLUMNS = {"Threads", "Seq Time (ms)", "Par Time (ms)", "Speedup", "Throughput (req/s)", "Accuracy"};

    public PathfindingVisualizer() {
        this.currentGrid = PathfindingExperiment.createVisualizationGrid();
        setTitle("Project 6 - Interactive Parallel Pathfinding");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Path Visualization", createVisualizationPanel());
        tabbedPane.addTab("Benchmark Summary", createBenchmarkPanel());
        add(tabbedPane);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createVisualizationPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        int cellSize = 30;
        gridPanel = new GridPanel(cellSize);
        gridPanel.setPreferredSize(new Dimension(currentGrid.getCols() * cellSize, currentGrid.getRows() * cellSize));

        JPanel controlPanel = createControlPanel();
        statusLabel = new JLabel("Grid created: 15x15. Enter coordinates (0-14) and click 'Find Path'.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(gridPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        startRField = new JTextField(2);
        startCField = new JTextField(2);
        goalRField = new JTextField(2);
        goalCField = new JTextField(2);
        startRField.setText("0");
        startCField.setText("0");
        goalRField.setText(String.valueOf(PathfindingExperiment.VIS_GRID_SIZE - 1));
        goalCField.setText(String.valueOf(PathfindingExperiment.VIS_GRID_SIZE - 1));
        panel.add(new JLabel("Start (R, C):"));
        panel.add(startRField);
        panel.add(startCField);
        panel.add(new JLabel("Goal (R, C):"));
        panel.add(goalRField);
        panel.add(goalCField);
        JButton findButton = new JButton("Find Path");
        findButton.addActionListener(this::runVisualizationPath);
        panel.add(findButton);

        return panel;
    }

    private void runVisualizationPath(ActionEvent e) {
        if (isAnimating) {
            statusLabel.setText("Animation in progress. Please wait...");
            return;
        }

        try {
            int startR = Integer.parseInt(startRField.getText());
            int startC = Integer.parseInt(startCField.getText());
            int goalR = Integer.parseInt(goalRField.getText());
            int goalC = Integer.parseInt(goalCField.getText());
            if (startR < 0 || startR >= currentGrid.getRows() ||
                    startC < 0 || startC >= currentGrid.getCols() ||
                    goalR < 0 || goalR >= currentGrid.getRows() ||
                    goalC < 0 || goalC >= currentGrid.getCols()) {
                statusLabel.setText("Error: Coordinates are out of bounds (0-" + (PathfindingExperiment.VIS_GRID_SIZE - 1) + ").");
                return;
            }
            visitedCells.clear();
            currentlyExploring.clear();
            currentPath = Path.notFound();
            startCell = currentGrid.getCell(startR, startC);
            goalCell = currentGrid.getCell(goalR, goalC);
            gridPanel.repaint();
            animatePathfinding(startR, startC, goalR, goalC);

        } catch (NumberFormatException ex) {
            statusLabel.setText("Error: Please enter valid integer coordinates.");
        }
    }

    private void animatePathfinding(int startR, int startC, int goalR, int goalC) {
        isAnimating = true;
        statusLabel.setText("Searching for path...");

        new SwingWorker<Path, Cell>() {
            @Override
            protected Path doInBackground() throws Exception {
                Grid grid = currentGrid;
                Cell start = grid.getCell(startR, startC);
                Cell goal = grid.getCell(goalR, goalC);
                grid.resetAllCells();
                PriorityQueue<Cell> openSet = new PriorityQueue<>();
                start.setPathCost(0);
                openSet.add(start);

                while (!openSet.isEmpty()) {
                    Cell current = openSet.poll();
                    publish(current);
                    Thread.sleep(100);
                    if (current.equals(goal)) {
                        return reconstructPath(start, goal);
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
            protected void process(List<Cell> chunks) {
                for (Cell cell : chunks) {
                    visitedCells.addAll(currentlyExploring);
                    currentlyExploring.clear();
                    currentlyExploring.add(cell);
                    gridPanel.repaint();
                }
            }

            @Override
            protected void done() {
                try {
                    currentPath = get();
                    visitedCells.addAll(currentlyExploring);
                    currentlyExploring.clear();

                    if (currentPath.isFound()) {
                        statusLabel.setText(String.format("Path found! Cost: %.2f, Length: %d",
                                currentPath.getTotalCost(), currentPath.getCells().size()));
                    } else {
                        statusLabel.setText("Path not found (obstacle or unreachable).");
                    }
                    gridPanel.repaint();
                } catch (Exception ex) {
                    statusLabel.setText("Error during pathfinding: " + ex.getMessage());
                } finally {
                    isAnimating = false;
                }
            }
        }.execute();
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
            return Path.notFound();
        }
        return new Path(path, totalCost);
    }

    private JPanel createBenchmarkPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        benchmarkTableModel = new DefaultTableModel(BENCHMARK_TABLE_COLUMNS, 0);
        JTable table = new JTable(benchmarkTableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton runButton = new JButton("Run Full Multi-Thread Benchmark (50 Requests)");
        runButton.addActionListener(this::runMultiThreadBenchmark);
        mainPanel.add(runButton, BorderLayout.SOUTH);
        return mainPanel;
    }

    private void runMultiThreadBenchmark(ActionEvent e) {
        benchmarkTableModel.setRowCount(0);
        new SwingWorker<List<BenchmarkResults>, BenchmarkResults>() {
            private final JButton sourceButton = (JButton) e.getSource();

            @Override
            protected List<BenchmarkResults> doInBackground() throws Exception {
                sourceButton.setEnabled(false);
                List<BenchmarkResults> allResults = new ArrayList<>();
                for (int threadCount : THREAD_COUNTS) {
                    BenchmarkResults result = PathfindingExperiment.runFullBenchmark(threadCount);
                    publish(result);
                    allResults.add(result);
                }

                return allResults;
            }

            @Override
            protected void process(List<BenchmarkResults> chunks) {
                for (BenchmarkResults result : chunks) {
                    benchmarkTableModel.addRow(new Object[]{
                            result.threadCount,
                            String.format("%.3f", result.sequentialTimeMs),
                            String.format("%.3f", result.parallelTimeMs),
                            String.format("%.2fx", result.calculateSpeedup()),
                            String.format("%.1f", result.calculateParallelThroughput()),
                            String.format("%d/%d", result.pathsFound, result.totalRequests)
                    });
                }
            }

            @Override
            protected void done() {
                sourceButton.setEnabled(true);
                JOptionPane.showMessageDialog(null,
                        "Benchmark Complete! Results added to table.",
                        "Finished",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    private class GridPanel extends JPanel {
        private final int cellSize;
        public GridPanel(int cellSize) {
            this.cellSize = cellSize;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));

            for (int r = 0; r < currentGrid.getRows(); r++) {
                for (int c = 0; c < currentGrid.getCols(); c++) {
                    Cell cell = currentGrid.getCell(r, c);
                    int x = c * cellSize;
                    int y = r * cellSize;
                    Color color;
                    int weight = cell.getWeight();

                    if (weight == 0) {
                        color = Color.BLACK; // Obstacle
                    } else if (cell.equals(startCell)) {
                        color = new Color(0, 150, 0); // Dark Green (Start)
                    } else if (cell.equals(goalCell)) {
                        color = Color.RED; // Red (Goal)
                    } else if (currentPath.isFound() && currentPath.getCells().contains(cell)) {
                        color = new Color(100, 150, 255); // Path Blue
                    } else if (currentlyExploring.contains(cell)) {
                        color = new Color(255, 255, 0); // Yellow (Currently testing)
                    } else if (visitedCells.contains(cell)) {
                        color = new Color(0, 200, 0); // Green (Selected/Visited)
                    } else {
                        // Weighted cells: scale brightness based on weight
                        float scale = 0.9f - (float) weight / (float) 20;
                        color = new Color(scale, scale, scale);
                    }

                    g2d.setColor(color);
                    g2d.fillRect(x, y, cellSize, cellSize);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.drawRect(x, y, cellSize, cellSize);
                    if (weight > 0) {
                        g2d.setColor(Color.WHITE);
                        String text = String.valueOf(weight);
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = x + (cellSize - fm.stringWidth(text)) / 2;
                        int textY = y + (cellSize - fm.getHeight()) / 2 + fm.getAscent();
                        g2d.drawString(text, textX, textY);
                    }
                }
            }
        }
    }
}