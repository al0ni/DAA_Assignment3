package tests;

import algorithms.Condensation;
import algorithms.DSU;
import algorithms.DagPaths;
import algorithms.MST;
import algorithms.SCC;
import algorithms.Topological;
import benchmark.Benchmark;
import benchmark.Generator;
import benchmark.Main;
import graph.Edge;
import graph.Graph;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

public class AlgorithmsTest {
    @Test
    void dsuMatchesSimpleLabels() {
        Random random = new Random(17);
        Metrics metrics = new Metrics();
        DSU dsu = new DSU(100, metrics);
        int[] label = new int[100];
        for (int i = 0; i < 100; i++) label[i] = i;
        int components = 100;
        int successful = 0;
        for (int step = 0; step < 1000; step++) {
            int a = random.nextInt(100);
            int b = random.nextInt(100);
            boolean different = label[a] != label[b];
            assertEquals(different, dsu.union(a, b));
            if (different) {
                int old = label[b];
                int replacement = label[a];
                for (int i = 0; i < 100; i++) {
                    if (label[i] == old) label[i] = replacement;
                }
                components--;
                successful++;
            }
            assertEquals(components, dsu.components());
            for (int i = 0; i < 100; i++) {
                assertEquals(label[a] == label[i], dsu.find(a) == dsu.find(i));
            }
        }
        assertEquals(successful, metrics.unionCalls);
        assertEquals(202000, metrics.findCalls);
        dsu.makeSet(5);
        assertEquals(5, dsu.components());
        assertNotEquals(dsu.find(0), dsu.find(1));
        assertEquals(0, new DSU(0, new Metrics()).components());
    }

