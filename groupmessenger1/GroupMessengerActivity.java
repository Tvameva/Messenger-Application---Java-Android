package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;



import android.content.Context;
import android.content.ContentResolver;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.content.ContentValues;
import android.widget.EditText;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import android.net.Uri;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 * @author manju_karthik_shivashankar
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static int msgSq = 0;
    static final String[] pList = {"11108", "11112", "11116", "11120", "11124"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));


        TextView textView = (TextView) findViewById(R.id.textView1);
        textView.setMovementMethod(new ScrollingMovementMethod());

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "ServerSocket - creation failed!");
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);
        

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(textView, getContentResolver()));
        


        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(null);
                TextView tempText = (TextView) findViewById(R.id.textView1);
                tempText.append("\t" + msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try {
                for(;;) {

                    Socket soc = serverSocket.accept();
                    BufferedReader input_stream = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                    String message = input_stream.readLine();

                    /* Stores each message string in a key value pair in the content provider and publishes the input stream to the UI thread. */
                    if(message != null) {
                        publishProgress(message);
                    }

                    soc.close();
                    input_stream.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "UnknownHostException");
            } catch(IOException e){
                Log.e(TAG, "Socket IOException");
            }

            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            resolveContents(strReceived);
        }

        protected void resolveContents(String strReceived){
            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", String.valueOf(msgSq));
            keyValueToInsert.put("value", strReceived);

            ContentResolver resolver = getContentResolver();
            Uri myUri = Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger1.provider");
            resolver.insert(myUri, keyValueToInsert);
            msgSq++;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                //Log.e(TAG, "Inside Client task");

                for(String remotePort : pList) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                    String msgToSend = msgs[0];
                    PrintWriter write = new PrintWriter(socket.getOutputStream(), true);
                    write.println(msgToSend);
                    socket.close();
                    write.close();
                }

            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }



}
