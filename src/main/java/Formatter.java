public class Formatter {
 
    
    public String formatSimpleString(String cmd){
        return "+"+cmd+"\r\n";
    }
    public String formatBulkString(String cmd){
        if (cmd==null)  return "$-1"+"\r\n";
        return "$"+cmd.length()+"\r\n"+cmd+"\r\n";
    }


}
