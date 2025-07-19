import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String dir = "/tmp";
        String dbfilename = "dump.rdb";
        
        for (int i = 0; i < args.length; i++) {
            if ("--dir".equals(args[i]) && i + 1 < args.length) {
                dir = args[++i];
            } else if ("--dbfilename".equals(args[i]) && i + 1 < args.length) {
                dbfilename = args[++i];
            }
        }

        String rdbPath = dir + "/" + dbfilename;
        System.out.println("[MAIN] Loading RDB from: " + rdbPath);
        Map<String, ValueWithExpiry> rdbData = RdbLoader.loadRdbData(rdbPath);
        System.out.println("[MAIN] Loaded " + rdbData.size() + " keys");

        try (ServerSocket serverSocket = new ServerSocket(6379)) {
            serverSocket.setReuseAddress(true);
            System.out.println("[MAIN] Server ready on port 6379");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket, 
                    new RedisConfig(dir, dbfilename), rdbData)).start();
            }
        } catch (IOException e) {
            System.err.println("[MAIN] Server error: " + e.getMessage());
            System.exit(1);
        }
    }
}