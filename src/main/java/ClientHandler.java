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
      SetGet store = new SetGet();
      while (true) {
          try {
              var cmd = parser.parseArray();
              String command = cmd.get(0);
              if (command.equalsIgnoreCase("PING")) {
                  out.write("+PONG\r\n".getBytes("UTF-8"));
              } else if (command.equalsIgnoreCase("ECHO") && cmd.size() > 1) {
                  EchoHandler(cmd.get(1), out);
              }else if (command.equalsIgnoreCase("SET") && cmd.size()>2){
                out.write(store.set(cmd.get(1), cmd.get(2)).getBytes("UTF-8"));
              }else if (command.equalsIgnoreCase("GET") && cmd.size() > 1) {
                out.write(store.get(cmd.get(1)).getBytes("UTF-8"));
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
