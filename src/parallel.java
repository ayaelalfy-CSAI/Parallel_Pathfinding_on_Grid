import algorithm.DijkstraPathFinder;
import algorithm.PathFinder;
import core.Cell;
import core.Grid;
import core.Path;
import core.PathRequest;

import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class parallel {
    public static void main(String[] args) {
        Grid grid = new Grid(15, 15, 10, 0.15);
        Cell start = grid.getCell(3, 1);
        Cell goal = grid.getCell(3, 7);

        PathFinder finder = new DijkstraPathFinder();

        System.out.println("Initial Grid:\n");
        printGrid(grid, null); // لا يوجد مسار بعد، لذا نمرر null
        System.out.println("\nTesting 5 runs...\n");

        for (int i = 0; i < 5; i++) {
            PathRequest req = new PathRequest(i+1, grid, start, goal);
            Path path = finder.findPath(req);
            System.out.printf("Run %d: Cost = %.2f, Length = %d\n",
                    i+1, path.getTotalCost(), path.getCells().size());
        }
    }

    /**
     * دالة لطباعة الشبكة مع تمييز المسار إذا تم تمريره
     */
    public static void printGrid(Grid grid, List<Cell> path) {
        for (int row = 0; row < grid.getRows(); row++) {
            for (int col = 0; col < grid.getCols(); col++) {
                Cell cell = grid.getCell(row, col);
                if (path != null && path.contains(cell)) {
                    System.out.print(" P "); // جزء من المسار
                } else if (cell.getWeight() > 1) {
                    System.out.print(" X "); // عقبة أو خلية ذات وزن مرتفع
                } else {
                    System.out.print(" . "); // خلية عادية
                }
            }
            System.out.println();
        }
    }


}