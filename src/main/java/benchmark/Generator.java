package benchmark;

import graph.Graph;
import java.util.Random;

public class Generator {
    public static Graph generate(int n, int m, String kind) {
        if (n < 1 || m < 0 || (n == 1 && m > 0)) {
            throw new IllegalArgumentException("Invalid generator size");
        }
        boolean undirected = kind.equals("undirected");
        boolean dag = kind.equals("dag");
        if (!undirected && !dag && !kind.equals("directed")) {
            throw new IllegalArgumentException("Unknown graph kind");
        }
        if (undirected && m < n - 1) throw new IllegalArgumentException("Too few edges");
        Random random = new Random(42);
        Graph graph = new Graph(n, !undirected);
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        if (undirected || dag) {
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = order[i];
                order[i] = order[j];
                order[j] = temp;
            }
        }
        if (undirected) {
            for (int i = 1; i < n; i++) {
                int from = order[random.nextInt(i)];
                graph.addEdge(from, order[i], 1 + random.nextInt(1000));
            }
        }
        while (graph.edges.size() < m) {
            int from = random.nextInt(n);
            int to = random.nextInt(n);
            while (from == to) to = random.nextInt(n);
            if (dag && order[from] > order[to]) {
                int temp = from;
                from = to;
                to = temp;
            }
            graph.addEdge(from, to, 1 + random.nextInt(1000));
        }
        return graph;
    }
}
