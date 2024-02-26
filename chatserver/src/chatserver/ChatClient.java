package chatserver;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import javax.swing.*;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {
	
	static String Cname;

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatte.X");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    JList<String> clientList;
    DefaultListModel<String> clientListModel;

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        frame.getContentPane().add(new JScrollPane(clientList), BorderLayout.WEST);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();

        // TODO: You may have to edit this event handler to handle point to point messaging,
        // where one client can send a message to a specific client. You can add some header to 
        // the message to identify the recipient. You can get the receipient name from the listbox.
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
        	public void actionPerformed(ActionEvent e) {
                List<String> selectedClients = clientList.getSelectedValuesList();
                if (!selectedClients.isEmpty()) {
                    for (String client : selectedClients) {
                        out.println(client + ">>" + textField.getText());
                    }
                } else {
                    out.println(textField.getText());
                }
                textField.setText("");
            }
        });
        
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chatter",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
    	//takes name to a variable
          Cname =  JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
         return Cname;
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9004);
        //recived from server
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        //send to server
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("PRIVATEMESSAGE")) {
            	
            	
            	
            	
            	//split string to subparts and takes sender reciver individually
            	//line = "PRIVATEMESSAGE <sender> <reciver> !!<message>"
            	String[] parts = line.split(" ");
            	String mSender = parts[1].trim();
            	String mReceiver = parts[2].trim();
                String recivedMessage = line.split("!!")[1];
                
                //display logice for private message
                //sender and reciver can only see pm
            	if(Cname.equals(mSender)|| Cname.equals(mReceiver)) {
            		
                messageArea.append("(private) FROM:" +mSender+ " TO:"+mReceiver+ "   : "+ recivedMessage + "\n");
                
            	}
            }
            else if (line.startsWith("CLIENTLIST")) {
                String[] clients = line.substring(11).split(",");
                updateClientList(clients);
            }
        }
    }
        
        private void updateClientList(String[] clientNames) {
            SwingUtilities.invokeLater(() -> {
                clientListModel.clear();
                for (String clientName : clientNames) {
                    clientListModel.addElement(clientName);
                }
            });
        }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}