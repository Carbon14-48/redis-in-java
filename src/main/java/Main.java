import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String dir = "/tmp";
        String dbfilename = "dump.rdb";
        int port = 6379;
        boolean isReplica = false;
        String replicaofHost = null;
        int replicaofPort = 0;
        for (int i = 0; i < args.length; i++) {
            if ("--dir".equals(args[i]) && i + 1 < args.length) {
                dir = args[++i];
            } else if ("--dbfilename".equals(args[i]) && i + 1 < args.length) {
                dbfilename = args[++i];
            } else if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--replicaof".equals(args[i]) && i + 1 < args.length) {
                isReplica = true;
                String[] parts = args[++i].split(" ");
                if (parts.length == 2) {
                    replicaofHost = parts[0];
                    replicaofPort = Integer.parseInt(parts[1]);
                }
            }
        }
        if (isReplica && replicaofHost != null && replicaofPort != 0) {
            new Handshake(replicaofHost, replicaofPort, port).start();
        }
        String rdbPath = dir + "/" + dbfilename;
        System.out.println("[MAIN] Loading RDB from: " + rdbPath);
        Map<String, ValueWithExpiry> rdbData = RdbLoader.loadRdbData(rdbPath);
        System.out.println("[MAIN] Loaded " + rdbData.size() + " keys");

        // Start server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("[MAIN] Server ready on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket,
                    new RedisConfig(dir, dbfilename), rdbData, isReplica)).start();
            }
        } catch (IOException e) {
            System.err.println("[MAIN] Server error: " + e.getMessage());
            System.exit(1);
        }
    }
}