package benchmark;

import algorithms.Condensation;
import algorithms.DagPaths;
import algorithms.MST;
import algorithms.SCC;
import algorithms.Topological;
import graph.Graph;
import metrics.Metrics;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("benchmark")) {
            Benchmark.run(args.length > 1 && args[1].equals("quick"));
            return;
        }
        Path file = Path.of(args.length == 0 ? "data/cyclic.txt" : args[0]);
        Graph graph = Graph.read(file);
        System.out.println("Graph: " + file + ", vertices: " + graph.n + ", edges: " + graph.edges.size());
        if (!graph.directed) {
            MST a = MST.kruskal(graph, new Metrics());
            MST b = MST.prim(graph, new Metrics());
            Benchmark.check(a.weight == b.weight, "MST mismatch");
            System.out.println("Forest weight: " + a.weight + ", components: " + a.components);
            return;
        }
        Metrics kosarajuMetrics = new Metrics();
        Metrics tarjanMetrics = new Metrics();
        SCC a = SCC.kosaraju(graph, kosarajuMetrics);
        SCC b = SCC.tarjan(graph, tarjanMetrics);
        Benchmark.check(samePartition(a, b), "SCC mismatch");
        System.out.println("SCC count: " + a.count);
        System.out.println("Kosaraju scans: " + kosarajuMetrics.edgeScans);
        System.out.println("Tarjan scans: " + tarjanMetrics.edgeScans);
        Graph condensed = Condensation.build(graph, a);
        DagPaths critical = DagPaths.longest(condensed, new Metrics());
        if (condensed.n > 0) {
            int end = 0;
            for (int i = 1; i < condensed.n; i++) {
                if (critical.distance[i] > critical.distance[end]) end = i;
            }
            System.out.println("Critical path weight: " + critical.distance[end]);
            System.out.println("Critical path SCC IDs: " + critical.pathTo(end));
        }
        int[] order;
        try {
            order = Topological.kahn(graph, new Metrics());
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
            return;
        }
        System.out.println("Topological order: " + Arrays.toString(order));
        DagPaths paths = DagPaths.shortest(graph, new Metrics());
        System.out.println("Distances: " + Arrays.toString(paths.distance));
        for (int i = 0; i < graph.n; i++) {
            System.out.println("Path to " + i + ": " + paths.pathTo(i));
        }
    }

    public static boolean samePartition(SCC a, SCC b) {
        if (a.count != b.count || a.component.length != b.component.length) return false;
        int[] forward = new int[a.count];
        int[] backward = new int[b.count];
        Arrays.fill(forward, -1);
        Arrays.fill(backward, -1);
        for (int i = 0; i < a.component.length; i++) {
            int x = a.component[i];
            int y = b.component[i];
            if ((forward[x] != -1 && forward[x] != y) ||
                    (backward[y] != -1 && backward[y] != x)) return false;
            forward[x] = y;
            backward[y] = x;
        }
        return true;
    }
}
