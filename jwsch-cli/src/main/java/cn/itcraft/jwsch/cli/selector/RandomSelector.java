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

    @Override
    public InetSocketAddress select(List<InetSocketAddress> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int index = random.nextInt(candidates.size());
        return candidates.get(index);
    }

    @Override
    public void onConnectSuccess(InetSocketAddress address) {
    }

    @Override
    public void onConnectFailed(InetSocketAddress address) {
    }
}