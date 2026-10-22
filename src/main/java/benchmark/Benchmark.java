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

    private interface Task {
        long run(Metrics metrics);
    }

    private static long measure(PrintWriter csv, String algorithm, String kind,
                                Graph graph, Task task, long expectedScans) {
        double[] times = new double[5];
        long result = 0;
        Metrics first = null;
        for (int run = 0; run < 5; run++) {
            Metrics metrics = new Metrics();
            long start = System.nanoTime();
            long value = task.run(metrics);
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

    public static void run(boolean small) throws IOException {
        Files.createDirectories(Path.of("results"));
        Path temporary = Path.of("results/results.pending.csv");
        int index = 0;
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(temporary))) {
            csv.println("algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result");
            System.out.println("n,m,mst,scc,dag_sum,reference");
            for (int n : SIZES) {
                if (small && n != 1000) break;
                for (int density : new int[]{4, 20}) {
                    int m = density * n;
                    String label = density == 4 ? "sparse" : "dense";
                    Graph roads = Generator.generate(n, m, "undirected");
                    long kruskal = measure(csv, "Kruskal", "undirected_" + label, roads, metrics -> {
                        MST tree = MST.kruskal(roads, metrics);
                        check(tree.components == 1 && tree.edges.size() == n - 1, "Invalid MST");
                        return tree.weight;
                    }, -1);
                    long prim = measure(csv, "Prim", "undirected_" + label, roads, metrics -> {
                        MST tree = MST.prim(roads, metrics);
                        check(tree.components == 1 && tree.edges.size() == n - 1, "Invalid MST");
                        return tree.weight;
                    }, 2L * m);
                    check(kruskal == prim, "Kruskal and Prim disagree");
                    Graph directed = Generator.generate(n, m, "directed");
                    long scc = measure(csv, "Kosaraju", "directed_" + label, directed,
                            metrics -> SCC.kosaraju(directed, metrics).count, 2L * m);
                    Graph dag = Generator.generate(n, m, "dag");
                    long ordered = measure(csv, "Kahn", "dag_" + label, dag, metrics -> {
                        int[] order = Topological.kahn(dag, metrics);
                        check(Topological.valid(dag, order), "Invalid topological order");
                        return order.length;
                    }, m);
                    check(ordered == n, "Missing vertices");
                    long sum = measure(csv, "DagShortestPath", "dag_" + label, dag,
                            metrics -> DagPaths.shortest(dag, metrics).sum(), m);
                    long[] reference = REFERENCES[index++];
                    check(kruskal == reference[0] && scc == reference[1] && sum == reference[2],
                            "Reference mismatch at " + n + "/" + m + ": " +
                            kruskal + ", " + scc + ", " + sum);
                    System.out.printf("%d,%d,%d,%d,%d,PASS%n", n, m, kruskal, scc, sum);
                }
            }
        }
        Path output = Path.of(small ? "results/quick.csv" : "results/results.csv");
        Files.move(temporary, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
