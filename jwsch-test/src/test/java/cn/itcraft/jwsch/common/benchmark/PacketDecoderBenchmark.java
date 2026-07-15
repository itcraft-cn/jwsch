package cn.itcraft.jwsch.common.benchmark;

import cn.itcraft.jwsch.common.protocol.Command;
import cn.itcraft.jwsch.common.protocol.Packet;
import cn.itcraft.jwsch.common.protocol.PacketDecoder;
import cn.itcraft.jwsch.common.protocol.PacketEncoder;
import cn.itcraft.jwsch.common.protocol.PacketHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class PacketDecoderBenchmark {

    private EmbeddedChannel decoderChannel;
    private ByteBuf normalPacketBuffer;
    private ByteBuf packetWithTopicBuffer;
    private ByteBuf packetWithLargeBodyBuffer;

    @Param({"1024", "4096", "16384"})
    private int largeBodySize;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PacketDecoderBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(5)
                .measurementIterations(10)
                .warmupTime(TimeValue.milliseconds(100))
                .measurementTime(TimeValue.milliseconds(100))
                .build();

        new Runner(opt).run();
    }

    @Setup(Level.Trial)
    public void setup() {
        EmbeddedChannel encoderChannel = new EmbeddedChannel(new PacketEncoder());

        PacketHeader normalHeader = new PacketHeader.Builder()
                .command(Command.REQUEST)
                .sourceId(12345L)
                .targetId(67890L)
                .build();
        ByteBuf normalBody = Unpooled.wrappedBuffer(new byte[128]);
        Packet normalPacket = new Packet(normalHeader, normalBody);
        encoderChannel.writeOutbound(normalPacket);
        normalPacketBuffer = encoderChannel.readOutbound();

        PacketHeader topicHeader = new PacketHeader.Builder()
                .command(Command.REQUEST)
                .sourceId(12345L)
                .targetId(67890L)
                .topic("test-topic-12345")
                .build();
        ByteBuf topicBody = Unpooled.wrappedBuffer(new byte[128]);
        Packet packetWithTopic = new Packet(topicHeader, topicBody);
        encoderChannel.writeOutbound(packetWithTopic);
        packetWithTopicBuffer = encoderChannel.readOutbound();

        PacketHeader largeHeader = new PacketHeader.Builder()
                .command(Command.REQUEST)
                .sourceId(12345L)
                .targetId(67890L)
                .build();
        ByteBuf largeBody = Unpooled.wrappedBuffer(new byte[largeBodySize]);
        Packet packetWithLargeBody = new Packet(largeHeader, largeBody);
        encoderChannel.writeOutbound(packetWithLargeBody);
        packetWithLargeBodyBuffer = encoderChannel.readOutbound();

        encoderChannel.finish();

        decoderChannel = new EmbeddedChannel(new PacketDecoder());
    }

    @Benchmark
    public void benchmarkDecode_normalPacket() {
        normalPacketBuffer.readerIndex(0);
        normalPacketBuffer.retain();
        decoderChannel.writeInbound(normalPacketBuffer);
        Packet packet = decoderChannel.readInbound();
        if (packet != null) {
            packet.release();
        }
    }

    @Benchmark
    public void benchmarkDecode_packetWithTopic() {
        packetWithTopicBuffer.readerIndex(0);
        packetWithTopicBuffer.retain();
        decoderChannel.writeInbound(packetWithTopicBuffer);
        Packet packet = decoderChannel.readInbound();
        if (packet != null) {
            packet.release();
        }
    }

    @Benchmark
    public void benchmarkDecode_packetWithLargeBody() {
        packetWithLargeBodyBuffer.readerIndex(0);
        packetWithLargeBodyBuffer.retain();
        decoderChannel.writeInbound(packetWithLargeBodyBuffer);
        Packet packet = decoderChannel.readInbound();
        if (packet != null) {
            packet.release();
        }
    }
}
