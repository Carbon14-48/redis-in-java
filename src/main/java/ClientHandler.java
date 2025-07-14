import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    Socket clientSocket;
    ClientHandler(Socket clientSocket){
        this.clientSocket=clientSocket;
    }


    @Override
    public void run() {
         try{
      var in =clientSocket.getInputStream();
      var out = clientSocket.getOutputStream();
      byte[] input = new byte[1024];
int bytesRead;
while ((bytesRead = in.read(input)) != -1) {
    out.write("+PONG\r\n".getBytes());
    out.flush();
      }
      }catch(IOException e ){
        System.out.println(e);
      }
        
    }
    
}
