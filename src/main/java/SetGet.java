import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.print.DocFlavor.STRING;

public class SetGet {
    ConcurrentHashMap<String, String> commandes=new ConcurrentHashMap<>();
    Formatter fmt = new Formatter();

    String  set (String key , String value){
        commandes.put( key,value);
        return fmt.formatSimpleString("OK");
    }
    String get(String key ){
        var value= commandes.get(key);
       
        return fmt.formatBulkString(value);
    }


}
