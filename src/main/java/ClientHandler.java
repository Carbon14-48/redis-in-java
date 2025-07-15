import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    Socket clientSocket;
    RespParser parser;
    RedisConfig config;
    ClientHandler(Socket clientSocket, RedisConfig config) {
        this.clientSocket=clientSocket;
        this.config = config;
      
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
    out.write(store.get(cmd.get(1)).getBytes("UTF-8"));
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
