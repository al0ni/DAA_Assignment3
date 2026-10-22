package graph;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Graph {
    public final int n;
    public final boolean directed;
    public final List<Edge> edges = new ArrayList<>();
    public final List<List<Edge>> adj = new ArrayList<>();

    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("Negative vertex count");
        this.n = n;
        this.directed = directed;
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    }

    public void addEdge(int from, int to, int weight) {
        if (from < 0 || from >= n || to < 0 || to >= n) {
            throw new IllegalArgumentException("Invalid vertex");
        }
        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adj.get(from).add(edge);
        if (!directed) adj.get(to).add(new Edge(to, from, weight));
    }

    public int[] indegrees() {
        int[] degree = new int[n];
        for (Edge edge : edges) degree[edge.to]++;
        return degree;
    }

    public Graph reverse() {
        if (!directed) throw new IllegalArgumentException("Directed graph required");
        Graph reversed = new Graph(n, true);
        for (Edge edge : edges) reversed.addEdge(edge.to, edge.from, edge.weight);
        return reversed;
    }

    public static Graph read(Path path) throws IOException {
        try (Scanner input = new Scanner(path)) {
            int n = input.nextInt();
            int m = input.nextInt();
            String kind = input.next();
            if (m < 0 || (!kind.equals("directed") && !kind.equals("undirected"))) {
                throw new IllegalArgumentException("Invalid graph header");
            }
            Graph graph = new Graph(n, kind.equals("directed"));
            for (int i = 0; i < m; i++) {
                graph.addEdge(input.nextInt(), input.nextInt(), input.nextInt());
            }
            if (input.hasNext()) throw new IllegalArgumentException("Extra input");
            return graph;
        }
    }
}
