package engine;

import core.*;
import algorithm.DijkstraPathFinder;
import algorithm.PathFinder;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class PathfindingVisualizer extends JFrame {
    private Grid currentGrid;
    private GridPanel gridPanel;
    private JLabel statusLabel;
    private JLabel executionTimeLabel;
    private DefaultTableModel benchmarkTableModel;
    private DefaultTableModel pathTableModel;
    private boolean isAnimating = false;
    private JComboBox<String> executionModeCombo;
    private PathFinder pathFinder = new DijkstraPathFinder();
    private List<PathPair> pathPairs = new ArrayList<>();
    private Cell pendingStartCell = null;
    private Map<Integer, Set<Cell>> exploredCells = new ConcurrentHashMap<>();
    private Map<Integer, Cell> currentCells = new ConcurrentHashMap<>();
    private static final Color[] PATH_COLORS = {
            new Color(150, 0, 255),  // Blue
            new Color(255, 100, 150),  // Pink
            new Color(0, 255, 0),  // Light Green
            new Color(255, 200, 100),  // Orange
            new Color(0, 0, 255),  // Purple
            new Color(100, 255, 255),  // Cyan
            new Color(100, 100, 100),  // Yellow
            new Color(255, 0, 0)   // Light Red
    };
    private static final int[] THREAD_COUNTS = {2 ,4 ,6 ,8 ,10 ,12 ,14 ,16};
    private static final String[] BENCHMARK_TABLE_COLUMNS = {"Threads", "Seq Time (ms)", "Par Time (ms)", "Speedup", "Throughput (req/s)", "Accuracy"};
    private static final String[] PATH_TABLE_COLUMNS = {"#", "Start", "Goal", "Cost", "Length", "Status"};
    private static final String MODE_SEQUENTIAL = "Sequential";
    private static final String MODE_PARALLEL = "Parallel";
    public PathfindingVisualizer() {
        this.currentGrid = PathfindingExperiment.createVisualizationGrid();
        setTitle("Project 6 - Interactive Parallel Pathfinding (Multi-Path with Dijkstra)");
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
        gridPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleCellClick(e.getX() / cellSize, e.getY() / cellSize);
            }
        });
        JPanel controlPanel = createControlPanel();
        JPanel pathListPanel = createPathListPanel();
        statusLabel = new JLabel("Click on a cell to select START, then click another cell for GOAL.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        executionTimeLabel = new JLabel("Algorithm: " + pathFinder.getFinderName());
        executionTimeLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        executionTimeLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.add(statusLabel);
        statusPanel.add(executionTimeLabel);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(gridPanel, BorderLayout.CENTER);
        centerPanel.add(pathListPanel, BorderLayout.EAST);
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        return mainPanel;
    }
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.add(new JLabel("Execution Mode:"));
        executionModeCombo = new JComboBox<>(new String[]{MODE_SEQUENTIAL, MODE_PARALLEL});
        executionModeCombo.setToolTipText("Choose between sequential (one-by-one) or parallel (concurrent) execution");
        panel.add(executionModeCombo);
        JButton findAllButton = new JButton("Find All Paths");
        findAllButton.addActionListener(this::findAllPaths);
        findAllButton.setToolTipText("Execute pathfinding based on selected mode");
        panel.add(findAllButton);
        JButton clearAllButton = new JButton("Clear All Paths");
        clearAllButton.addActionListener(e -> {
            pathPairs.clear();
            pendingStartCell = null;
            exploredCells.clear();
            currentCells.clear();
            currentGrid.resetAllCells();
            updatePathTable();
            gridPanel.repaint();
            statusLabel.setText("All paths cleared. Click on a cell to select START.");
            executionTimeLabel.setText("Algorithm: " + pathFinder.getFinderName());
        });

        panel.add(clearAllButton);

        return panel;
    }

    private JPanel createPathListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Path List"));
        panel.setPreferredSize(new Dimension(300, 0));
        pathTableModel = new DefaultTableModel(PATH_TABLE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(pathTableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < pathPairs.size()) {
                pathPairs.remove(selectedRow);
                exploredCells.remove(selectedRow);
                currentCells.remove(selectedRow);
                updatePathTable();
                gridPanel.repaint();
                statusLabel.setText("Path removed. " + pathPairs.size() + " path(s) remaining.");
            }
        });
        panel.add(removeButton, BorderLayout.SOUTH);

        return panel;
    }
    private void handleCellClick(int col, int row) {
        if (isAnimating) {
            statusLabel.setText("Animation in progress. Please wait...");
            return;
        }

        if (row < 0 || row >= currentGrid.getRows() || col < 0 || col >= currentGrid.getCols()) {
            return;
        }

        Cell clickedCell = currentGrid.getCell(row, col);
        if (pendingStartCell == null) {
            pendingStartCell = clickedCell;
            statusLabel.setText(String.format("START selected at (%d, %d). Now click on a cell for GOAL.", row, col));
            gridPanel.repaint();
        } else {
            if (clickedCell.getRow() == pendingStartCell.getRow() &&
                    clickedCell.getCol() == pendingStartCell.getCol()) {
                statusLabel.setText("START and GOAL cannot be the same. Click on a different cell for GOAL.");
                return;
            }
            Color pathColor = PATH_COLORS[pathPairs.size() % PATH_COLORS.length];
            PathPair newPair = new PathPair(pendingStartCell, clickedCell, pathColor);
            pathPairs.add(newPair);
            pendingStartCell = null;
            updatePathTable();
            gridPanel.repaint();
            statusLabel.setText(String.format("Path #%d added: (%d,%d) → (%d,%d). Click for next START or 'Find All Paths'.",
                    pathPairs.size(),
                    newPair.start.getRow(), newPair.start.getCol(),
                    newPair.goal.getRow(), newPair.goal.getCol()));
        }
    }

    private void updatePathTable() {
        pathTableModel.setRowCount(0);
        for (int i = 0; i < pathPairs.size(); i++) {
            PathPair pair = pathPairs.get(i);
            String status = pair.path == null ? "Not computed" :
                    (pair.path.isFound() ? "Found" : "Not found");
            String cost = pair.path != null && pair.path.isFound() ?
                    String.format("%.2f", pair.path.getTotalCost()) : "-";
            String length = pair.path != null && pair.path.isFound() ?
                    String.valueOf(pair.path.getCells().size()) : "-";
            pathTableModel.addRow(new Object[]{
                    i + 1,
                    String.format("(%d,%d)", pair.start.getRow(), pair.start.getCol()),
                    String.format("(%d,%d)", pair.goal.getRow(), pair.goal.getCol()),
                    cost,
                    length,
                    status
            });
        }
    }
    private void findAllPaths(ActionEvent e) {
        if (isAnimating) {
            statusLabel.setText("Animation in progress. Please wait...");
            return;
        }
        if (pathPairs.isEmpty()) {
            statusLabel.setText("No paths to find. Click on cells to add start-goal pairs.");
            return;
        }
        String mode = (String) executionModeCombo.getSelectedItem();
        if (MODE_SEQUENTIAL.equals(mode)) {
            executeSequential();
        } else {
            executeParallel();
        }
    }

    private void executeSequential() {
        isAnimating = true;
        exploredCells.clear();
        currentCells.clear();
        statusLabel.setText("Finding paths SEQUENTIALLY using Dijkstra (one-by-one)...");
        executionTimeLabel.setText("Execution in progress...");
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws Exception {
                long startTime = System.nanoTime();
                for (int i = 0; i < pathPairs.size(); i++) {
                    PathPair pair = pathPairs.get(i);
                    exploredCells.put(i, new HashSet<>());
                    Grid gridCopy = createGridCopy();
                    Cell startCell = gridCopy.getCell(pair.start.getRow(), pair.start.getCol());
                    Cell goalCell = gridCopy.getCell(pair.goal.getRow(), pair.goal.getCol());
                    pair.path = findPathWithVisualization(gridCopy, startCell, goalCell, i);
                    Thread.sleep(500);
                    currentCells.remove(i);
                    exploredCells.get(i).clear();
                    gridPanel.repaint();
                }
                long endTime = System.nanoTime();
                return (endTime - startTime) / 1_000_000;
            }
            @Override
            protected void done() {
                try {
                    long executionTimeMs = get();
                    isAnimating = false;
                    updatePathTable();
                    gridPanel.repaint();
                    long foundCount = pathPairs.stream().filter(p -> p.path != null && p.path.isFound()).count();
                    statusLabel.setText(String.format("SEQUENTIAL execution complete! %d/%d paths found.",
                            foundCount, pathPairs.size()));
                    executionTimeLabel.setText(String.format("⏱ Sequential Time: %d ms | Algorithm: %s",
                            executionTimeMs, pathFinder.getFinderName()));
                } catch (Exception ex) {
                    statusLabel.setText("Error during sequential execution: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }.execute();
    }
    private void executeParallel() {
        isAnimating = true;
        exploredCells.clear();
        currentCells.clear();
        statusLabel.setText("Finding paths IN PARALLEL using Dijkstra (concurrent)...");
        executionTimeLabel.setText("Execution in progress...");
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws Exception {
                long startTime = System.nanoTime();
                List<PathRequest> requests = new ArrayList<>();
                for (int i = 0; i < pathPairs.size(); i++) {
                    PathPair pair = pathPairs.get(i);
                    exploredCells.put(i, Collections.synchronizedSet(new HashSet<>()));
                    Grid gridCopy = createGridCopy();
                    Cell startCell = gridCopy.getCell(pair.start.getRow(), pair.start.getCol());
                    Cell goalCell = gridCopy.getCell(pair.goal.getRow(), pair.goal.getCol());
                    requests.add(new PathRequest(i + 1, gridCopy, startCell, goalCell));
                }
                VisualizationCallback callback = (pathIndex, cell, isCurrent) -> {
                    if (isCurrent) {

                        currentCells.put(pathIndex, cell);
                    } else {

                        exploredCells.get(pathIndex).add(cell);
                    }
                    SwingUtilities.invokeLater(() -> gridPanel.repaint());
                    Thread.sleep(30);
                };
                int numThreads = Math.min(pathPairs.size(), Runtime.getRuntime().availableProcessors());
                ParallelPathfindingEngine engine = new ParallelPathfindingEngine(numThreads);
                List<Path> results = engine.processRequestsWithVisualization(requests, callback);
                for (int i = 0; i < results.size(); i++) {
                    pathPairs.get(i).path = results.get(i);
                }
                Thread.sleep(500);
                currentCells.clear();
                for (Set<Cell> cells : exploredCells.values()) {
                    cells.clear();
                }
                SwingUtilities.invokeLater(() -> gridPanel.repaint());
                long endTime = System.nanoTime();
                return (endTime - startTime) / 1_000_000;
            }

            @Override
            protected void done() {
                try {
                    long executionTimeMs = get();
                    isAnimating = false;
                    updatePathTable();
                    gridPanel.repaint();
                    long foundCount = pathPairs.stream()
                            .filter(p -> p.path != null && p.path.isFound())
                            .count();

                    statusLabel.setText(String.format("PARALLEL execution complete! %d/%d paths found.",
                            foundCount, pathPairs.size()));

                    executionTimeLabel.setText(String.format(
                            "⚡ Parallel Time: %d ms (using %d threads) | Algorithm: %s",
                            executionTimeMs,
                            Math.min(pathPairs.size(), Runtime.getRuntime().availableProcessors()),
                            pathFinder.getFinderName()));
                } catch (Exception ex) {
                    statusLabel.setText("Error during parallel execution: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }.execute();
    }
    private Path findPathWithVisualization(Grid grid, Cell start, Cell goal, int pathIndex) throws InterruptedException {
        grid.resetAllCells();
        PriorityQueue<NodeCell> openSet = new PriorityQueue<>();
        Set<Cell> visited = new HashSet<>();
        start.setPathCost(0);
        openSet.add(new NodeCell(start, 0));
        while (!openSet.isEmpty()) {
            NodeCell node = openSet.poll();
            Cell current = node.cell;
            if (visited.contains(current)) continue;
            visited.add(current);
            currentCells.put(pathIndex, current);
            exploredCells.get(pathIndex).add(current);
            SwingUtilities.invokeLater(() -> gridPanel.repaint());
            Thread.sleep(50);
            if (current.equals(goal)) {
                return reconstructPath(start, current);
            }
            for (Cell neighbor : grid.getNeighbors(current)) {
                if (visited.contains(neighbor)) continue;
                double newCost = current.getPathCost() + neighbor.getWeight();
                if (newCost < neighbor.getPathCost()) {
                    neighbor.setPathCost(newCost);
                    neighbor.setPredecessor(current);
                    openSet.add(new NodeCell(neighbor, newCost));
                }
            }
        }

        return Path.notFound();
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
    private Grid createGridCopy() {
        return new Grid(currentGrid);
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
                JOptionPane.showMessageDialog(PathfindingVisualizer.this,
                        "Benchmark Complete! Results added to table.",
                        "Finished",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    private static class PathResult {
        int index;
        Path path;
        PathResult(int index, Path path) {
            this.index = index;
            this.path = path;
        }
    }
    private class PathPair {
        Cell start;
        Cell goal;
        Color color;
        Path path;
        PathPair(Cell start, Cell goal, Color color) {
            this.start = start;
            this.goal = goal;
            this.color = color;
            this.path = null;
        }
    }
    private class GridPanel extends JPanel {
        private final int cellSize;
        public GridPanel(int cellSize) {
            this.cellSize = cellSize;
        }
        private Color blendColors(List<Color> colors) {
            if (colors.isEmpty()) return Color.WHITE;
            if (colors.size() == 1) return colors.get(0);
            int r = 0, g = 0, b = 0;
            for (Color color : colors) {
                r += color.getRed();
                g += color.getGreen();
                b += color.getBlue();
            }
            int count = colors.size();
            r /= count;
            g /= count;
            b /= count;
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            hsb[1] = Math.min(1.0f, hsb[1] * 1.3f);
            hsb[2] = Math.min(1.0f, hsb[2] * 1.1f);

            return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
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
                    int weight = cell.getWeight();
                    List<Color> cellColors = new ArrayList<>();
                    if (weight == 0) {

                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(x, y, cellSize, cellSize);
                    } else {

                        for (Map.Entry<Integer, Cell> entry : currentCells.entrySet()) {
                            Cell currentCell = entry.getValue();
                            if (currentCell.getRow() == r && currentCell.getCol() == c) {
                                PathPair pair = pathPairs.get(entry.getKey());
                                cellColors.add(brighten(pair.color, 1.5));
                            }
                        }
                        if (cellColors.isEmpty()) {
                            for (Map.Entry<Integer, Set<Cell>> entry : exploredCells.entrySet()) {
                                for (Cell exploredCell : entry.getValue()) {
                                    if (exploredCell.getRow() == r && exploredCell.getCol() == c) {
                                        PathPair pair = pathPairs.get(entry.getKey());
                                        cellColors.add(lighten(pair.color, 0.3));
                                        break;
                                    }
                                }
                            }
                        }

                        if (cellColors.isEmpty()) {
                            for (PathPair pair : pathPairs) {
                                if (pair.path != null && pair.path.isFound()) {
                                    for (Cell pathCell : pair.path.getCells()) {
                                        if (pathCell.getRow() == r && pathCell.getCol() == c) {
                                            cellColors.add(pair.color);
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (cellColors.isEmpty()) {
                            for (PathPair pair : pathPairs) {
                                if (r == pair.start.getRow() && c == pair.start.getCol()) {
                                    cellColors.add(darken(pair.color, 0.5));
                                    break;
                                } else if (r == pair.goal.getRow() && c == pair.goal.getCol()) {
                                    cellColors.add(darken(pair.color, 0.7));
                                    break;
                                }
                            }
                        }

                        if (cellColors.isEmpty() && pendingStartCell != null &&
                                r == pendingStartCell.getRow() && c == pendingStartCell.getCol()) {
                            cellColors.add(new Color(255, 165, 0));
                        }

                        Color finalColor;
                        if (cellColors.isEmpty()) {
                            float scale = 0.9f - (float) weight / 20f;
                            finalColor = new Color(scale, scale, scale);
                        } else if (cellColors.size() == 1) {
                            finalColor = cellColors.get(0);
                        } else {
                            finalColor = blendColors(cellColors);
                        }
                        g2d.setColor(finalColor);
                        g2d.fillRect(x, y, cellSize, cellSize);
                    }
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
        private Color darken(Color color, double factor) {
            return new Color(
                    Math.max(0, (int) (color.getRed() * factor)),
                    Math.max(0, (int) (color.getGreen() * factor)),
                    Math.max(0, (int) (color.getBlue() * factor))
            );
        }
        private Color lighten(Color color, double factor) {
            return new Color(
                    Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor)),
                    Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor)),
                    Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor))
            );
        }
        private Color brighten(Color color, double factor) {
            return new Color(
                    Math.min(255, (int) (color.getRed() * factor)),
                    Math.min(255, (int) (color.getGreen() * factor)),
                    Math.min(255, (int) (color.getBlue() * factor))
            );
        }
    }
}