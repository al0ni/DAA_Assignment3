package algorithms;

import graph.Edge;
import graph.Graph;
import java.util.HashMap;
import java.util.Map;

public class Condensation {
    public static Graph build(Graph graph, SCC scc) {
        if (!graph.directed) throw new IllegalArgumentException("Directed graph required");
        Graph result = new Graph(scc.count, true);
        Map<Long, Integer> weights = new HashMap<>();
        for (Edge edge : graph.edges) {
            int from = scc.component[edge.from];
            int to = scc.component[edge.to];
            if (from == to) continue;
            long key = ((long) from << 32) | (to & 0xffffffffL);
            Integer old = weights.get(key);
            if (old == null || edge.weight > old) weights.put(key, edge.weight);
        }
        for (Map.Entry<Long, Integer> entry : weights.entrySet()) {
            long key = entry.getKey();
            result.addEdge((int) (key >>> 32), (int) key, entry.getValue());
        }
        return result;
    }
}
