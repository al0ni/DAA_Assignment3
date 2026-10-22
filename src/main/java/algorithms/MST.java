package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class MST {
    public final List<Edge> edges = new ArrayList<>();
    public long weight;
    public int components;

    public static MST kruskal(Graph graph, Metrics metrics) {
        if (graph.directed) throw new IllegalArgumentException("Undirected graph required");
        MST result = new MST();
        List<Edge> sorted = new ArrayList<>(graph.edges);
        sorted.sort((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight, b.weight);
        });
        DSU dsu = new DSU(graph.n, metrics);
        for (Edge edge : sorted) {
            if (dsu.components() <= 1) break;
            metrics.edgeScans++;
            if (dsu.find(edge.from) != dsu.find(edge.to)) {
                dsu.union(edge.from, edge.to);
                result.edges.add(edge);
                result.weight += edge.weight;
            }
        }
        result.components = dsu.components();
        return result;
    }

    public static MST prim(Graph graph, Metrics metrics) {
        if (graph.directed) throw new IllegalArgumentException("Undirected graph required");
        MST result = new MST();
        boolean[] used = new boolean[graph.n];
        PriorityQueue<Edge> queue = new PriorityQueue<>((a, b) -> {
            metrics.comparisons++;
            return Integer.compare(a.weight, b.weight);
        });
        for (int start = 0; start < graph.n; start++) {
            if (used[start]) continue;
            result.components++;
            queue.add(new Edge(start, start, 0));
            while (!queue.isEmpty()) {
                Edge edge = queue.remove();
                int vertex = edge.to;
                if (used[vertex]) continue;
                used[vertex] = true;
                if (edge.from != vertex) {
                    result.edges.add(edge);
                    result.weight += edge.weight;
                }
                for (Edge next : graph.adj.get(vertex)) {
                    metrics.edgeScans++;
                    if (!used[next.to]) queue.add(next);
                }
            }
        }
        return result;
    }
}
