import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class RPUSH {
 static HashMap<String , ArrayList<String> > lists;
private Formatter fmt  = new Formatter();
static Map<String , Queue<Object> > blockedClients =new HashMap<>();
static final Object globalLock= new Object();
    RPUSH( ){
      lists= new HashMap<>();
    } 
    void handleRPUSH(OutputStream os, String list_name, ArrayList<String> elements) {
        Object toWake = null;
        synchronized (globalLock) {
            lists.putIfAbsent(list_name, new ArrayList<>());
            lists.get(list_name).addAll(elements);
    
            Queue<Object> waiters = blockedClients.get(list_name);
            if (waiters != null && !waiters.isEmpty()) {
                toWake = waiters.poll();
            }
        }
        if (toWake != null) {
            synchronized (toWake) {
                toWake.notify();
            }
        }
        String answer = fmt.formatRpush(lists.get(list_name).size());
        try {
            os.write(answer.getBytes());
        } catch (IOException e) {
            System.out.println("Exception while trying to send command from RPUSH");
        }
    }
    
    void handleLRANGE(OutputStream os, String list_name, int start, int end) {
        String answer;
        ArrayList<String> result = new ArrayList<>();
        try {
            if (!lists.containsKey(list_name)) {
                answer = fmt.formatArray(result);
                os.write(answer.getBytes());
                return;
            }
            var target = lists.get(list_name);
            int size = target.size();
            if (start < 0) start = size + start;
            if (end < 0) end = size + end;
            if (start < 0) start = 0;
            if (end < 0) end = 0;
            if (end >= size) end = size - 1;
            if (start > end || start >= size) {
                answer = fmt.formatArray(result);
                os.write(answer.getBytes());
                return;
            }
            for (int i = start; i <= end; i++) {
                result.add(target.get(i));
            }
            answer = fmt.formatArray(result);
            os.write(answer.getBytes());
        } catch (IOException e) {
            System.out.println("something went wrong during handle range ");
        }
    }

    void handleLPUSH(OutputStream os, String list_name, ArrayList<String> elements) {
        Object toWake = null;
        synchronized (globalLock) {
            lists.putIfAbsent(list_name, new ArrayList<>());
            ArrayList<String> target = lists.get(list_name);
            for (int i = 0; i < elements.size(); i++) {
                target.add(0, elements.get(i));
            }
            Queue<Object> waiters = blockedClients.get(list_name);
            if (waiters != null && !waiters.isEmpty()) {
                toWake = waiters.poll();
            }
        }
        if (toWake != null) {
            synchronized (toWake) {
                toWake.notify();
            }
        }
        String answer = fmt.formatRpush(lists.get(list_name).size());
        try {
            os.write(answer.getBytes());
        } catch (IOException e) {
            System.out.println("Exception while trying to send command from LPUSH");
        }
    }
    void handleLLEN( OutputStream os , String list_name ){
        String  answer;
        if(!lists.containsKey(list_name)) answer =fmt.formatRpush(0);
       else  answer=fmt.formatRpush(lists.get(list_name).size());
       try{
        os.write(answer.getBytes());
       }catch(IOException e ){
        System.out.println("something went worng during LLen ");
       }
    }
    void handleLPOP(OutputStream os, String listName) {
        String answer;
        try {
            if (!lists.containsKey(listName) || lists.get(listName).isEmpty()) {
                answer = "$-1\r\n";
            } else {
                String popped = lists.get(listName).remove(0);
                answer = fmt.formatBulkString(popped);
            }
            os.write(answer.getBytes());
        } catch (IOException e) {
            System.out.println("Something went wrong during LPOP");
        }
    }
    void handleLPOP(OutputStream os, String listName, int count) {
        String answer;
        try {
            if (!lists.containsKey(listName) || lists.get(listName).isEmpty()) {
                answer = "$-1\r\n";
            } else {
                var target = lists.get(listName);
                ArrayList<String> deleted = new ArrayList<>();
                int numToRemove = Math.min(count, target.size());
                for (int i = 0; i < numToRemove; i++) {
                    deleted.add(target.remove(0));
                }
                answer = fmt.formatArray(deleted);
            }
            os.write(answer.getBytes());
        } catch (IOException e) {
            System.out.println("Something went wrong during LPOP");
        }
    }
    void handleBLPOP(OutputStream os, String listName, double timeoutSeconds) {
        Object myLock = new Object();
        boolean isWaiting = false;
        long deadline = System.currentTimeMillis() + (long)(timeoutSeconds * 1000);
    
        while (true) {
            synchronized (globalLock) {
                ArrayList<String> list = lists.get(listName);
                if (list != null && !list.isEmpty()) {
                    String value = list.remove(0);
                    String resp = fmt.formatArray(List.of(listName, value));
                    try {
                        os.write(resp.getBytes());
                        os.flush();
                    } catch (IOException e) {
                        System.out.println("Error writing BLPOP response");
                    }
                    return;
                }
                if (!isWaiting) {
                    blockedClients.putIfAbsent(listName, new LinkedList<>());
                    blockedClients.get(listName).add(myLock);
                    isWaiting = true;
                }
            }
            long waitMillis;
            if (timeoutSeconds == 0) {
                waitMillis = 0; // wait forever
            } else {
                waitMillis = deadline - System.currentTimeMillis();
                if (waitMillis <= 0) {
                    // Timeout expired, remove lock and return null bulk string
                    synchronized (globalLock) {
                        blockedClients.get(listName).remove(myLock);
                    }
                    try {
                        os.write("$-1\r\n".getBytes());
                        os.flush();
                    } catch (IOException e) {
                        System.out.println("Error writing BLPOP timeout response");
                    }
                    return;
                }
            }
            synchronized (myLock) {
                try {
                    if (waitMillis > 0) {
                        myLock.wait(waitMillis);
                    } else {
                        myLock.wait();
                    }
                } catch (InterruptedException e) {
                    System.out.println("something went wrong during BLPOP operation ");
                }
            }
        }
    }



    }

