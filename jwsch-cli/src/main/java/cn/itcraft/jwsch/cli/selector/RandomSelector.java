package cn.itcraft.jwsch.cli.selector;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * Random node selector.
 *
 * <p>Selects a random node from the candidate list.
 * Simple load distribution without tracking connection state.
 */
public class RandomSelector implements NodeSelector {

    private final Random random = new Random();

    /**
     * Selects a node address using random strategy.
     *
     * @param candidates list of candidate addresses
     * @return randomly selected address, or null if candidates is empty
     */
    @Override
    public InetSocketAddress select(List<InetSocketAddress> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int index = random.nextInt(candidates.size());
        return candidates.get(index);
    }

    /**
     * No-op for random selector.
     *
     * @param address the address that succeeded
     */
    @Override
    public void onConnectSuccess(InetSocketAddress address) {
    }

    /**
     * No-op for random selector.
     *
     * @param address the address that failed
     */
    @Override
    public void onConnectFailed(InetSocketAddress address) {
    }
}