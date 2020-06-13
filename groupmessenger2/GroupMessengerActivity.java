package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = "GroupMessengerActivity";
    static final String EMPTY_STRING = "";
    static final String NEW_LINE = "\n";
    static final String Port0 = "11108";
    static final String Port1 = "11112";
    static final String Port2 = "11116";
    static final String Port3 = "11120";
    static final String Port4 = "11124";
    static final int SERVER_PORT = 10000;
    static ArrayList<String> Clients = new ArrayList<String>(Arrays.asList(Port0,
            Port1, Port2, Port3, Port4));
    static HashMap<Integer,Integer> Proposals = new HashMap<Integer,Integer>();
    static int ClientSeq = -1;
    static int ServSeq = -1;
    static String failedClient = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        Proposals.put(11108,-1);
        Proposals.put(11112,-1);
        Proposals.put(11116,-1);
        Proposals.put(11120,-1);
        Proposals.put(11124,-1);

        TelephonyManager telManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        String portString = telManager.getLine1Number().substring(telManager.getLine1Number().length() - 4);
        final String portNumber = String.valueOf((Integer.parseInt(portString)) * 2);

        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (Exception e){
            Log.e(TAG, e.toString());
        }
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editText1);
                Log.i(TAG, "From EditText1 : " +editText.getText().toString());
                String msg = editText.getText().toString();

                editText.setText(EMPTY_STRING);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, portNumber);
                Log.i(TAG, "Client "+portNumber+" created");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int proSeq = -1;
            PriorityQueue<ClientMessages> clientMessages = new PriorityQueue<ClientMessages>();
            try{
                while(true){
                    Socket client = serverSocket.accept();
                    BufferedReader read_msgs = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String msg;
                    if((msg = read_msgs.readLine()) != null){
                        Log.i("read_msgs", msg);
                        String clientPort = msg.split(":")[0]; Log.i("testing", clientPort);

                        if(Clients.contains(clientPort)) {
                            proSeq = 1 + Math.max (ServSeq,proSeq);
                            clientMessages.add(new ClientMessages(clientPort,
                                    msg.split(":")[1], Integer.parseInt(msg.split(":")[2]), false));
                            int max_seq = Proposals.get(Integer.parseInt(clientPort));
                            Proposals.put(Integer.parseInt(clientPort),Math.max(max_seq,proSeq));
                            Log.i("Manooja2",""+Math.max(max_seq,proSeq)+"writing to port : "+clientPort);
                        }
                        else {

                            Log.i("Received", "In else ServSeq = " + ServSeq + " Message = " + msg);
                            int seq = Integer.parseInt(msg.split(":")[1]);
                            String rcdClt = msg.split(":")[0];
                            clientPort = msg.split(":")[2];
                            Iterator<ClientMessages> itr = clientMessages.iterator();
                            ClientMessages delMsg = null;
                            while (itr.hasNext()){
                                delMsg = itr.next();
                                if(delMsg.mesg.equals(rcdClt))
                                    break;
                            }
                            clientMessages.remove(delMsg);
                            if(!(failedClient.length() > 0 && clientPort.equals(failedClient)))
                            {

                                clientMessages.add(new ClientMessages(clientPort, rcdClt, seq, true));}
                        }
                        Log.i("Fuck","Failed client is   "+failedClient);
                        ClientMessages head = null;
                        while ((head = clientMessages.peek()) != null && head.delv_flag) {
                            ServSeq += 1;
                            publishProgress(head.mesg+":"+ServSeq);
                            Log.i("Manooja Final",""+ServSeq+" && "+Proposals.get(Integer.parseInt(clientPort)));
                            clientMessages.remove();
                            Proposals.put(Integer.parseInt(clientPort),-1);
                            client.close();
                        }
                        if((head = clientMessages.peek()) != null && !head.delv_flag
                                && failedClient.length() > 0 && head.cPort.equals(failedClient)){
                            Log.i("Remove","Message: "+head.mesg);
                            clientMessages.remove(head);
                        }

                    }

                }

            } catch (IOException e){
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            String strReceived = values[0].split(":")[0].trim();
            String seq = values[0].split(":")[1].trim();
            ContentResolver mContentResolver = getContentResolver();
            Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            ContentValues mContentValues = new ContentValues();
            mContentValues.put("key", seq);
            mContentValues.put("value", strReceived);
            mContentResolver.insert(mUri, mContentValues);
            Log.i("delivered", "key : "+seq+" value : "+strReceived);
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(strReceived + NEW_LINE);

        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            ArrayList<String> clients = new ArrayList<String>();
            ClientSeq += 1;
            Log.i("Dht","From port  "+params[1]+" Seq being"+ClientSeq);
            String portNumber = params[1];
            String message = params[0];
            for (String remote_port : Clients) {
                try {
                    Socket client = new Socket();
                    client.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remote_port)));
                    client.setSoTimeout(500);
                    PrintWriter msgSend = new PrintWriter(client.getOutputStream(), true);
                    Log.i("Manooja param ",portNumber);
                    msgSend.println(portNumber + ":" + message + ":" + ClientSeq);
                    Thread.sleep(500);
                } catch (Exception e) {
                    Log.e("Seq Num", e.toString());
                }
            }
            int suggestedhash = Proposals.get(Integer.parseInt(portNumber));
            //Log.i("Manooja",""+suggestedhash +"  "+suggestedSeqNum +" && "+portNumber);
            int suggestedSeqNum = suggestedhash;
            for (String remote_port : Clients) {
                try {
                    Socket client = new Socket();
                    client.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remote_port)));
                    client.setSoTimeout(500);
                    Log.i("Sending", "Sending to client: " + remote_port);
                    //String message = params[0];
                    PrintWriter msgSend = new PrintWriter(client.getOutputStream(), true);
                    clients.add(remote_port);
                    Log.i("Manooja parameter 1",params[1]);
                    msgSend.println(message + ":" + suggestedSeqNum + ":" + params[1]);
                    Thread.sleep(500);
                } catch (NullPointerException e) {
                    failedClient = remote_port;
                    Log.i("Failure", "Clients " + failedClient);
                } catch (Exception e) {
                    Log.e("Sending message", e.toString());
                }
            }

            if(failedClient.length() > 0 && Clients.contains(failedClient))
                Clients.remove(failedClient);
            return null;
        }


    }
}