import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientHandler implements Runnable {
    Socket clientSocket;
    RespParser parser;
    RedisConfig config;
    Map<String, ValueWithExpiry> rdbData;
    RPUSH rpush= new RPUSH();
    boolean isReplica; 

   
    public ClientHandler(Socket clientSocket, RedisConfig config, Map<String, ValueWithExpiry> rdbData, boolean isReplica) {
        this.clientSocket = clientSocket;
        this.config = config;
        this.rdbData = rdbData;
        this.isReplica = isReplica;
    }
    
       private void handlePing(OutputStream out) throws IOException {
    out.write("+PONG\r\n".getBytes("UTF-8"));
}

private void handleEcho(List<String> cmd, OutputStream out) throws IOException {
    if (cmd.size() > 1) {
        String arg = cmd.get(1);
        Formatter fmt = new Formatter();
        out.write(fmt.formatBulkString(arg).getBytes("UTF-8"));
    }
}

private void handleSet(List<String> cmd, OutputStream out, SetGet store) throws IOException {
    String key = cmd.get(1);
    String value = cmd.get(2);
    Long pxMillis = null;
    for (int i = 3; i < cmd.size() - 1; i++) {
        if (cmd.get(i).equalsIgnoreCase("PX")) {
            try {
                pxMillis = Long.parseLong(cmd.get(i + 1));
            } catch (NumberFormatException e) {
                System.out.println("error formatting px value");
            }
            break;
        }
    }
    out.write(store.set(key, value, pxMillis).getBytes("UTF-8"));
}
private void handleGet(List<String> cmd, OutputStream out, SetGet store) throws IOException {
    if (cmd.size() < 2) {
        out.write("-ERR wrong args\r\n".getBytes());
        return;
    }

    String key = cmd.get(1);
    Formatter fmt = new Formatter();
    
    try {
        // Check memory store first
        String value = store.getValue(key);
        if (value == null) {
            // Check RDB data
            ValueWithExpiry vwe = rdbData.get(key);
            value = (vwe != null && !vwe.isExpired()) ? vwe.value : null;
        }
        out.write(fmt.formatBulkString(value).getBytes("UTF-8"));
    } catch (Exception e) {
        out.write("-ERR internal error\r\n".getBytes());
    }
}
private void handleType(OutputStream out, String key, SetGet store) throws IOException {
    String type = "none";
    ValueWithExpiry vwe = store.store.get(key);
    if (vwe == null || vwe.isExpired()) {
        vwe = rdbData.get(key);
        if (vwe == null || vwe.isExpired()) {
            type = "none";
        } else {
            type = "string"; 
        }
    } else {
        type = "string"; 
    }
    out.write(("+TYPE " + type + "\r\n").getBytes("UTF-8"));
}
private static final String MASTER_REPLID = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
private static final String MASTER_REPL_OFFSET = "0";


private void handlePsync(OutputStream out) throws IOException {
    String resp = "+FULLRESYNC " + MASTER_REPLID + " 0\r\n";
    out.write(resp.getBytes("UTF-8"));

    // Canonical empty RDB file (45 bytes)
    byte[] contents = java.util.HexFormat.of().parseHex(
        "5245444953303030360aff0000000000000000000000000000000000000000000000"
    );
    out.write(("$" + contents.length + "\r\n").getBytes("UTF-8"));
    out.write(contents); // No trailing \r\n
}
private void handleInfo(List<String> cmd, OutputStream out) throws IOException {
    if (cmd.size() > 1 && "replication".equalsIgnoreCase(cmd.get(1))) {
        StringBuilder info = new StringBuilder();
        info.append("role:").append(isReplica ? "slave" : "master").append("\r\n");
        if (!isReplica) { 
            info.append("master_replid:").append(MASTER_REPLID).append("\r\n");
            info.append("master_repl_offset:").append(MASTER_REPL_OFFSET).append("\r\n");
        }
        String resp = new Formatter().formatBulkString(info.toString());
        out.write(resp.getBytes("UTF-8"));
    } else {
        out.write("$0\r\n\r\n".getBytes("UTF-8"));
    }
}
private void handleReplconf(OutputStream out) throws IOException {
    out.write("+OK\r\n".getBytes("UTF-8"));
}

private void handleConfigGet(List<String> cmd, OutputStream out) throws IOException {
    Formatter fmt = new Formatter();
    String param = cmd.size() > 1 ? cmd.get(1) : "";
    String value = null;
    if ("dir".equalsIgnoreCase(param)) {
        value = config.getDir();
    } else if ("dbfilename".equalsIgnoreCase(param)) {
        value = config.getDbfilename();
    }
    out.write(fmt.formatBulkArray(param, value).getBytes("UTF-8"));
}

private void handleKeys(OutputStream out, List<String> cmd) throws IOException {
    Formatter fmt = new Formatter();
    if (cmd.size() > 1 && "*".equals(cmd.get(1))) {
        out.write(fmt.formatArray(rdbData.keySet()).getBytes("UTF-8"));
    } else {
        out.write(fmt.formatArray(Set.of()).getBytes("UTF-8"));
    }
}
public void run() {
  try {
      var in = clientSocket.getInputStream();
      var out = clientSocket.getOutputStream();
      RespParser parser = new RespParser(in);
      SetGet store = new SetGet();
      while (true) {
          try {
              var cmd = parser.parseArray();
              String command = cmd.get(0).toUpperCase();
              switch (command) {
                    case "PING":
                      handlePing(out);
                      break;
                    case "ECHO":
                      handleEcho(cmd, out);
                      break;
                    case "SET":
                      handleSet(cmd, out, store);
                      break;
                    case   "GET":
                      handleGet(cmd, out, store);
                      break;
                    case "CONFIG":
                      if (cmd.size() > 1 && "GET".equalsIgnoreCase(cmd.get(1))) {
                          handleConfigGet(cmd.subList(1, cmd.size()), out);
                      }
                      break;
                      case "KEYS":
                      handleKeys(out, cmd);
                      break;
                    case "RPUSH":
                    if(cmd.size()>2){
                        ArrayList<String > elements = new ArrayList<>();
                        for(int i =2; i<cmd.size();i++){
                            elements.add(cmd.get(i));
                        }
                      rpush.handleRPUSH(out , cmd.get(1), elements);

                    }
                    break;
                    case "LRANGE":
                    if( cmd.size()>2){
                        rpush.handleLRANGE(out, cmd.get(1), Integer.parseInt(cmd.get(2)),  Integer.parseInt(cmd.get(3)));
                    }

                    break;
                    case "LPUSH":
                    if(cmd.size()>2){
                        ArrayList<String > elements = new ArrayList<>();
                        for(int i =2; i<cmd.size();i++){
                            elements.add(cmd.get(i));
                        }
                      rpush.handleLPUSH(out , cmd.get(1), elements);

                    }
                    break;
                    case "LLEN":
                    if(cmd.size()>1){
                        rpush.handleLLEN(out, cmd.get(1));
                    }
                    break;
                    case "LPOP":
                        if(cmd.size() == 2) {
                            rpush.handleLPOP(out, cmd.get(1)); 
                        } else if(cmd.size() > 2) {
                            rpush.handleLPOP(out, cmd.get(1), Integer.parseInt(cmd.get(2)));
                        }
                    break;
                    case "BLPOP":
                    if (cmd.size() > 2) {
                        double timeout = Double.parseDouble(cmd.get(cmd.size() - 1));
                        String listName = cmd.get(1);
                        rpush.handleBLPOP(out, listName, timeout);
                    }
                    break;
                    case "TYPE":
                    handleType(out, cmd.get(1),store);
                    break;
                    case "INFO":
                    handleInfo(cmd, out);
                    break;
                    case "REPLCONF":
                    handleReplconf(out);
                    break;
                    case "PSYNC":
    handlePsync(out);
    break;
              }
              
              out.flush();
          } catch (IOException e) {
              break;
          }
      }
  } catch (IOException E) {
      System.out.println("Exception in run");
  }
}



}
