import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    Socket clientSocket;
    RespParser parser;
    ClientHandler(Socket clientSocket) {
        this.clientSocket=clientSocket;
      
        }
    
        void EchoHandler( String arg ,OutputStream os) throws IOException{
          String resp = "$" + arg.length() + "\r\n" + arg + "\r\n";
          os.write(resp.getBytes("UTF-8"));

        }


    @Override
    public void run() {
         try{
      var in =clientSocket.getInputStream();
      var out = clientSocket.getOutputStream();
      RespParser parser = new RespParser(in);
      while (true) {
          try {
              var cmd = parser.parseArray();
              String command = cmd.get(0);
              if (command.equalsIgnoreCase("PING")) {
                  out.write("+PONG\r\n".getBytes("UTF-8"));
              } else if (command.equalsIgnoreCase("ECHO") && cmd.size() > 1) {
                  EchoHandler(cmd.get(1), out);
              }
              out.flush();
          } catch (IOException e) {
              
              break;
          }
      }
        
         }catch (IOException E ){
          System.out.println("Excpetion in run ");
         }
    
        }}
