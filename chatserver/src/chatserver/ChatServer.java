package chatserver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9005;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<String>();
    
    //synchronized names for thread safety
    public synchronized static void addName(String NewName) {
    	names.add(NewName);
    }
    
    public synchronized static void remName(String RemName) {
    	names.remove(RemName);
    }

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    
    /**
     * The application main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
            	Socket socket  = listener.accept();
                Thread handlerThread = new Thread(new Handler(socket));
                handlerThread.start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        
     //modified the run() method to call broadcastLoggedInClients() whenever a client joins or leaves 
        
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME"); //sending to client
                    name = in.readLine(); //recived from client
                    if (name == null) {
                        return;
                    }
                    
                    // Added code to ensure the thread safety of the
                    // the shared variable 'names'
                    if (!names.contains(name)) {
                    		addName(name);
                            break;
                        }
                    
                 }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");  //sent to other class
                writers.add(out);
                broadcastLoggedInClients();// Broadcast updated client list
                
                //sends a welcome message
                for (PrintWriter writer : writers) {
                	writer.println("MESSAGE ...Hi !! " + name + " welcome to our chat server...");
                }
                
   
                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                while (true) {
                	
                    String input = in.readLine();
                   
                    
                    if (input.contains(">>")) {
                        String[] parts = input.split(">>");
                        String receiver = parts[0].trim();
                        String message = parts[1].trim();

                        // Iterate through the names to find the receiver
                        for (String n : names) {
                            if (n.equals(receiver)) {
                                // If the receiver is found, iterate through the writers
                                for (PrintWriter writer : writers) {
                                    // If the writer matches the sender or receiver, send the message
                                	// line = "PRIVATEMESSAGE <sender> <reciver> !!<message>"
                                    if (writer.equals(out) || n.contains(receiver)) {
                                       writer.println("PRIVATEMESSAGE " + receiver + " " + name +" !!"+ message  );
                                    }
                                }
                                break; // Break the loop if the receiver is found and the message is sent
                            }
                        }
                    } else {
                        // If the message doesn't contain >>, send it to all clients :}
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + ":"
                            		+ "" + input);
                        }
                    	
                    }
                    
                    
                    
                }
            }// TODO: Handle the SocketException here to handle a client closing the socket
            catch (IOException e) {
                System.out.println(e);
                out.println("MESSAGE someone leaved from the server");
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                	remName(name);
                }
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
                broadcastLoggedInClients();// Broadcast updated client list
            }
            
        }
     // Broadcast the list of logged-in clients to all clients
     //so it can displayed in every clients
        private synchronized void broadcastLoggedInClients() {
            StringBuilder clientListMessage = new StringBuilder();
            clientListMessage.append("CLIENTLIST ");
            for (String client : names) {
                clientListMessage.append(client).append(",");
            }
            for (PrintWriter writer : writers) {
                writer.println(clientListMessage);
            }
        }
    }
}