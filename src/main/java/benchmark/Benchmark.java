package benchmark;

import algorithms.DagPaths;
import algorithms.MST;
import algorithms.SCC;
import algorithms.Topological;
import graph.Graph;
import metrics.Metrics;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public class Benchmark {
    public static final int[] SIZES = {1000, 3162, 10000, 31623, 100000};
    public static final long[][] REFERENCES = {
        {147970, 27, 396274}, {30363, 1, 252411},
        {468911, 137, 1271911}, {97589, 1, 723157},
        {1486139, 373, 3941828}, {309550, 1, 2381342},
        {4715515, 1188, 12551891}, {968517, 1, 7376612},
        {15012094, 3923, 40096539}, {3056681, 1, 23512087}
    };

    private static long runAlgorithm(String name, Graph graph, Metrics metrics) {
        if (name.equals("Kruskal") || name.equals("Prim")) {
            MST tree;
            if (name.equals("Kruskal")) {
                tree = MST.kruskal(graph, metrics);
            } else {
                tree = MST.prim(graph, metrics);
            }
            check(tree.components == 1, "Generated graph must be connected");
            check(tree.edges.size() == graph.n - 1, "Wrong tree size");
            return tree.weight;
        }
        if (name.equals("Kosaraju")) {
            return SCC.kosaraju(graph, metrics).count;
        }
        if (name.equals("Kahn")) {
            int[] order = Topological.kahn(graph, metrics);
            check(Topological.valid(graph, order), "Invalid topological order");
            return order.length;
        }
        if (name.equals("DagShortestPath")) {
            return DagPaths.shortest(graph, metrics).sum();
        }
        throw new IllegalArgumentException("Unknown algorithm");
    }

    private static long measure(PrintWriter csv, String algorithm, String kind,
                                Graph graph, long expectedScans) {
        double[] times = new double[5];
        long result = 0;
        Metrics first = null;
        for (int run = 0; run < 5; run++) {
            Metrics metrics = new Metrics();
            long start = System.nanoTime();
            long value = runAlgorithm(algorithm, graph, metrics);
            times[run] = (System.nanoTime() - start) / 1_000_000.0;
            if (run == 0) {
                result = value;
                first = metrics;
            } else {
                check(value == result, "Result changes between runs");
                check(metrics.dsuOps() == first.dsuOps() &&
                      metrics.edgeScans == first.edgeScans &&
                      metrics.comparisons == first.comparisons &&
                      metrics.maxDepth == first.maxDepth, "Counters change between runs");
            }
            if (expectedScans >= 0) {
                check(metrics.edgeScans == expectedScans, algorithm + " scan count mismatch");
            }
        }
        Arrays.sort(times);
        csv.printf(Locale.US, "%s,%s,%d,%d,%.3f,%d,%d,%d,%d,%d%n",
                algorithm, kind, graph.n, graph.edges.size(), times[2],
                first.dsuOps(), first.edgeScans, first.comparisons, first.maxDepth, result);
        csv.flush();
        return result;
    }

    public static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void run() throws IOException {
        Files.createDirectories(Path.of("results"));
        int index = 0;
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(Path.of("results/results.csv")))) {
            csv.println("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");
            System.out.println("n,m,mst,scc,dag_sum,reference");
            for (int n : SIZES) {
                for (int density : new int[]{4, 20}) {
                    int m = density * n;
                    String label = density == 4 ? "sparse" : "dense";
                    Graph roads = Generator.generate(n, m, "undirected");
                    long kruskal = measure(csv, "Kruskal", "undirected_" + label, roads, -1);
                    long prim = measure(csv, "Prim", "undirected_" + label, roads, 2L * m);
                    check(kruskal == prim, "Kruskal and Prim disagree");
                    Graph directed = Generator.generate(n, m, "directed");
                    long scc = measure(csv, "Kosaraju", "directed_" + label, directed, 2L * m);
                    Graph dag = Generator.generate(n, m, "dag");
                    long ordered = measure(csv, "Kahn", "dag_" + label, dag, m);
                    check(ordered == n, "Missing vertices");
                    long sum = measure(csv, "DagShortestPath", "dag_" + label, dag, m);
                    long[] reference = REFERENCES[index++];
                    check(kruskal == reference[0] && scc == reference[1] && sum == reference[2],
                            "Reference mismatch at " + n + "/" + m + ": " +
                            kruskal + ", " + scc + ", " + sum);
                    System.out.printf("%d,%d,%d,%d,%d,PASS%n", n, m, kruskal, scc, sum);
                }
            }
        }
    }
}
