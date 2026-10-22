package metrics;

public class Metrics {
    public long findCalls;
    public long unionCalls;
    public long edgeScans;
    public long comparisons;
    public int maxDepth;

    public long dsuOps() {
        return findCalls + unionCalls;
    }

    public void depth(int depth) {
        maxDepth = Math.max(maxDepth, depth);
    }
}
