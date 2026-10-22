package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DagPaths {
    public final long[] distance;
    public final int[] parent;

    private DagPaths(int n) {
        distance = new long[n];
        parent = new int[n];
        Arrays.fill(parent, -1);
    }

    public static DagPaths shortest(Graph graph, Metrics metrics) {
        return solve(graph, false, metrics);
    }

    public static DagPaths longest(Graph graph, Metrics metrics) {
        return solve(graph, true, metrics);
    }

    private static DagPaths solve(Graph graph, boolean longest, Metrics metrics) {
        int[] order = Topological.kahn(graph, new Metrics());
        DagPaths result = new DagPaths(graph.n);
        long infinity = longest ? Long.MIN_VALUE : Long.MAX_VALUE;
        Arrays.fill(result.distance, infinity);
        int[] degree = graph.indegrees();
        for (int i = 0; i < graph.n; i++) {
            if (degree[i] == 0) result.distance[i] = 0;
        }
        for (int vertex : order) {
            for (Edge edge : graph.adj.get(vertex)) {
                metrics.edgeScans++;
                long candidate = result.distance[vertex] + edge.weight;
                if ((longest && candidate > result.distance[edge.to]) ||
                        (!longest && candidate < result.distance[edge.to])) {
                    result.distance[edge.to] = candidate;
                    result.parent[edge.to] = vertex;
                }
            }
        }
        return result;
    }

    public List<Integer> pathTo(int vertex) {
        List<Integer> path = new ArrayList<>();
        while (vertex != -1) {
            path.add(vertex);
            vertex = parent[vertex];
        }
        Collections.reverse(path);
        return path;
    }

    public long sum() {
        long total = 0;
        for (long value : distance) total += value;
        return total;
    }
}
