package benchmark;

import algorithms.Condensation;
import algorithms.DagPaths;
import algorithms.SCC;
import algorithms.Topological;
import graph.Graph;
import metrics.Metrics;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class BonusBenchmark {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("results"));
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(Path.of("results/bonus.csv")))) {
            csv.println("n,m,kosaraju_scans,tarjan_scans,scc_count,condensation_edges,critical_weight,critical_path");
            for (int n : Benchmark.SIZES) {
                for (int density : new int[]{4, 20}) {
                    int m = n * density;
                    Graph graph = Generator.generate(n, m, "directed");
                    Metrics first = new Metrics();
                    Metrics second = new Metrics();
                    SCC a = SCC.kosaraju(graph, first);
                    SCC b = SCC.tarjan(graph, second);
                    Benchmark.check(Main.samePartition(a, b), "Bonus SCC partition mismatch");
                    Benchmark.check(first.edgeScans == 2L * m && second.edgeScans == m,
                            "Bonus SCC scan mismatch");
                    Graph condensed = Condensation.build(graph, a);
                    Benchmark.check(Topological.valid(condensed,
                            Topological.kahn(condensed, new Metrics())), "Condensation has a cycle");
                    DagPaths paths = DagPaths.longest(condensed, new Metrics());
                    int end = 0;
                    for (int i = 1; i < condensed.n; i++) {
                        if (paths.distance[i] > paths.distance[end]) end = i;
                    }
                    csv.printf("%d,%d,%d,%d,%d,%d,%d,\"%s\"%n", n, m,
                            first.edgeScans, second.edgeScans, a.count, condensed.edges.size(),
                            paths.distance[end], paths.pathTo(end));
                    csv.flush();
                    System.out.println("Bonus PASS: " + n + "/" + m);
                }
            }
        }
    }
}
