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
            String pingCmd = fmt.formatArray(java.util.List.of("PING"));
            out.write(pingCmd.getBytes("UTF-8"));
            out.flush();
            readLine(in);
            String replconfPortCmd = fmt.formatArray(java.util.List.of("REPLCONF", "listening-port", String.valueOf(replicaPort)));
            out.write(replconfPortCmd.getBytes("UTF-8"));
            out.flush();
            readLine(in); // Read +OK
            String replconfCapaCmd = fmt.formatArray(java.util.List.of("REPLCONF", "capa", "psync2"));
            out.write(replconfCapaCmd.getBytes("UTF-8"));
            out.flush();
            readLine(in);

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