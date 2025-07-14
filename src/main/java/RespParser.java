import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class RespParser {
    private final InputStream in;
    public RespParser(InputStream in) {
        this.in = in;
    }


public List<String> parseArray () throws IOException{
    int firstByte = in.read();
    if(firstByte!='*') {
        throw new IOException("Expected Array Marker '*' but got ->"+(char)firstByte);
    }
    int arraySize=Integer.parseInt(readLine());
    List<String> elements = new ArrayList<>(arraySize);
    for(int i =0;i<arraySize;i++){
        elements.add(parseBulkString());
    }
    return elements;
 }
 private String parseBulkString() throws IOException{
    int marker=in.read();
    if(marker!='$'){
        throw new IOException("Expected Array Marker '$' but got ->"+(char)marker);         
    }

    int length = Integer.parseInt(readLine());
    if(length==-1){
        return null;
    }
    byte [] buf = new byte[length];
    int bytesRead=0;
    while(bytesRead<length){
        int n = in.read(buf, bytesRead, length-bytesRead);
        if(n==-1){
            throw new IOException("unexpecte end of stream while parsing Bulk String");
        }
        bytesRead+=n;
    }
    if(in.read()!='\r' || in.read()!='\n'){
        throw new IOException("Malformed RESP Bulk");
    }
    return new String(buf, "UTF-8");

}


private String readLine() throws IOException{
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int prev=-1;
    while(true){
        int b=in.read();
        if(b==-1) throw new IOException("Unexpecte end of stream");
        if(prev=='\r' && b =='\n'){
            break;
        }
        if(prev!=-1){
            baos.write(prev);
        }
        prev=b;
    }
    return baos.toString("UTF-8");
}
    
}
