import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Handshake {
    private final String masterHost;
    private final int masterPort;
    private final int replicaPort; 

    public Handshake(String masterHost, int masterPort, int replicaPort) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.replicaPort = replicaPort;
    }

    public void start() {
        try (Socket masterSocket = new Socket(masterHost, masterPort)) {
            OutputStream out = masterSocket.getOutputStream();
            InputStream in = masterSocket.getInputStream();
            Formatter fmt = new Formatter();

            // 1. Send PING
            String pingCmd = fmt.formatArray(java.util.List.of("PING"));
            out.write(pingCmd.getBytes("UTF-8"));
            out.flush();
            readLine(in);

            // 2. Send REPLCONF listening-port <PORT>
            String replconfPortCmd = fmt.formatArray(java.util.List.of("REPLCONF", "listening-port", String.valueOf(replicaPort)));
            out.write(replconfPortCmd.getBytes("UTF-8"));
            out.flush();
            readLine(in); // Read +OK

            // 3. Send REPLCONF capa eof capa psync2
            String replconfCapaCmd = fmt.formatArray(java.util.List.of("REPLCONF", "capa", "eof", "capa", "psync2"));
            out.write(replconfCapaCmd.getBytes("UTF-8"));
            out.flush();
            readLine(in);

            String psyncCmd = fmt.formatArray(java.util.List.of("PSYNC", "?", "-1"));
            out.write(psyncCmd.getBytes("UTF-8"));
            out.flush();
            readLine(in); // Response is +FULLRESYNC <REPL_ID> 0\r\n (ignore for now)

            System.out.println("[REPLICA] Handshake completed with master at " + masterHost + ":" + masterPort);
        } catch (Exception e) {
            System.err.println("[REPLICA] Handshake failed: " + e.getMessage());
        }
    }

    private void readLine(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            sb.append((char)c);
            if (c == '\n') break;
        }
    }
}