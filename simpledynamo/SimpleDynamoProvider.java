package edu.buffalo.cse.cse486586.simpledynamo;
		import java.io.BufferedReader;
		import java.io.IOException;
		import java.io.InputStreamReader;
		import java.net.ServerSocket;
		import java.net.Socket;
		import java.net.UnknownHostException;
		import java.security.MessageDigest;
		import java.security.NoSuchAlgorithmException;
		import java.util.Formatter;
		import java.util.HashMap;
		import java.util.HashSet;
		import java.io.*;
		import java.util.*;
		import java.util.Iterator;
		import java.util.PriorityQueue;
		import java.net.InetAddress;
		import java.net.InetSocketAddress;


		import android.content.ContentProvider;
		import android.content.ContentResolver;
		import android.content.ContentValues;
		import android.database.Cursor;
		import android.database.MatrixCursor;
		import android.net.Uri;
		import android.os.AsyncTask;
		import android.os.Bundle;
		import android.util.Log;
		import android.widget.TextView;
		import android.telephony.TelephonyManager;
		import android.content.Context;


public class SimpleDynamoProvider extends ContentProvider  {
	static HashMap<String,String> Values = new HashMap<String, String>(); //store the keys and their values here
	static HashMap<String,String> NodeList = new HashMap<String,String>();//maps genHash(nodes) to nodes
	static List<String> Nodes = new ArrayList<String>(); // list of all nodes, visible to all AVDs
	static List<String> HashNode = new ArrayList<String>(); //list of all genHash(nodes)
	static final int SERVER_PORT = 10000;

	private Uri mUri;
	public String myPort;
	public String nodeId;
	public int sucSet =0, predSet =0,back=0;
	public String successor;
	public String predecessor;
	public String hsuccessor;
	public String hpredecessor;
	public String least,maxx,hmaxx,hleast;
	public List<String> localKeys = new ArrayList<String>();
	public List<String> othersKeys2 = new ArrayList<String>();
	public List<String> othersKeys1 = new ArrayList<String>();
	public List<String> allKeys = new ArrayList<String>();
	public String ResKeys = "";//used only for * query
	public HashMap<String,String> QueryResult = new HashMap<String, String>(); //store the keys and their values here



