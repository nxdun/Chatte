package chatserver;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import javax.swing.*; //  used to create a list, which can be customized to select multiple list items
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * IT21800900
 * A simple Swing-based client for the chat server. Graphically it is a frame
 * with a text field for entering messages and a textarea to see the whole
 * dialog.
 *
 * The client follows the Chat Protocol which is as follows. When the server
 * sends "SUBMITNAME" the client replies with the desired screen name. The
 * server will keep sending "SUBMITNAME" requests as long as the client submits
 * screen names that are already in use. When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start sending the server
 * arbitrary strings to be broadcast to all chatters connected to the server.
 * When the server sends a line beginning with "MESSAGE " then all characters
 * following this string should be displayed in its message area.
 */

public class ChatClient {

	//this string for displaying names
	static String Cname = "Unknown";

	BufferedReader in;
	PrintWriter out;
	JFrame frame = new JFrame("Chatte.X(Unregisterd)");
	JTextField textField = new JTextField(40);
	JTextArea messageArea = new JTextArea(8, 40);
	JList<String> clientList;
	DefaultListModel<String> clientListModel;

	JCheckBox broadcastCheckbox; // Declare checkbox

	/**
	 * Constructs the client by laying out the GUI and registering a listener with
	 * the textfield so that pressing Return in the listener sends the textfield
	 * contents to the server. Note however that the textfield is initially NOT
	 * editable, and only becomes editable AFTER the client receives the
	 * NAMEACCEPTED message from the server.
	 */
	public ChatClient() {

		// Layout GUI
		textField.setEditable(false);
		messageArea.setEditable(false);
		clientListModel = new DefaultListModel<>();
		clientList = new JList<>(clientListModel);
		clientList.setCellRenderer(new ClientListCellRenderer());
		frame.getContentPane().setLayout(new BorderLayout());

		// Create a panel to hold the text field and checkboxes
		JPanel textFieldPanel = new JPanel(new BorderLayout());
		textFieldPanel.add(textField, BorderLayout.CENTER);

		// Create a panel for checkboxes
		JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));

		// Create two checkboxes
		JCheckBox broadcastCheckbox = new JCheckBox("Send To All");
		broadcastCheckbox.setSelected(true);

		// Add both checkboxes to the panel
		checkboxPanel.add(broadcastCheckbox);

		// Add checkboxPanel to textFieldPanel
		textFieldPanel.add(checkboxPanel, BorderLayout.EAST);

		// Add textFieldPanel to frame
		frame.getContentPane().add(new JScrollPane(clientList), BorderLayout.WEST);
		frame.getContentPane().add(textFieldPanel, BorderLayout.NORTH);
		frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
		frame.pack();

		
		textField.addActionListener(new ActionListener() {
			/**
			 * Responds to pressing the enter key in the textfield by sending the contents
			 * of the text field to the server. Then clear the text area in preparation for
			 * the next message.
			 */
			public void actionPerformed(ActionEvent e) {
				List<String> selectedClients = clientList.getSelectedValuesList();

				if (broadcastCheckbox.isSelected()) { // Check if broadcast is enabled
					out.println(textField.getText()); // Broadcast message to all clients
				} else {
					if (!selectedClients.isEmpty()) {
						for (String client : selectedClients) {
							out.println(client + ">>" + textField.getText()); // Send message to selected clients
						}
					} else {
						out.println(textField.getText()); // Broadcast if no client selected
					}
				}
				textField.setText("");

			}

		});

	}

	/**
	 * Prompt for and return the address of the server.
	 */
	private String getServerAddress() {
		return JOptionPane.showInputDialog(frame, "Enter IP Address of the Server:", "Welcome to the Chatter",
				JOptionPane.QUESTION_MESSAGE);
	}

	/**
	 * Prompt for and return the desired screen name.
	 */
	private String getName() {
		// takes name to a variable
		Cname = JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
				JOptionPane.PLAIN_MESSAGE);
		return Cname;
	}

	/**
	 * Connects to the server then enters the processing loop.
	 */
	private void run() throws IOException {

		// Make connection and initialize streams
		String serverAddress = getServerAddress();
		// client and server must run on same socket
		Socket socket = new Socket(serverAddress, 9005);
		// recived from server
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		// send to server
		out = new PrintWriter(socket.getOutputStream(), true);

		// Process all messages from server, according to the protocol.
		// infinite loop
		while (true) {
			String line = in.readLine();

			// handling events
			if (line.startsWith("SUBMITNAME")) {
				out.println(getName());
				frame.setTitle("Chatte.X  (" + Cname + ")");
			} else if (line.startsWith("NAMEACCEPTED")) {
				textField.setEditable(true);
			} else if (line.startsWith("MESSAGE")) {
				messageArea.append(line.substring(8) + "\n");
			} else if (line.startsWith("PRIVATEMESSAGE")) {

				// split string to subparts and takes sender reciver individually
				// line = "PRIVATEMESSAGE <sender> <reciver> !!<message>"
				String[] parts = line.split(" ");
				String mSender = parts[1].trim();
				String mReceiver = parts[2].trim();
				String recivedMessage = line.split("!!")[1];

				// display logic for private message
				// sender and reciver can only see pm
				if (Cname.equals(mSender) || Cname.equals(mReceiver)) {

					if (mSender.equals(mReceiver)) {
						messageArea.append("You sent yourself :? ");
					} else {
						messageArea.append(
								"(private) FROM: " + mReceiver + " TO: " + mSender + "   :   " + recivedMessage + "\n");
					}

				}
			} else if (line.startsWith("CLIENTLIST")) {

				// takes all data after clientlist^
				String[] clients = line.substring(11).split(",");

				// set all recived data
				updateClientList(clients);
			}
		}
	}

	// method to set all clients
	private void updateClientList(String[] clientNames) {
		SwingUtilities.invokeLater(() -> {
			clientListModel.clear();
			for (String clientName : clientNames) {
				clientListModel.addElement(clientName);
			}
		});
	}

	// checkboxes renders by bypassing previous method
	class ClientListCellRenderer extends JCheckBox implements ListCellRenderer<String> {
		/**
		 * automatically added serial vuid
		 */
		private static final long serialVersionUID = 1L;

		public ClientListCellRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
				boolean isSelected, boolean cellHasFocus) {
			setText(value);
			setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
			setEnabled(list.isEnabled());
			setSelected(list.isSelectedIndex(index));
			return this;
		}
	}

	/**
	 * Runs the client as an application with a closeable frame.
	 * this is main function
	 */
	public static void main(String[] args) throws Exception {
		ChatClient client = new ChatClient();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}
}