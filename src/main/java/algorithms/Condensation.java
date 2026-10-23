package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.HashMap;
import java.util.Map;

public class Condensation {
    public static Graph build(Graph graph, SCC scc) {
        if (!graph.directed) throw new IllegalArgumentException("Directed graph required");
        Graph result = new Graph(scc.count, true);
        Map<String, Edge> best = new HashMap<>();
        for (Edge edge : graph.edges) {
            int from = scc.component[edge.from];
            int to = scc.component[edge.to];
            if (from == to) continue;
            String key = from + " " + to;
            Edge old = best.get(key);
            if (old == null || edge.weight > old.weight) {
                best.put(key, new Edge(from, to, edge.weight));
            }
        }
        for (Edge edge : best.values()) {
            result.addEdge(edge.from, edge.to, edge.weight);
        }
        return result;
    }
}