	@Override
	public boolean onCreate() {
		//super.onCreate(savedInstanceState);
		// TODO Auto-generated method stub
		TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		//Log.i("On Create","Triggering AVD creation for  "+portStr);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));
		String joinReq = "AddNode$" + myPort;
		try {
			//Log.i("OnCreate","Creating a new AVD instance");
			nodeId = genHash(String.valueOf(Integer.parseInt(myPort) / 2)); // using this at insertion
			ServerSocket serverSocket = null;
			//Log.i("On Create","Creating a server socket");
			//serverSocket.setReuseAddress(true);
			serverSocket = new ServerSocket(SERVER_PORT);
			//Log.i("OnCreate","Calling server task");
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
			Thread.sleep(1);
			mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");
			//Log.i("OnCreate","Build URI successful");


		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//Log.i("OnCreate","Firing client to add node "+myPort);
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, joinReq, "11108");

		return true;
	}


	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		//Log.i("Provider Insert","Entering Insert  ");
		String key = values.get("key").toString();
		String value = values.get("value").toString();
		if (Nodes.size()<2){
			//Log.i("Provider Insert","Insertion called locally at provider");
			Values.put(key,value);
			inputToFile(key,value);
			localKeys.add(key);
			//Log.i("Provider Insert","Inserted locally in provider itself");
			//Log.i("Insertion Complete","Inserting into  "+myPort+"  Value is  "+value);
			return uri;
		}// the condition is applied as the requirements said insertions will happen after all nodes join,
		// else just call setSuccessor every-time
		//if (predSet == 0 && Nodes.size()>1) setPredecessor();
		//Log.i("Provider Insert","My port is "+myPort+" add requested by key :"+key+" and its value is  "+value);
		try {
			String hashKey = genHash(key);
			//check if the chord has more than one node
			////String least = NodeList.get(Collections.min(HashNode));
			//String maxx = NodeList.get(Collections.max(HashNode));
			Log.i("Provider Insert","least is  "+least+" and max is  "+maxx);
			if(hashKey.compareTo(hleast)<0){
				//the hashKey of the received key is so small that we must automatically add it to the first node in the chord
				String msgInsert = "MustInsert$3$" + key + "$" + value;
				//String mustPort = NodeList.get(Collections.min(HashNode));
				Log.i("Provider Insert","Forwarding insert to the least[MIN] node "+least);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgInsert, least);
			}
			else if(hashKey.compareTo(hmaxx)>0){
				//the hashKey of the received key is so big that we must automatically add it to the first node in the chord
				String msgInsert = "MustInsert$3$" + key + "$" + value;
				//String mustPort = NodeList.get(Collections.min(HashNode));
				Log.i("Provider Insert","Forwarding insert request[MAX] to the least node  "+least);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgInsert, least);
			}
           /* else if(Nodes.size() <2){
                //Log.i("Provider Insert","SingleNode Insert"+Values.size());
                //the insert is requested at the only AVD
                Values.put(key,value);
                inputToFile(key,value);
                localKeys.add(key);
                //Log.i("Insertion Complete","Inserting into  "+myPort+"  Value is  "+value);
            }*/
			else {
				//there are more nodes
				//check if the generated key belongs to my range
				//get the index of this node first
				//int ind = HashNode.indexOf(hashKey);
				//Log.i("Provider Insert","Insertion called");
				if((hashKey.compareTo(nodeId)<0 && hashKey.compareTo(hpredecessor)>0) ){
					//|| (hashKey.compareTo(HashNode.get(HashNode.size()-1))>0 && HashNode.indexOf(nodeId) ==0) ){
					//the insert is requested at the right AVD
					// or its genHash(key) is less than mine nodeId and greater than my predecessors'
					// or if its greater than the last node in the chord and the current node is Node0 then add it to this node
					Log.i("Provider Insert","Insertion called locally at provider");
					String mustInsert = "MustInsert$2$"+key+"$"+value;
					Values.put(key,value);
					inputToFile(key,value);
					localKeys.add(key);
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mustInsert, successor);
					//Log.i("Provider Insert","Inserted locally in provider itself");
					//Log.i("Insertion Complete","Inserting into  "+myPort+"  Value is  "+value);
				}
				else {
					//the hashKey of the message received is greater than my hash node ID so i shall forward it to my next port
					String msgInsert = "InsertMsg$" + key + "$" + value;
					Log.i("Provider Insert","Forwarding insert request to successor");
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgInsert, successor);
				}
               /* else if(hashKey.compareTo(nodeId)==0 || (hashKey.compareTo(nodeId)<0 && hashKey.compareTo(genHash(predecessor))>0) || (hashKey.compareTo(HashNode.get(HashNode.size()-1))>0 && HashNode.indexOf(nodeId) ==0) ){
                    //the insert is requested at the right AVD
                    // or its genHash(key) is less than mine nodeId and greater than my predecessors'
                    // or if its greater than the last node in the chord and the current node is Node0 then add it to this node
                    Values.put(key,value);
                    inputToFile(key,value);
                    localKeys.add(key);
                    Log.i("Provider Insert","Inserted locally");

                }
                else if((hashKey.compareTo(nodeId)<0 && hashKey.compareTo(genHash(predecessor))<=0)){
                    //the hashKey of the message received is smaller than my hash node ID so i shall forward it to my previous port
                    String msgInsert = "InsertMsg$" + key + "$" + value;
                    Log.i("Provider Insert","Forwarding insert request to predecessor");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgInsert, predecessor);
                }
                else{
                    Log.i("failed insertion","message insertion failed");
                }*/
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}


		return uri;
	}

	public void inputToFile(String key, String value) {
		//code copied from PA2A
		FileOutputStream outputStream;
		try {
			outputStream = getContext().openFileOutput(key, Context.MODE_PRIVATE);
			outputStream.write(value.getBytes());
			outputStream.close();
			//Log.d("Wrote to file", key + "$" + value);
		} catch (Exception e) {
			//Log.i("FileOutputCreation", "Error");
		}
	}

	private String readFromFile(String key) {

		FileInputStream inputStream;
		String value = null;
		byte[] bArray = new byte[128];
		try {
			inputStream = getContext().openFileInput(key);
			int readCount = inputStream.read(bArray);
			if (readCount != -1) value = new String(bArray, 0, readCount);
			//Log.d("Read from file", key + "$" + value);
			inputStream.close();
		} catch (IOException e) {
			Log.e("File read Error", key);
		}
		return value;
	}


	public String getSuccessor(String port) throws NoSuchAlgorithmException {
		//Collections.sort(HashNode);
		int nodeSize = HashNode.size();
		int myIndex = HashNode.indexOf(port); //get my index in the chord
		String sucNow = new String();
		if(myIndex < nodeSize-1)sucNow = NodeList.get(HashNode.get(myIndex+1)); // its not the genHashed valued, its the port number only
		else sucNow = NodeList.get(HashNode.get(0));
		String sucks = "\\$";
		for(int i=0; i<nodeSize;i++){
			sucks = sucks+NodeList.get(HashNode.get(i)) +"\\$";
		}
		String serPort = NodeList.get(HashNode.get(myIndex));
		if(serPort.equals("11108")) {successor = sucNow;
			hsuccessor = genHash(String.valueOf(Integer.parseInt(successor) / 2));
			//Log.i("Successor","Setting succ for 11108 "+successor);
		}
		//Log.i("Successor", "Currently succ lokks like  "+sucks+" returning the succ as "+sucNow+" of the node  "+NodeList.get(HashNode.get(myIndex)));
		//sucSet =1;

		return sucNow;
	}

	public String getPredecessor(String port) throws NoSuchAlgorithmException {
		int nodeSize = HashNode.size();
		int myIndex = HashNode.indexOf(port); //get my index in the chord
		String predNow = new String();
		if(myIndex > 0) predNow = NodeList.get(HashNode.get((myIndex-1))); // real port number, not the genHash value is set here
		else predNow = NodeList.get(HashNode.get(nodeSize-1));
		//predSet=1;
		String serPort = NodeList.get(HashNode.get(myIndex));
		if(serPort.equals("11108"))
		{   predecessor = predNow;
			hpredecessor = genHash(String.valueOf(Integer.parseInt(predecessor) / 2));
			//Log.i("Successor","Setting pred for 11108 "+predecessor);
		}
		//Log.i("Successor", "Currently succ lokks like   returning the pred as "+predNow+" of the node  "+NodeList.get(HashNode.get(myIndex)));
		return predNow;
	}




	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
						String sortOrder) {
		String[] mColumns = {"key", "value"};
		MatrixCursor cursor = new MatrixCursor(mColumns);
		//Log.i("Query Provider","Inside query  ");
		if(Nodes.size()<2){
			if(selection.equals("@")||selection.equals("*")){
				for (String key : localKeys) {
					String value = Values.get(key);
					String res[] = {key, value};
					cursor.addRow(res);
					//Log.d("Retrieved", key + ":" + value);
				}
			} else{
				String key = selection;
				String value = Values.get(key);
				String res[] = {key, value};
				cursor.addRow(res);
				//Log.d("Retrieved", key + ":" + value);
			}
		}

		else{

			try {
				String hashKey = genHash(selection);
				String msgQuery = "Query$" + selection + "$" + hashKey+"$"+myPort;
				if (selection.equals("@")) {
					for (String key : localKeys) {
						String value = Values.get(key);
						String res[] = {key, value};
						cursor.addRow(res);
						Log.d("Query local", key + ":" + value);
					}
					for (String key : othersKeys1) {
						String value = Values.get(key);
						String res[] = {key, value};
						cursor.addRow(res);
						Log.d("Query Local", key + ":" + value);
					}
					for (String key : othersKeys2) {
						String value = Values.get(key);
						String res[] = {key, value};
						cursor.addRow(res);
						Log.d("Query Local", key + ":" + value);
					}
				} else if(selection.equals("*")) {
					Log.i("Provider Query", "Calling with * ");
					//QueryResult = new HashMap<String, String>();
					String allQuery = "AllResponse";
					back=0;
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, allQuery, successor);
					for(;;){
						Log.i("PQuery All","waiting for all response");
						if(back == 1) break;
					}
					String[] m_part_keys = ResKeys.split("\\$");
					for (int i=1;i<m_part_keys.length;i=i+2) {
						String key = m_part_keys[i];
						//String value = readFromFile(key);
						String value = m_part_keys[i+1];
						Log.i("Provider Query", "Key "+key+" value "+value);
						String res[] = {key, value};
						cursor.addRow(res);
						//Log.d("Retrieved", key + ":" + value);
					}


					//Log.i("Provider Query","Broke out of for");

				} else if ((hashKey.compareTo(hleast) < 0) || (hashKey.compareTo(hmaxx) > 0)) {
					Log.i("Provider Query", "Forwarding insert request{MIN} / [MAX] to the least node  " + least);

					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgQuery, least);
					for(;;){
						Log.i("Provider Query", "Calling at least node ");
						if(QueryResult.containsKey(selection)) break;
					}
					String key = selection;
					String value = QueryResult.get(key);
					String res[] = {key, value};
					cursor.addRow(res);
					Log.d("Retrieved", key + ":" + value);
				} else if (hashKey.compareTo(nodeId) < 0 && hashKey.compareTo(hpredecessor) > 0) {
					String key = selection;
					String value = Values.get(key);
					String res[] = {key, value};
					cursor.addRow(res);
					Log.d("Retrieved", key + ":" + value);
				} else {
					String sucQuery = "SQuery$" + selection + "$" + hashKey+"$"+myPort;
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, sucQuery, successor);
					for(;;){
						//Log.i("Provider Query", "Calling SQuery ");
						if(QueryResult.containsKey(selection)) break;
					}
					String key = selection;
					String value = QueryResult.get(key);
					String res[] = {key, value};
					cursor.addRow(res);
					Log.d("Retrieved", key + ":" + value);
				}
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (selection.equals("@")) {
			if(localKeys.size()>0){
				localKeys.clear();
				Values.clear();
				String delMsg = "MustDelete$2$"+myPort;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, delMsg, successor);
			}
		}
		else if(selection.equals("*")){
			String delMsg = "AllDelete";
			if(!localKeys.isEmpty())
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, delMsg, successor);
			for(;;){
				if(localKeys.isEmpty()) break;
			}

		}
		else{
			if(localKeys.contains(selection)){
				localKeys.remove(selection);
				Values.remove(selection);
				String delMsg = "OtherDelete$2$"+selection+"$"+myPort;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, delMsg, successor);
			}
			else{
				String delMsg = "Delete$" + selection+"$"+myPort;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, delMsg, successor);

			}

		}
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	//starting server side code
	//copying from PA2
	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			//Log.i("ServerTask","Entering server task");
			ServerSocket serverSocket = sockets[0];
			Socket client = null;
			DataInputStream read_msgs = null;
			//DataOutputStream ackSend = null;
			//DataInputStream read_msgs = null;
			//new DataInputStream(socket.getInputStream());
			//String receivedFromServer = dataInputStream.readUTF();
			try {
				while (true) {
					//Log.i("ServerTask","Accepting client connection and printing socket[0]  "+sockets[0]);

					client = serverSocket.accept();

					//Log.i("ServerTask","connection accepted by  "+myPort);

					read_msgs = new DataInputStream(client.getInputStream());
					//Log.i("ServerTask","Creating DataInput stream");
					String msg = read_msgs.readUTF();
					//Log.i("ServerTask","connection accepted");

					//Log.i("ServerTask","read message  "+msg);


					Log.i("Server Task","trying Split" +msg);
					String[] m_parts = msg.split("\\$");
					Log.i("ServerTask","Message split  "+m_parts[0]);
					if(m_parts.length == 0) return null;

					String swich = m_parts[0];
					if ("InsertMsg".equals(swich)) {

						Log.i("Server Task","Calling Insert msg of port "+myPort);
						InsertMsg(m_parts[1], m_parts[2]);

					}else if ("MustInsert".equals(swich)) {
						Log.i("Server Task","Calling MustInsert msg at port "+myPort+" with msg "+msg);
						MustInsert(m_parts[1], m_parts[2],m_parts[3]);
					}else if ("AddNode".equals(swich)) {
						//Log.i("ServerTask","Inside Add node  "+m_parts[1]);
						//if (m_parts[1] != null)
						try {
							//Log.i("ServerTask","Calling add node to add port  "+m_parts[1]);
							AddNode(m_parts[1], genHash(String.valueOf(Integer.parseInt(m_parts[1]) / 2)));
							//Log.i("Add Node","Its a part of chord now");
						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
						}
					}else if ("Join".equals(swich)) {
						Joined(msg);
					}else if ("Query".equals(swich)) {
						//Log.i("Server Query","received query for key "+m_parts[1]);
						QueryMsg(m_parts[1], m_parts[2], m_parts[3]);
					} else if ("SQuery".equals(swich)) {
						//Log.i("Server Query","received query for key "+m_parts[1]);
						SQueryMsg(msg,m_parts[1], m_parts[2],m_parts[3]);
					} else if ("QueryAll".equals(swich)) {
						//Log.i("Server Query","received query for key "+m_parts[1]);
						QueryAll();
					} else if ("Response".equals(swich)) {
						Log.i("Server Query","received Response for key "+m_parts[1]);
						Response(m_parts[1], m_parts[2]);
					} else if ("AllResponse".equals(swich)) {
						//Log.i("Server Query","received Response for key "+m_parts[0]);
						AllResponse(msg);
					} else if ("Delete".equals(swich)) {
						Delete(m_parts[1],m_parts[2]);
					} else if ("OtherDelete".equals(swich)) {
						OtherDelete(m_parts[1],m_parts[2],m_parts[3]);
					} else if ("MustDelete".equals(swich)) {
						MustDelete(m_parts[1],m_parts[2]);
					} else if ("AllDelete".equals(swich)) {
						AllDelete();
					} else {
						Log.i("Do in Background", "failed to switch   "+m_parts[0]);
					}
					//ackSend = new DataOutputStream(client.getOutputStream());
					//Log.i("ServerTask","created ip and op streams");
					//String ack = "Server Ack";
					// ackSend.writeUTF(ack);
					//Log.i("ServerTask","Writing ack");
				}


			} catch (IOException e) {

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				try {
					if (client != null) client.close();
					if (read_msgs != null) read_msgs.close();
					//if (ackSend != null) ackSend.close();
					//Log.i("Server TAsk","Inside Finally");
				}catch (Exception e){
					//Log.i("Server TAsk","Catching Finally");
				}


			}
			//Log.i("Server TAsk","Returning Null");
			return null;
		}

		public void AddNode(String reqPort, String hashNode) throws NoSuchAlgorithmException {
			//now add the incoming node to the
			//Log.i("ServerTask","Received add request");
			if(!Nodes.contains(reqPort)) {
				Nodes.add(reqPort);
				HashNode.add(hashNode);
				NodeList.put(hashNode, reqPort);
				//lets sort the HashNode ArrayList so that we can form a chord everytime a new node is added
				Collections.sort(HashNode);
				least = NodeList.get(Collections.min(HashNode));
				maxx = NodeList.get(Collections.max(HashNode));
				hmaxx = genHash(String.valueOf(Integer.parseInt(maxx) / 2));
				hleast = genHash(String.valueOf(Integer.parseInt(least) / 2));
				Log.i("Provider Insert","least is  "+least+" and max is  "+maxx);
				//try {
				//     Thread.sleep(500);
				//} catch (InterruptedException e) {
				//    e.printStackTrace();
				//}
				//Log.i("ServerTask", "Leaving add node request, current Nodes Size is" + Nodes.size());
				//setSuccessor();
				//setPredecessor();
				//Log.i("Insert","My port is "+myPort+" successor is  "+successor+"   Predecessor is "+predecessor);
				//String least = NodeList.get(Collections.min(HashNode));
				//String maxx = NodeList.get(Collections.max(HashNode));
				//Log.i("Provider Insert","least is  "+least+" and max is  "+maxx);
				String joinMsg = "Join$";
				for (int i=0;i<Nodes.size();i++){
					String allNode = NodeList.get(HashNode.get(i));//get them in sorted order ans send them in the same way
					joinMsg = joinMsg + allNode;
					if(i!=Nodes.size()-1) joinMsg = joinMsg+"$";
				}

				//Log.i("Server Node Join","My current join message is "+joinMsg);
				for (String nodes : Nodes) {
					//Log.i("Server task join","Picked node "+nodes);
					String sucNow = "";
					String predNow = "";
					try {
						sucNow = getSuccessor(genHash(String.valueOf(Integer.parseInt(nodes) / 2)));
						predNow = getPredecessor(genHash(String.valueOf(Integer.parseInt(nodes) / 2)));
						//Log.i("Server task to join","Brought succ and preds    "+sucNow+"  "+predNow+"  for node "+nodes);
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
					if (!nodes.equals("11108")) {
						Log.i("Server Task", "Asking all nodes to add node " + reqPort + " to their port");

						joinMsg = joinMsg+"$"+sucNow+"$"+predNow+"$"+maxx+"$"+least;
						//Log.i("Server task to join","Message being sent is  "+joinMsg);
						new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, joinMsg, nodes);
					}
				}
			}
		}

		public void Joined(String joinMsg) throws NoSuchAlgorithmException, InterruptedException {
			String[] addPort = joinMsg.split("\\$");
			//Log.i("Server Task of node : "+myPort," adding node  "+addPort+" to my chord structure");
			NodeList = new HashMap<String,String>();//maps genHash(nodes) to nodes
			Nodes = new ArrayList<String>(); // list of all nodes, visible to all AVDs
			HashNode = new ArrayList<String>();
			for(int i=1;i<addPort.length-4;i++){
				String hashPort = genHash(addPort[i]);
				if(!Nodes.contains(addPort[i])) {
					Nodes.add(addPort[i]);
					HashNode.add(hashPort);
					NodeList.put(hashPort, addPort[i]);
					//lets sort the HashNode ArrrayList so that we can form a chord everytime a new node is added
					//Collections.sort(HashNode);
					//Thread.sleep(500);
					Log.i("Server Task Join","Added "+addPort[i]+" to my chord at "+myPort+" Current chord size is : "+Nodes.size());
				}
			}
			//setSuccessor();
			//setPredecessor();

			successor = addPort[addPort.length-4];
			hsuccessor = genHash(String.valueOf(Integer.parseInt(successor) / 2));
			predecessor = addPort[addPort.length-3];
			hpredecessor = genHash(String.valueOf(Integer.parseInt(predecessor) / 2));
			maxx = addPort[addPort.length-2];
			hmaxx = genHash(String.valueOf(Integer.parseInt(maxx) / 2));
			least = addPort[addPort.length-1];
			hleast = genHash(String.valueOf(Integer.parseInt(least) / 2));
			//Log.i("Insert","My port is "+myPort+" successor is  "+successor+"   Predecessor is "+predecessor);
			//Log.i("Provider Insert","least is  "+least+" and max is  "+maxx);
		}

		public void InsertMsg(String key, String value) throws NoSuchAlgorithmException {
			String hashKey = genHash(key);
			//if (sucSet == 0 && Nodes.size()>1) setSuccessor(); // the condition is applied as the requirements said insertions will happen after all nodes join,
			// else just call setSuccessor every-time
			//if (predSet == 0 && Nodes.size()>1) setPredecessor();
			//Log.i("Insertion","My port is "+myPort+" successor is  "+successor+"   Predecessor is "+predecessor);
			//copied this piece of code from previous submissions

			if((hashKey.compareTo(nodeId)<0 && hashKey.compareTo(hpredecessor)>0) ){
				//|| (hashKey.compareTo(nodeId)<0 && HashNode.indexOf(nodeId) == 0 && hashKey.compareTo(hpredecessor)<0) ){
				// || (hashKey.compareTo(HashNode.get(HashNode.size()-1))>0 && HashNode.indexOf(nodeId) ==0) ){
				//the insert is requested at the right AVD
				// or its genHash(key) is less than mine nodeId and greater than my predecessors'
				// or if its greater than the last node in the chord and the current node is Node0 then add it to this node
				//Log.i("Insertion Local","Inserting to my node  ");
				Values.put(key,value);
				inputToFile(key,value);
				localKeys.add(key);
				//insert to content provider
				ContentValues keyValueToInsert = new ContentValues();
				keyValueToInsert.put("key", key);
				keyValueToInsert.put("value", value);
				allKeys.add(key);
				Log.i("Insertion Complete","Inserting into  "+myPort+"  Value is  "+value);
				String msgInsert = "MustInsert$2$" + key + "$" + value;
				Log.i("Insertion Foward","sending to succ client  "+successor);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgInsert, successor);
				//insert(mUri, keyValueToInsert);


			}
			else{
				//the hashKey of the message received is greater than my hash node ID so i shall forward it to my next port
				Log.i("Insertion Foward","sending to succ client  "+successor);
				String msgInsert = "InsertMsg$" + key + "$" + value;
				if(!allKeys.contains(key)) {
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgInsert, successor);
					allKeys.add(key);
				}
			}
            /*else if((hashKey.compareTo(nodeId)<0 && hashKey.compareTo(genHash(predecessor))<=0)){
                //the hashKey of the message received is smaller than my hash node ID so i shall forward it to my previous port
                Log.i("Insertion Foward","sending to pred client  "+predecessor);
                String msgInsert = "InsertMsg$" + hashKey + "$" + value;
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgInsert, predecessor);
            }
            else{
                Log.i("failed insertion","message insertion failed");
            }*/


		}

		public void MustInsert(String turn, String key, String value) {
			Log.i("Insertion Local","Inserting to my node with must insert condition turn is "+turn);
			if(turn.equals("3")) {
				Values.put(key,value);
				inputToFile(key,value);
				localKeys.add(key);
				//insert to content provider
				ContentValues keyValueToInsert = new ContentValues();
				keyValueToInsert.put("key", key);
				keyValueToInsert.put("value", value);
				allKeys.add(key);
				String mustInsert = "MustInsert$2$"+key+"$"+value;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mustInsert, successor);
				Log.i("Must Insert", "Done inserting in 3");
			} else if(turn.equals("2")) {
				Values.put(key,value);
				inputToFile(key,value);
				ContentValues keyValueToInsert = new ContentValues();
				keyValueToInsert.put("key", key);
				keyValueToInsert.put("value", value);
				allKeys.add(key);
				othersKeys2.add(key);
				String mustInsert = "MustInsert$1$"+key+"$"+value;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mustInsert, successor);
				Log.i("Must Insert", "Done inserting in 2");
			}else if(turn.equals("1")){
				Values.put(key,value);
				inputToFile(key,value);
				ContentValues keyValueToInsert = new ContentValues();
				keyValueToInsert.put("key", key);
				keyValueToInsert.put("value", value);
				allKeys.add(key);
				othersKeys1.add(key);
				Log.i("Must Insert", "Done inserting in 1");
			}else{
				//turn is zero
				Log.i("Must Insert", "Done inserting in all 3");
			}
			// insert(mUri, keyValueToInsert);
			//Log.i("Insertion Complete","Inserting into  "+myPort+"  Value is  "+value);
		}

		public void QueryMsg(String queryKey, String hqKey, String queryRequestFrom){
			if(localKeys.contains(queryKey) || othersKeys2.contains(queryKey) || othersKeys1.contains(queryKey)){
				String resVal = Values.get(queryKey);
				String response = "Response$"+queryKey+"$"+resVal;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, response, queryRequestFrom);
			}
		}

		public void SQueryMsg(String msg,String queryKey, String hqKey, String queryRequestFrom){
			if(localKeys.contains(queryKey) || othersKeys2.contains(queryKey) || othersKeys1.contains(queryKey)){
				String resVal = Values.get(queryKey);
				String response = "Response$"+queryKey+"$"+resVal;
				Log.i("Inside SQuery","sending response "+response+" to port " +queryRequestFrom);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, response, queryRequestFrom);
			}
			else{
				Log.i("Inside SQuery","Forwarding request to succc" +successor);
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, successor);
			}
		}

		public void QueryAll(){
			String response = "AllResponse";
			for(String lkey : localKeys){
				//String resVal = Values.get(lkey);
				response = response+"$"+ lkey;
			}
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, response, successor);
		}

		public void Response(String rcvdKey, String rcvdValue){
			Log.i("Inside Response","Rcvd key "+rcvdKey+" and its value "+rcvdValue);
			if (!QueryResult.containsKey(rcvdKey)) {
				Log.i("Inside Response","Rcvd key "+rcvdKey+" and its value "+rcvdValue);
				QueryResult.put(rcvdKey, rcvdValue);
			}
			else{
				Log.i("Inside Response","IN ELSE Rcvd key "+rcvdKey+" and its value "+rcvdValue);
			}



		}

		public void AllResponse(String msg){
			Log.i("All response","Inside all response of port received msg  "+msg);
			for(String lkey : localKeys){
				String resVal = Values.get(lkey);
				if(!msg.contains(lkey))
				{
					msg = msg+"$"+ lkey+"$"+resVal;
				}
			}
			ResKeys = msg;
			int prev_back = back;
			back=1;
			//if(prev_back==0)
			{new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, successor);}

		}



		public void Delete(String keyToDelete, String fromPort){
			String delMsg = "Delete$"+keyToDelete+"$"+fromPort;
			if(localKeys.contains(keyToDelete)){
				localKeys.remove(keyToDelete);
				Values.remove(keyToDelete);
				String del = "MustDelete$2$"+fromPort;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, del, successor);
				//back=1;
			}
			else {
				{new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,delMsg, successor);}
			}
		}

		public void MustDelete(String turns, String rcvdPort){
			if(turns.equals("2")){
				String delMsg = "MustDelete$1$"+rcvdPort;
				for(String key : othersKeys2){
					//othersKeys2.remove(key);
					Values.remove(key);
				}
				othersKeys2.clear();
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,delMsg, successor);
			} else if(turns.equals("1")){
				String delMsg = "DoneDelete";
				for(String key : othersKeys1){
					//othersKeys1.remove(key);
					Values.remove(key);
				}
				othersKeys1.clear();
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,delMsg, rcvdPort);
			}
		}

		public void OtherDelete(String turns,String key, String rcvdPort){
			if(turns.equals("2")){
				String delMsg = "OtherDelete$1$"+key+"$"+rcvdPort;
				if(othersKeys2.contains(key)) {
					Values.remove(key);
					othersKeys2.remove(key);
					allKeys.remove(key);
				}
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,delMsg, successor);
			} else if(turns.equals("1")){
				String delMsg = "DoneDelete";
				if(othersKeys1.contains(key)) {
					Values.remove(key);
					othersKeys1.remove(key);
					allKeys.remove(key);
				}
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,delMsg, rcvdPort);
			}
		}

		public void AllDelete(){
			String delMsg = "AllDelete$";
			localKeys.clear();
			Values.clear();
			othersKeys1.clear();
			othersKeys2.clear();
			allKeys.clear();
			{new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,delMsg, successor);}
		}

		@Override
		protected void onProgressUpdate(String... values) {
			return;

		}
	}

	private class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {
			//Log.i("Client Task","Entering client task");
			try {
				String port = msgs[1];
				String msgToSend = msgs[0];
				//Log.i("Client TAsk","Before sending message to the port  "+port);
				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
						Integer.parseInt(port));
				//Log.i("Client TAsk of port  " + myPort," Sending "+ msgToSend+" to port "+port);
				DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
				//Log.i("Client TAsk","Creating Op stream to write msg "+msgToSend);
				dataOutputStream.writeUTF(msgToSend);
				//Log.i("Client TAsk","Sent Msg  "+msgToSend);

               /* DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                Log.i("Client TAsk","Creating IP stream");
                String receivedFromServer = dataInputStream.readUTF();
                Log.i("Client TAsk","Receiving from IP stream");*/
				Thread.sleep(50);
				socket.close();
				//Log.i("Client Task","Closed socket  " + port);


			} catch (UnknownHostException e) {
				Log.e(myPort, "ClientTask UnknownHostException");
			} catch (IOException e) {
				Log.e(myPort, "ClientTask socket IOException" + e);
			} catch (NullPointerException e){
				Log.i("Client TAsk","Null poniter exception caught");
			}catch (Exception e){
				Log.i("ClientTask", "trying to catch all exceptions");
			}
			return null;
		}
	}

}
