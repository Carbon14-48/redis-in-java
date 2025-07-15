import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

public class Main {
    
  public static void main(String[] args){
    String dir = "/tmp"; 
    String dbfilename = "dump.rdb"; 
    for (int i = 0; i < args.length; i++) {
      if ("--dir".equals(args[i]) && i + 1 < args.length) {
        dir = args[++i];
      } else if ("--dbfilename".equals(args[i]) && i + 1 < args.length) {
        dbfilename = args[++i];
      }
    }
    RedisConfig config = new RedisConfig(dir, dbfilename);
     String rdbPath = dir + "/" + dbfilename;
    Set<String> keys = RdbLoader.loadKeys(rdbPath);

        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int port = 6379;
        
        try {
          serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
      while (true) {
       clientSocket = serverSocket.accept();
       new Thread(new ClientHandler(clientSocket,config,keys)).start();
        
      }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
       } finally {
         try {
            if (clientSocket != null) {
              clientSocket.close();
            }
        } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());

       
          }
        }
        
        
  }


  
}
