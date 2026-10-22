package algorithms;

import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import java.util.Arrays;

public class SCC {
    public final int[] component;
    public final int count;

    public SCC(int[] component, int count) {
        this.component = component;
        this.count = count;
    }

    public static SCC kosaraju(Graph graph, Metrics metrics) {
        if (!graph.directed) throw new IllegalArgumentException("Directed graph required");
        int n = graph.n;
        boolean[] visited = new boolean[n];
        int[] next = new int[n];
        int[] stack = new int[n];
        int[] order = new int[n];
        int finished = 0;
        for (int start = 0; start < n; start++) {
            if (visited[start]) continue;
            int depth = 1;
            stack[0] = start;
            visited[start] = true;
            metrics.depth(depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                if (next[vertex] < graph.adj.get(vertex).size()) {
                    Edge edge = graph.adj.get(vertex).get(next[vertex]++);
                    metrics.edgeScans++;
                    if (!visited[edge.to]) {
                        visited[edge.to] = true;
                        stack[depth++] = edge.to;
                        metrics.depth(depth);
                    }
                } else {
                    order[finished++] = vertex;
                    depth--;
                }
            }
        }
        Graph reversed = graph.reverse();
        int[] component = new int[n];
        Arrays.fill(component, -1);
        int count = 0;
        for (int i = n - 1; i >= 0; i--) {
            int start = order[i];
            if (component[start] != -1) continue;
            int depth = 1;
            stack[0] = start;
            component[start] = count;
            metrics.depth(depth);
            while (depth > 0) {
                int vertex = stack[--depth];
                for (Edge edge : reversed.adj.get(vertex)) {
                    metrics.edgeScans++;
                    if (component[edge.to] == -1) {
                        component[edge.to] = count;
                        stack[depth++] = edge.to;
                        metrics.depth(depth);
                    }
                }
            }
            count++;
        }
        return new SCC(component, count);
    }

    public static SCC tarjan(Graph graph, Metrics metrics) {
        if (!graph.directed) throw new IllegalArgumentException("Directed graph required");
        int n = graph.n;
        int[] index = new int[n];
        int[] low = new int[n];
        int[] next = new int[n];
        int[] parent = new int[n];
        int[] component = new int[n];
        int[] stack = new int[n];
        int[] active = new int[n];
        boolean[] onStack = new boolean[n];
        Arrays.fill(index, -1);
        Arrays.fill(parent, -1);
        Arrays.fill(component, -1);
        int time = 0;
        int activeSize = 0;
        int count = 0;
        for (int start = 0; start < n; start++) {
            if (index[start] != -1) continue;
            int depth = 1;
            stack[0] = start;
            index[start] = low[start] = time++;
            active[activeSize++] = start;
            onStack[start] = true;
            metrics.depth(depth);
            while (depth > 0) {
                int vertex = stack[depth - 1];
                if (next[vertex] < graph.adj.get(vertex).size()) {
                    Edge edge = graph.adj.get(vertex).get(next[vertex]++);
                    metrics.edgeScans++;
                    int to = edge.to;
                    if (index[to] == -1) {
                        parent[to] = vertex;
                        index[to] = low[to] = time++;
                        active[activeSize++] = to;
                        onStack[to] = true;
                        stack[depth++] = to;
                        metrics.depth(depth);
                    } else if (onStack[to]) {
                        low[vertex] = Math.min(low[vertex], index[to]);
                    }
                } else {
                    depth--;
                    if (parent[vertex] != -1) {
                        int p = parent[vertex];
                        low[p] = Math.min(low[p], low[vertex]);
                    }
                    if (low[vertex] == index[vertex]) {
                        int to;
                        do {
                            to = active[--activeSize];
                            onStack[to] = false;
                            component[to] = count;
                        } while (to != vertex);
                        count++;
                    }
                }
            }
        }
        return new SCC(component, count);
    }
}