    @Test
    void mstAgreesOn100ConnectedGraphs() {
        Random random = new Random(23);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(60);
            Graph graph = new Graph(n, false);
            for (int i = 1; i < n; i++) {
                graph.addEdge(random.nextInt(i), i, random.nextInt(101) - 50);
            }
            for (int i = 0; i < n * 4; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(101) - 50);
            }
            MST a = MST.kruskal(graph, new Metrics());
            MST b = MST.prim(graph, new Metrics());
            assertEquals(a.weight, b.weight);
            assertTree(graph, a);
            assertTree(graph, b);
        }
    }

    private void assertTree(Graph graph, MST tree) {
        assertEquals(1, tree.components);
        assertEquals(graph.n - 1, tree.edges.size());
        DSU dsu = new DSU(graph.n, new Metrics());
        long total = 0;
        for (Edge edge : tree.edges) {
            assertTrue(dsu.union(edge.from, edge.to));
            boolean exists = false;
            for (Edge original : graph.edges) {
                boolean same = original.from == edge.from && original.to == edge.to;
                boolean reverse = original.from == edge.to && original.to == edge.from;
                if (original.weight == edge.weight && (same || reverse)) exists = true;
            }
            assertTrue(exists);
            total += edge.weight;
        }
        assertEquals(tree.weight, total);
        assertEquals(1, dsu.components());
    }

    @Test
    void mstMatchesExhaustiveSearch() {
        Random random = new Random(81);
        for (int trial = 0; trial < 30; trial++) {
            Graph graph = new Graph(5, false);
            for (int i = 1; i < 5; i++) graph.addEdge(i - 1, i, random.nextInt(21) - 10);
            for (int i = 0; i < 5; i++) {
                graph.addEdge(random.nextInt(5), random.nextInt(5), random.nextInt(21) - 10);
            }
            long best = Long.MAX_VALUE;
            for (int mask = 0; mask < (1 << graph.edges.size()); mask++) {
                if (Integer.bitCount(mask) != 4) continue;
                DSU dsu = new DSU(5, new Metrics());
                long weight = 0;
                boolean tree = true;
                for (int i = 0; i < graph.edges.size(); i++) {
                    if ((mask & (1 << i)) == 0) continue;
                    Edge edge = graph.edges.get(i);
                    if (!dsu.union(edge.from, edge.to)) tree = false;
                    weight += edge.weight;
                }
                if (tree && dsu.components() == 1) best = Math.min(best, weight);
            }
            assertEquals(best, MST.kruskal(graph, new Metrics()).weight);
            assertEquals(best, MST.prim(graph, new Metrics()).weight);
        }
    }

    @Test
    void fileGraphsAndEdgeCases() throws Exception {
        Graph disconnected = Graph.read(Path.of("data/disconnected.txt"));
        MST a = MST.kruskal(disconnected, new Metrics());
        MST b = MST.prim(disconnected, new Metrics());
        assertEquals(6, a.weight);
        assertEquals(4, a.components);
        assertEquals(3, a.edges.size());
        assertEquals(a.weight, b.weight);
        assertEquals(a.components, b.components);
        Graph cyclic = Graph.read(Path.of("data/cyclic.txt"));
        SCC components = SCC.kosaraju(cyclic, new Metrics());
        assertEquals(4, components.count);
        assertTrue(Main.samePartition(components, SCC.tarjan(cyclic, new Metrics())));
        assertThrows(IllegalArgumentException.class, () -> Topological.kahn(cyclic, new Metrics()));
        Graph dag = Graph.read(Path.of("data/dag.txt"));
        assertArrayEquals(new long[]{0, 0, 2, -3, -2, 1, 0},
                DagPaths.shortest(dag, new Metrics()).distance);
        assertEquals(List.of(1, 2, 3, 4, 5), DagPaths.shortest(dag, new Metrics()).pathTo(5));
        Graph empty = new Graph(0, false);
        assertEquals(0, MST.prim(empty, new Metrics()).weight);
        assertEquals(0, MST.kruskal(empty, new Metrics()).components);
        Graph emptyDirected = new Graph(0, true);
        assertEquals(0, SCC.kosaraju(emptyDirected, new Metrics()).count);
        assertEquals(0, SCC.tarjan(emptyDirected, new Metrics()).count);
        assertEquals(0, DagPaths.shortest(emptyDirected, new Metrics()).sum());
        Graph loop = new Graph(1, true);
        loop.addEdge(0, 0, -3);
        assertEquals(1, SCC.tarjan(loop, new Metrics()).count);
        assertThrows(IllegalArgumentException.class, () -> DagPaths.shortest(loop, new Metrics()));
        Graph largeWeights = new Graph(3, true);
        largeWeights.addEdge(0, 1, Integer.MAX_VALUE);
        largeWeights.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(4294967294L, DagPaths.shortest(largeWeights, new Metrics()).distance[2]);
    }

    private boolean[][] reachability(Graph graph) {
        boolean[][] reach = new boolean[graph.n][graph.n];
        for (int start = 0; start < graph.n; start++) {
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(start);
            reach[start][start] = true;
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (Edge edge : graph.adj.get(vertex)) {
                    if (!reach[start][edge.to]) {
                        reach[start][edge.to] = true;
                        queue.add(edge.to);
                    }
                }
            }
        }
        return reach;
    }

    @Test
    void bothSccAlgorithmsMatchBruteForce() {
        Random random = new Random(56);
        for (int trial = 0; trial < 100; trial++) {
            int n = 1 + random.nextInt(200);
            Graph graph = new Graph(n, true);
            for (int i = 0; i < n * 3; i++) {
                graph.addEdge(random.nextInt(n), random.nextInt(n), random.nextInt(21) - 10);
            }
            boolean[][] reach = reachability(graph);
            Metrics first = new Metrics();
            Metrics second = new Metrics();
            SCC a = SCC.kosaraju(graph, first);
            SCC b = SCC.tarjan(graph, second);
            int expectedCount = 0;
            for (int i = 0; i < n; i++) {
                boolean firstVertex = true;
                for (int j = 0; j < n; j++) {
                    boolean same = reach[i][j] && reach[j][i];
                    assertEquals(same, a.component[i] == a.component[j]);
                    assertEquals(same, b.component[i] == b.component[j]);
                    if (j < i && same) firstVertex = false;
                }
                if (firstVertex) expectedCount++;
            }
            assertEquals(expectedCount, a.count);
            assertEquals(expectedCount, b.count);
            assertEquals(2L * graph.edges.size(), first.edgeScans);
            assertEquals(graph.edges.size(), second.edgeScans);
            Graph condensed = Condensation.build(graph, a);
            assertTrue(Topological.valid(condensed, Topological.kahn(condensed, new Metrics())));
        }
    }

    @Test
    void topologicalValidationRejectsBadOrders() {
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        assertTrue(Topological.valid(graph, new int[]{0, 1, 2}));
        assertFalse(Topological.valid(graph, new int[]{0, 0, 2}));
        assertFalse(Topological.valid(graph, new int[]{2, 1, 0}));
        assertFalse(Topological.valid(graph, new int[]{0, 1}));
        assertFalse(Topological.valid(graph, new int[]{0, 1, 3}));
        graph.addEdge(2, 0, 1);
        assertThrows(IllegalArgumentException.class, () -> Topological.kahn(graph, new Metrics()));
    }

    @Test
    void dagPathsMatchBellmanFordWithNegativeWeights() {
        Random random = new Random(62);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(45);
            Graph graph = Generator.generate(n, n * 4, "dag");
            Graph weighted = new Graph(n, true);
            for (Edge edge : graph.edges) weighted.addEdge(edge.from, edge.to, random.nextInt(101) - 50);
            Metrics metrics = new Metrics();
            int[] order = Topological.kahn(weighted, metrics);
            assertTrue(Topological.valid(weighted, order));
            assertEquals(weighted.edges.size(), metrics.edgeScans);
            DagPaths shortest = DagPaths.shortest(weighted, new Metrics());
            DagPaths longest = DagPaths.longest(weighted, new Metrics());
            assertArrayEquals(bellmanFord(weighted, false), shortest.distance);
            assertArrayEquals(bellmanFord(weighted, true), longest.distance);
            checkPaths(weighted, shortest);
            checkPaths(weighted, longest);
        }
    }

    private long[] bellmanFord(Graph graph, boolean longest) {
        long infinity = longest ? Long.MIN_VALUE : Long.MAX_VALUE;
        long[] distance = new long[graph.n];
        Arrays.fill(distance, infinity);
        int[] degree = graph.indegrees();
        for (int i = 0; i < graph.n; i++) {
            if (degree[i] == 0) distance[i] = 0;
        }
        for (int pass = 1; pass < graph.n; pass++) {
            for (Edge edge : graph.edges) {
                if (distance[edge.from] == infinity) continue;
                long candidate = distance[edge.from] + edge.weight;
                if ((longest && candidate > distance[edge.to]) ||
                        (!longest && candidate < distance[edge.to])) distance[edge.to] = candidate;
            }
        }
        return distance;
    }

    private void checkPaths(Graph graph, DagPaths paths) {
        int[] degree = graph.indegrees();
        for (int end = 0; end < graph.n; end++) {
            List<Integer> path = paths.pathTo(end);
            assertEquals(0, degree[path.get(0)]);
            assertEquals(end, path.get(path.size() - 1));
            assertTrue(path.size() <= graph.n);
            for (int i = 1; i < path.size(); i++) {
                int from = path.get(i - 1);
                int to = path.get(i);
                boolean exists = false;
                for (Edge edge : graph.adj.get(from)) {
                    if (edge.to == to && paths.distance[from] + edge.weight == paths.distance[to]) {
                        exists = true;
                    }
                }
                assertTrue(exists);
            }
        }
    }

    @Test
    void bothSccAlgorithmsAreSafeOn100000VertexPath() {
        Graph graph = new Graph(100000, true);
        for (int i = 1; i < graph.n; i++) graph.addEdge(i - 1, i, 1);
        Metrics first = new Metrics();
        Metrics second = new Metrics();
        assertEquals(graph.n, SCC.kosaraju(graph, first).count);
        assertEquals(graph.n, SCC.tarjan(graph, second).count);
        assertEquals(graph.n, first.maxDepth);
        assertEquals(graph.n, second.maxDepth);
        graph.addEdge(graph.n - 1, 0, 1);
        assertEquals(1, SCC.kosaraju(graph, new Metrics()).count);
        assertEquals(1, SCC.tarjan(graph, new Metrics()).count);
    }

    @Test
    void condensationAndCriticalPath() throws Exception {
        Graph graph = Graph.read(Path.of("data/cyclic.txt"));
        SCC scc = SCC.kosaraju(graph, new Metrics());
        Graph condensed = Condensation.build(graph, scc);
        assertEquals(4, condensed.n);
        assertEquals(2, condensed.edges.size());
        DagPaths paths = DagPaths.longest(condensed, new Metrics());
        assertEquals(12, paths.distance[scc.component[5]]);
        assertEquals(List.of(scc.component[0], scc.component[3], scc.component[5]),
                paths.pathTo(scc.component[5]));
        Graph parallel = new Graph(3, true);
        parallel.addEdge(0, 1, 2);
        parallel.addEdge(0, 1, 8);
        parallel.addEdge(1, 2, -3);
        Graph reduced = Condensation.build(parallel, SCC.tarjan(parallel, new Metrics()));
        assertEquals(2, reduced.edges.size());
        long maximum = Long.MIN_VALUE;
        for (long distance : DagPaths.longest(reduced, new Metrics()).distance) {
            maximum = Math.max(maximum, distance);
        }
        assertEquals(8, maximum);
    }

    @Test
    void firstReferencePairMatches() {
        for (int density : new int[]{4, 20}) {
            int row = density == 4 ? 0 : 1;
            Graph roads = Generator.generate(1000, 1000 * density, "undirected");
            assertEquals(Benchmark.REFERENCES[row][0], MST.kruskal(roads, new Metrics()).weight);
            assertEquals(Benchmark.REFERENCES[row][0], MST.prim(roads, new Metrics()).weight);
            Graph directed = Generator.generate(1000, 1000 * density, "directed");
            assertEquals(Benchmark.REFERENCES[row][1], SCC.kosaraju(directed, new Metrics()).count);
            assertEquals(Benchmark.REFERENCES[row][1], SCC.tarjan(directed, new Metrics()).count);
            Graph dag = Generator.generate(1000, 1000 * density, "dag");
            assertEquals(Benchmark.REFERENCES[row][2], DagPaths.shortest(dag, new Metrics()).sum());
        }
    }
}
