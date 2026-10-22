package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import java.util.ArrayDeque;
import java.util.Arrays;

public class Topological {
    public static int[] kahn(Graph graph, Metrics metrics) {
        if (!graph.directed) throw new IllegalArgumentException("Directed graph required");
        int[] degree = graph.indegrees();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < graph.n; i++) {
            if (degree[i] == 0) queue.add(i);
        }
        int[] order = new int[graph.n];
        int size = 0;
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            order[size++] = vertex;
            for (Edge edge : graph.adj.get(vertex)) {
                metrics.edgeScans++;
                if (--degree[edge.to] == 0) queue.add(edge.to);
            }
        }
        if (size != graph.n) throw new IllegalArgumentException("Graph has a cycle");
        return order;
    }

    public static boolean valid(Graph graph, int[] order) {
        if (!graph.directed || order.length != graph.n) return false;
        int[] position = new int[graph.n];
        Arrays.fill(position, -1);
        for (int i = 0; i < order.length; i++) {
            int vertex = order[i];
            if (vertex < 0 || vertex >= graph.n || position[vertex] != -1) return false;
            position[vertex] = i;
        }
        for (Edge edge : graph.edges) {
            if (position[edge.from] >= position[edge.to]) return false;
        }
        return true;
    }
}
