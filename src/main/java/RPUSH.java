import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class RPUSH {
HashMap<String , ArrayList<String> > lists;
private Formatter fmt  = new Formatter();
    RPUSH( ){
      lists= new HashMap<>();
    } 

     void handleRPUSH(OutputStream os ,String list_name , String element ){
        if( lists.containsKey(list_name)){
            lists.get(list_name).add(element);
        }else{
            var temp = new ArrayList<String>();
            temp.add(element);
            lists.put(list_name,temp);
        }
        var answer =fmt.formatRpush(lists.get(list_name).size());
        try{
        os.write(answer.getBytes());
        }catch( IOException e ){
            System.out.println("Exception while trying to send command from RPUSH ");
        }


    }

}
