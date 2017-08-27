import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Server{

	private ServerSocket serverSocket = null;
	private List<ServerThreads> clientSockets = new ArrayList<ServerThreads>();
		
	public Server(int serverPort){		
		try {
			System.out.println("Starting CN Project Server at port number - " + serverPort);
			serverSocket = new ServerSocket(serverPort); // Initialize the server socket at the port specified by user.
			System.out.println("Server started and running....");
			startServer();
		} catch (IOException e) {
			System.out.println("Failed to start server.");
			System.out.println(e.toString());	
			if(serverSocket != null){
				try {
					serverSocket.close();
				} catch (IOException ex) {
					System.out.println("Failed to close server socket at port - "+ serverSocket.getLocalPort());
//					System.out.println(ex.toString());
				}
			}
		}
	}
    /**
     * Starts the server and accepts socket when client communicates with server.
     *
     */ 	
	  public void startServer(){
		  while(true){
			  try {
				System.out.println("Waiting for client connection...");
				Socket socket = serverSocket.accept(); // Accept the socket of new client connection.
				ServerThreads serverThread = new ServerThreads(this, socket, "Client"+socket.getPort()); 
				clientSockets.add(serverThread); // Add socket in the list of client sockets.
				System.out.println("Client"+socket.getPort()+" connected at Socket - "+ socket.getPort());
				serverThread.start();
			} catch (IOException e) {
				System.out.println("Failed to initialize client socket.");
//				System.out.println(e.toString());
			}
		  }
	  }

	    /**
	     * Sends the message to every client included in Arraylist except the sender.
	     *
	     */ 	  
	public synchronized void broadcastMessage(String str, String clientId){
		for(ServerThreads st : clientSockets){
			if(!st.getClientId().equals(clientId)){
				st.sendData("@"+clientId+": "+str);
			}			
		}
	}
    /**
     * Sends the file to every client included in Arraylist except the sender.
     *
     */ 	
	public void broadcastFile(String str, String fileContent, String clientId){
		for(ServerThreads st : clientSockets){
			if(!st.getClientId().equals(clientId)){
				st.sendData("File: \"" + str + "\" was sent by " + clientId + "-" + fileContent);
			}			
		}		
	}
    /**
     * Sends the message to every client specified by sender.
     *
     */ 	
	public synchronized void unicastMessage(String str, String senderClientId, String receiverClientId){
		for(ServerThreads st : clientSockets){
			if(st.getClientId().equalsIgnoreCase(receiverClientId)){
				st.sendData("@"+senderClientId+": "+str);
			}			
		}
	}
	   /**
     * Sends the file to every client specified by sender.
     *
     */	
	public void unicastFile(String str, String fileContent, String senderClientId , String receiverClientId){
		for(ServerThreads st : clientSockets){
			if(st.getClientId().equalsIgnoreCase(receiverClientId)){
				System.out.println("sent");
				st.sendData("File: \"" + str + "\" was sent by " + senderClientId + "-" + fileContent);
			}			
		}	
	}
	 /**
     * Sends the message to every client included in CLient List except the blocked user.
     *
     */	
	public synchronized void blockCastMessage(String str, String senderClientId, String blockedClientId){
		for(ServerThreads st : clientSockets){
			if(!st.getClientId().equalsIgnoreCase(senderClientId) && !st.getClientId().equalsIgnoreCase(blockedClientId)){
				st.sendData("@"+senderClientId+": "+str);
			}			
		}
	}
	   /**
	    * Sends the file to every client included in CLient List except the blocked user.
	    *
	    */		
	public void blockCastFile(String str, String fileContent, String senderClientId, String blockedClientId){
		for(ServerThreads st : clientSockets){
			if(!st.getClientId().equalsIgnoreCase(senderClientId) && !st.getClientId().equalsIgnoreCase(blockedClientId)){
				
				System.out.println("sending");
				st.sendData("File: \"" + str + "\" was sent by " + senderClientId + "-" + fileContent);
			}			
		}		
	}		
	
	public synchronized void removeClient(String clientId){		
		clientSockets.remove(clientId);
		System.out.println(clientId +" disconnected.");
	}
	  
	
	public static void main(String[] args) {
		new Server(9800);
	}
}

/**
 * Creates a Server thread dedicated to a unique client.
 *
 * @return thread
 */ 
class ServerThreads extends Thread{
	private OutputStreamWriter out = null;  //stream write to the socket
	private InputStreamReader in = null;    //stream read from the socket
	private Server server = null;
	private Socket socket = null;
	private BufferedReader br = null;
	BufferedWriter writer = null;
	private String clientId = "";
	
	public String getClientId(){
		return clientId;
	}

	public ServerThreads(Server server, Socket socket, String clientId){
		this.socket = socket;
		this.server = server;
		this.clientId = clientId;
	}
    /**
     * Initialize the Input and Output Stream.
     * 
     */ 	
	public void startCommunication(){
		try {
			in = new InputStreamReader(socket.getInputStream());
			out = new OutputStreamWriter(socket.getOutputStream());
			br = new BufferedReader(in);
			writer  = new BufferedWriter(out);
			sendData("You are connected to server and your unique client ID is "+ clientId);
		} catch (IOException e) {
			System.out.println("Error in reading or writing data from socket.");
		}		
	}
	
	   public void closeConnection() throws IOException
	   {  
		  if (socket != null)    socket.close();
	      if (in != null)  in.close();
	      if (out != null) out.close();
	      server.removeClient(clientId);
	   }	
	
	public void sendData(String message){
		try {
			writer.write(message+"\n");
			writer.flush();
		} catch (IOException e) {
//			e.printStackTrace();
			System.out.println("IO Exception in "+clientId+" while writing data.");
		}		
	}
	
	@Override
	public void run() {
		startCommunication();	
		
		try {
			while(true){
				String str;			
				str = (String) br.readLine();
				if(str != null){
					String text="";
					String fileContent = "";
					String receiverClient="";
					int secondIndex=0;
					try{
						int firstIndex = str.indexOf("\"")+1;
						secondIndex= str.indexOf("\"", firstIndex+1);
						text=str.substring(firstIndex, secondIndex);
						if(str.contains("broadcast") && str.contains("file")){
							fileContent = str.substring(secondIndex+2);
						}
						if(str.contains("file") && (str.contains("unicast") || str.contains("blockcast"))){
							String[] ss = str.substring(secondIndex+1).split("-");
							receiverClient = ss[0];
							fileContent = ss[1];
						}
						
					}catch(Exception e){
						sendData("Incorrect Message.");
						continue;
					}
	
					if(str.contains("broadcast")){
						if(str.contains("file")){							
							server.broadcastFile(text, fileContent, clientId);
						}else{
							server.broadcastMessage(text,clientId);
						}
					}else if(str.contains("unicast")){
						if(str.contains("file")){
							server.unicastFile(text, fileContent, clientId,receiverClient.trim());
						}else{
							String rc = str.substring(secondIndex+1);
							server.unicastMessage(text,clientId,rc.trim());
						}						
					}else if(str.contains("blockcast")){
						if(str.contains("file")){
							server.blockCastFile(text, fileContent,clientId,receiverClient.trim());
						}else{
							String bc = str.substring(secondIndex+1);
							server.blockCastMessage(text,clientId,bc.trim());
						}							
					}else{
						sendData("Incorrect Message.");
					}
				}
			}
		} catch (IOException e) {
			System.out.println("IO Exception in "+clientId);
		}finally{
			try {
				closeConnection();
			} catch (IOException e) {
				System.out.println("IO Exception in "+clientId+" while closing socket.");
			}
		}
	}
	
	
}
