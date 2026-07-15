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

    @Override
    public InetSocketAddress select(List<InetSocketAddress> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int index = Math.abs(counter.getAndIncrement() % candidates.size());
        return candidates.get(index);
    }

    @Override
    public void onConnectSuccess(InetSocketAddress address) {
    }

    @Override
    public void onConnectFailed(InetSocketAddress address) {
    }
}