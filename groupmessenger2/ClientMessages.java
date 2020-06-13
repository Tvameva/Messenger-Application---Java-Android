package edu.buffalo.cse.cse486586.groupmessenger2;

public class ClientMessages implements Comparable<ClientMessages> {
    int seq;
    boolean delv_flag;
    String cPort, mesg;
    public ClientMessages(String clientPort, String message, int seqNum, boolean deliverable){
        this.cPort = clientPort;
        this.mesg = message;
        this.seq = seqNum;
        this.delv_flag = deliverable;
    }
    @Override
    public int compareTo(ClientMessages client2) {
        if(seq < client2.seq){
            return -1;
        }
        else if(seq > client2.seq){
            return 1;
        }
        else{
            if(client2.delv_flag && !delv_flag){
                return -1;
            }
            else if(!client2.delv_flag && delv_flag){
                return 1;
            }
            else{
                if(Integer.parseInt(cPort) >= Integer.parseInt(client2.cPort)){
                    return 1;
                }
                else {
                    return -1;
                }
            }
        }
    }
}
