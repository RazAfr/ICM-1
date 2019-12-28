package client;

import ocsf.client.*;
import java.io.*;

import Gui.ClientUI;
import Utilities.MessageObject;
import Utilities.ScreenManager;

/**
 * This class overrides some of the methods defined in the abstract superclass
 * in order to give more functionality to the client.
 *
 */
public class Client extends AbstractClient {
	// Instance variables **********************************************

	private static Client singletonInstance = null;
	private String userID;
	/**
	 * Instance of screenManager witch saves all scenes and the stage with this
	 * screenManager you can add new scenes and switch between them
	 */

	// Constructors ****************************************************

	/**
	 * Constructs an instance of the chat client.
	 *
	 * @param host     The server to connect to.
	 * @param port     The port number to connect on.
	 * @param clientUI The interface type variable.
	 */

	private Client(String host, int port) throws IOException {
		super(host, port); // Call the superclass constructor
		openConnection();
	}
	
	/**
	 * Get the Singleton's Instance
	 * @return Client Singleton Instance
	 * @throws IOException 
	 */
	public static Client getInstance() {
		if (singletonInstance == null)
			try {
			singletonInstance = new Client(ClientUI.DEFAULT_SERVER, ClientUI.DEFAULT_PORT);
			} catch (Exception ex) {
				// ex.printStackTrace();
			}
		return singletonInstance;
	}
	
	public static void initialize(String host, int port) throws IOException {
		singletonInstance = new Client(host, port);
	}

	// Instance methods ************************************************

	/**
	 * This method handles all data that comes in from the server. messages from
	 * server should only come as MessageObjects. this method checks what kind of
	 * Request was handled by the server and calls the right controller into action
	 * 
	 * @param msg The message from the server.
	 */
	public void handleMessageFromServer(Object msg) {
		RequestHandler.getInstance().handle(msg);
	}

	/**
	 * A method to switch Scene's on the main stage method needs the fxml file name
	 * (without .fxml)
	 * 
	 * @param fxml
	 */
	public void switchScene(String fxml) {
		try {
			ScreenManager.getInstance().switchScene(fxml);
		} catch (Exception e) {
			System.out.println("Error occured while trying to switch to  scene: " + fxml);
			e.printStackTrace();
		}
	}
	
	public Boolean sceneExists(String fxml_name) {
		return ScreenManager.getInstance().sceneExists(fxml_name);
	}

	/**
	 * returns the instance of the current controller that is being used by a scene
	 * it is important in order to use the controllers methods when you switch
	 * between new scene's
	 * 
	 * @return
	 */
	public Object getCurrentFX() {
		return ScreenManager.getInstance().getCurrentFX();
	}

	/**
	 * This method handles all data coming from the UI
	 *
	 * @param message The message from the UI.
	 */
	public void handleMessageFromClientUI(String message) {
		try {
			sendToServer(message);

			System.out.println("Message sent: " + message + "from Client");

		} catch (IOException e) {
			System.out.println("Error Message wasnt sent to the server");
			e.printStackTrace();
			quit();
		}
	}

	/**
	 * This method terminates the client.
	 */
	public void quit() {
		try {
			closeConnection();
		} catch (IOException e) {
		}
		System.exit(0);
	}

	/**
	 * this method prints the message that was received from the server
	 * 
	 * @param message
	 */
	public void printMessageRecieved(MessageObject message) {
		System.out.println("Message recieved: " + (message).getTypeRequest().toString() + " | "
				+ (message).getArgs().toString() + " from server");

	}

	public void setUserID(String id) {
		userID = id;
	}

	public String getUserID() {
		return userID;
	}
}
//End of ChatClient class
