package cn.itcraft.jwsch.common.protocol;

import cn.itcraft.jwsch.common.config.TcpConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.*;

public class PacketSizeLimitTest {

    private static Packet buildPacket(int bodyLen) {
        PacketHeader header = new PacketHeader.Builder()
            .command(Command.PUSH)
            .topic("/t")
            .bodyLength(bodyLen)
            .build();
        ByteBuf body = bodyLen > 0 ? Unpooled.buffer(bodyLen).writeZero(bodyLen) : null;
        return new Packet(header, body);
    }

    @Test
    public void testHardLimitClamp() {
        assertEquals(ProtocolConsts.MAX_PACKET_LENGTH_LIMIT, TcpConfig.normalizePacketLimit(5 * 1024 * 1024));
        assertEquals(ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH, TcpConfig.normalizePacketLimit(-1));
        assertEquals(1024, TcpConfig.normalizePacketLimit(1024));
        assertEquals(ProtocolConsts.MAX_PACKET_LENGTH_LIMIT, TcpConfig.normalizePacketLimit(Integer.MAX_VALUE));
    }

    @Test
    public void testTcpConfigSoftLimitClamp() {
        TcpConfig cfg = new TcpConfig();
        assertEquals(ProtocolConsts.DEFAULT_MAX_PACKET_LENGTH, cfg.getMaxPacketLength());
        cfg.setMaxPacketLength(2 * 1024 * 1024);
        assertEquals(ProtocolConsts.MAX_PACKET_LENGTH_LIMIT, cfg.getMaxPacketLength());
        cfg.setMaxPacketLength(64 * 1024);
        assertEquals(64 * 1024, cfg.getMaxPacketLength());
    }

    @Test
    public void testDecoderDropsOversizeButKeepsChannel() {
        int limit = 100;
        EmbeddedChannel ch = new EmbeddedChannel(new PacketDecoder(limit));

        Packet tooBig = buildPacket(300);
        Packet ok = buildPacket(10);
        ByteBuf rawBig = PacketWriter.write(tooBig, UnpooledByteBufAllocator.DEFAULT);
        ByteBuf rawOk = PacketWriter.write(ok, UnpooledByteBufAllocator.DEFAULT);
        ByteBuf combined = Unpooled.wrappedBuffer(rawBig, rawOk);

        ch.writeInbound(combined);

        Packet decoded = ch.readInbound();
        assertNotNull("small packet should pass", decoded);
        assertEquals(10, decoded.getBodyBuf().readableBytes());
        assertNull("oversize packet should be dropped", ch.readInbound());
        assertTrue("channel should stay open", ch.isOpen());
        ch.finishAndReleaseAll();
    }

    @Test
    public void testDecoderClampsOversizedLimit() {
        EmbeddedChannel ch = new EmbeddedChannel(new PacketDecoder(Integer.MAX_VALUE));
        Packet ok = buildPacket(10);
        ch.writeInbound(PacketWriter.write(ok, UnpooledByteBufAllocator.DEFAULT));
        // limit clamped to 500K hard cap, normal packet still decodes
        Packet decoded = ch.readInbound();
        assertNotNull(decoded);
        ch.finishAndReleaseAll();
    }

    @Test
    public void testEncoderDropsOversizeOnWrite() {
        int limit = 100;
        EmbeddedChannel ch = new EmbeddedChannel(new PacketEncoder(limit));

        ch.writeOutbound(buildPacket(300));
        assertEquals(0, ch.outboundMessages().size());

        ch.writeOutbound(buildPacket(10));
        Object sent = ch.readOutbound();
        assertTrue(sent instanceof ByteBuf);
        ch.finishAndReleaseAll();
    }
}
