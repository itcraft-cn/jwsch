package cn.itcraft.jwsch.bench;

/**
 * 多进程 Benchmark 统一入口。
 * 
 * <p>通过第一个参数区分角色：server / publisher / subscriber。
 * 
 * <p>使用示例：
 * <pre>
 * java -jar jwsch-bench.jar server --wsPort 8080 --tcpPort 9090
 * java -jar jwsch-bench.jar subscriber --wsUrl ws://localhost:8080/ws --subscribers 50
 * java -jar jwsch-bench.jar publisher --host localhost --tcpPort 9090 --publishers 1
 * </pre>
 */
public final class BenchLauncher {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }
        
        String role = args[0];
        String[] roleArgs = new String[args.length - 1];
        System.arraycopy(args, 1, roleArgs, 0, args.length - 1);
        
        switch (role) {
            case "server":
                BenchServerMain.main(roleArgs);
                break;
            case "publisher":
                BenchPublisherMain.main(roleArgs);
                break;
            case "subscriber":
                BenchSubscriberMain.main(roleArgs);
                break;
            case "--help":
            case "-h":
                printHelp();
                break;
            default:
                System.err.println("Unknown role: " + role);
                printHelp();
                System.exit(1);
        }
    }
    
    private static void printHelp() {
        System.out.println("Usage: java -jar jwsch-bench.jar <role> [options]");
        System.out.println();
        System.out.println("Roles:");
        System.out.println("  server      Start Jwsch server (WebSocket + TCP)");
        System.out.println("  subscriber  Start WebSocket subscribers");
        System.out.println("  publisher   Start TCP publishers");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar jwsch-bench.jar server --wsPort 8080 --tcpPort 9090");
        System.out.println("  java -jar jwsch-bench.jar subscriber --subscribers 50 --topic /topic/bench");
        System.out.println("  java -jar jwsch-bench.jar publisher --publishers 1 --interval 50 --payloadSize 20480");
        System.out.println();
        System.out.println("For role-specific options, run: java -jar jwsch-bench.jar <role> --help");
    }
}