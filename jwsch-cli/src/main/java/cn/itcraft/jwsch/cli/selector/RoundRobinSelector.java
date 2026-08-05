package cn.itcraft.jwsch.cli.selector;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin node selector.
 *
 * <p>Selects nodes in round-robin order for even distribution.
 * Thread-safe implementation using AtomicInteger.
 */
public class RoundRobinSelector implements NodeSelector {

    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Selects a node address using round-robin strategy.
     *
     * @param candidates list of candidate addresses
     * @return selected address, or null if candidates is empty
     */
    @Override
    public InetSocketAddress select(List<InetSocketAddress> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int index = Math.abs(counter.getAndIncrement() % candidates.size());
        return candidates.get(index);
    }

    /**
     * No-op for round-robin selector.
     *
     * @param address the address that succeeded
     */
    @Override
    public void onConnectSuccess(InetSocketAddress address) {
    }

    /**
     * No-op for round-robin selector.
     *
     * @param address the address that failed
     */
    @Override
    public void onConnectFailed(InetSocketAddress address) {
    }
}