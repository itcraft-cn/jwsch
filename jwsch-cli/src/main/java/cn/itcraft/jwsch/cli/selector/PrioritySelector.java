package cn.itcraft.jwsch.cli.selector;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Priority node selector.
 *
 * <p>Always tries base-port node first. Falls back to other nodes if base-port fails.
 * Tracks failure count per node to avoid repeatedly connecting to failed nodes.
 *
 * <p>Selection strategy:
 * <ol>
 *   <li>Try base-port node first (assumed to be first in the list)</li>
 *   <li>If base-port has recent failures, try next available node</li>
 *   <li>Reset failure count on successful connection</li>
 * </ol>
 */
public class PrioritySelector implements NodeSelector {

    private static final int MAX_FAILURES = 3;

    private final ConcurrentHashMap<InetSocketAddress, AtomicInteger> failureCounts = new ConcurrentHashMap<>();

    @Override
    public InetSocketAddress select(List<InetSocketAddress> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        InetSocketAddress basePortAddress = candidates.get(0);
        AtomicInteger failures = failureCounts.get(basePortAddress);

        if (failures == null || failures.get() < MAX_FAILURES) {
            return basePortAddress;
        }

        for (int i = 1; i < candidates.size(); i++) {
            InetSocketAddress candidate = candidates.get(i);
            AtomicInteger candidateFailures = failureCounts.get(candidate);

            if (candidateFailures == null || candidateFailures.get() < MAX_FAILURES) {
                return candidate;
            }
        }

        return basePortAddress;
    }

    @Override
    public void onConnectSuccess(InetSocketAddress address) {
        AtomicInteger failures = failureCounts.get(address);
        if (failures != null) {
            failures.set(0);
        }
    }

    @Override
    public void onConnectFailed(InetSocketAddress address) {
        failureCounts.computeIfAbsent(address, k -> new AtomicInteger(0)).incrementAndGet();
    }
}