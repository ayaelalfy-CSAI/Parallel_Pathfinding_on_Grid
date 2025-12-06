package algorithm;

import core.Path;
import core.PathRequest;

public interface PathFinder {

    Path findPath(PathRequest request);
    String getFinderName();
}