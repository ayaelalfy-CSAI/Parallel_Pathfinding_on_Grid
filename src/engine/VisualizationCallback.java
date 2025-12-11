package engine;

import core.Cell;


@FunctionalInterface
public interface VisualizationCallback {

    void onCellExplored(int pathIndex, Cell cell, boolean isCurrent) throws InterruptedException;
}
