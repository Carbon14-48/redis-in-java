public class Formatter {
 
    
    public String formatSimpleString(String cmd){
        return "+"+cmd+"\r\n";
    }
    public String formatBulkString(String cmd){
        if (cmd==null)  return "$-1"+"\r\n";
        return "$"+cmd.length()+"\r\n"+cmd+"\r\n";
    }
    public String formatBulkArray(String key, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("*2\r\n");
        sb.append(formatBulkString(key));
        sb.append(formatBulkString(value));
        return sb.toString();
    }

}
