package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Common.AttachedFile;
import Common.MyFile;
import Common.User;
import Utilities.MessageObject;
import Utilities.RequestType;
import jdbc.mysqlConnection;
import ocsf.server.ConnectionToClient;

public class ServerRequestHandler {
	private jdbc.SqlRequestHandler mysqlRequestHandler = null;

	// Instance methods ************************************************
	/**
	 * This function handles messages from clients
	 * 
	 * @param msg    Message from Client
	 * @param client Client that sent the message
	 */
	public void handle(Object msg, ConnectionToClient client) {
		if (msg instanceof MessageObject) {
			// cast to MessageObject and send a print that a message was received
			MessageObject message = (MessageObject) msg;

			DBServer.getInstance().printMessageRecieved(message, client);

			mysqlRequestHandler = mysqlConnection.getInstance().handle();

			MessageObject responseMessage = null;
			switch (message.getTypeRequest()) {
			case LogOut:
				handleLogOut(message, client);
				break;
			case Login:
				responseMessage = handleLogin(message, client);
				break;
			case View_Req_Details:
				responseMessage = handleSearchRequest(message, client);
				break;
			case change_Status:
				responseMessage = handleChangeStatus(message, client);
				break;
			case viewRequestTable:
			case refreshViewUserRequestTable:
				responseMessage = handleViewRequestTable(message, client);
				break;
			case NewChangeRequest:
				responseMessage = handleNewChange(message, client);
				break;
			case AttachFile:
				handleAttachFile(message, client);
				break;
			case ViewAttachedFiles:
				responseMessage = handleViewAttachedFiles(message);
				break;
			case DownloadAttachedFiles:
				responseMessage = handleDownloadFile(message);
				break;
			case ApprovedEvaluator:
				responseMessage = handleApprovedEvaluator(message);
				break;
			case InformationSystem_Details:
				responseMessage = handleInformationSystemDetails(message, client);
				break;
			case AllUserDetails:
				responseMessage = handleAllUserDetails(message, client);
				break;
			case PermanentRoles_Details:
				responseMessage = handlePermanentRolesDetails(message, client);
				break;
			case UpdateEvaluator:
				handleUpdateEvaluator(message, client);
				break;
			case UpdatePermanentRoles:
				handleUpdatePermanentRoles(message, client);
				break;
			case ViewEvaluatorTable:
				responseMessage = handleEvaluatorTable(message, client);
				break;
			case UploadEvaluatorReport:
				responseMessage = handleUploadEvaluatorReport(message, client);
				break;
			case ViewIseTable:
				responseMessage = handleISETable(message, client);
				break;
			default:
				break;
			}

			if (responseMessage != null)
				DBServer.getInstance().sendMessage(responseMessage, client);
		} else
			System.out.println("Error - Message rechieved is not a MessageObject");
	}

	/**
	 * bring from the database Table of all ISE workers for Supervisor to pick new
	 * Evaluator when he reject the automatic Appoitment
	 * 
	 */
	private MessageObject handleISETable(MessageObject message, ConnectionToClient client) {
		return mysqlRequestHandler.viewIseTable(message);
	}

	/**
	 * this method uploads the evaluator report to the relevant table in the db
	 * 
	 * @param message
	 * @param client
	 * @return
	 */
	private MessageObject handleUploadEvaluatorReport(MessageObject message, ConnectionToClient client) {

		boolean result = mysqlRequestHandler.uploadEvaluatorReport((String) message.getArgs().get(3),
				(String) message.getArgs().get(0), (String) message.getArgs().get(1), (String) message.getArgs().get(2),
				(String) message.getArgs().get(4));
		message.getArgs().clear();
		message.getArgs().add(result);
		return message;

	}

	/**
	 * when a user presses log out this method logges him out from the server
	 * 
	 * @param message
	 * @param client
	 */
	private void handleLogOut(MessageObject message, ConnectionToClient client) {
		String userID = (String) message.getArgs().get(0);

		if (UserConnectivityManager.getInstance().logOutUser(userID)) {
		} else
			System.out.println("Error some one hacked the system");
	}

	private void handleUpdatePermanentRoles(MessageObject message, ConnectionToClient client) {
		try {
			mysqlRequestHandler.updatePermanentRoles(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private MessageObject handleEvaluatorTable(MessageObject message, ConnectionToClient client) {
		return mysqlRequestHandler.viewEvaluatorTable(message);
	}

	private void handleUpdateEvaluator(MessageObject message, ConnectionToClient client) {
		try {
			mysqlRequestHandler.updateEvaluator(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * delete Approved Evaluator from Evaluator Appointment Table update Request
	 * Evaluator in Requests table Insert Stage Evaluator in StageTable
	 * 
	 * @param message contains requestID and
	 * @return messege object contains requestID and
	 */
	public MessageObject handleApprovedEvaluator(MessageObject message) {
		String requestID = (String) message.getArgs().get(0);
		String evaluatrorID = (String) message.getArgs().get(1);

		mysqlRequestHandler.deleteApprovedEvaluator(requestID);
		mysqlRequestHandler.updateRequestEvaluator(requestID, evaluatrorID);
		mysqlRequestHandler.insertStageEvaluator(requestID);
		return message;

	}

	public MessageObject handleDownloadFile(MessageObject message) {
		String[] fileNames = (String[]) message.getArgs().get(0);
		String requestID = (String) message.getArgs().get(1);
		String dir = System.getProperty("user.dir");
		File file;

		MyFile[] downloadFileList = new MyFile[fileNames.length];
		int i = 0;
		for (String fileName : fileNames) {
			file = new File(dir + "\\RequestsAttachedFiles\\" + requestID + "\\" + fileName);
			downloadFileList[i] = downloadFile(file);
			i++;
		}
		message.getArgs().clear();
		message.setTypeRequest(RequestType.DownloadAttachedFiles);
		message.getArgs().add(downloadFileList);
		return message;

	}


	public MyFile downloadFile(File file) {
		MyFile downloadedFile = new MyFile(file.getName());
		String localFilePath = file.getAbsolutePath();

		byte[] myByteArray = new byte[(int) file.length()];
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

			downloadedFile.initArray(myByteArray.length);
			downloadedFile.setSize(myByteArray.length);

			bufferedInputStream.read(downloadedFile.getMybytearray(), 0, myByteArray.length);

			return downloadedFile;
		} catch (Exception e) {

		}
		return null;

	}

	/**
	 * this method returns a string array to the client with all the names of the
	 * attached files of this request
	 * 
	 * @param message
	 * @return
	 */
	public MessageObject handleViewAttachedFiles(MessageObject message) {
		String requestID = (String) message.getArgs().get(0);
		 String dir = System.getProperty("user.dir");
		File file = new File(dir + "\\RequestsAttachedFiles\\" + requestID);

		String[] stringFileArray = null;
		if (file.exists())
			stringFileArray = file.list();

		message.getArgs().remove(0);
		message.getArgs().add((Object) stringFileArray);

		return message;

	}


	/**
	 * this method handles a creation of new change request
	 * 
	 * @param message
	 * @param client
	 * @return MessageObject that should be sent back to the client, indicating
	 *         specific request response.
	 */
	public MessageObject handleNewChange(MessageObject message, ConnectionToClient client) {
		boolean res = mysqlRequestHandler.addCRToDB(message.getArgs());

		ArrayList<Object> args = new ArrayList<Object>();

		String requestID = mysqlRequestHandler.getLastInsertID();

		args.add(res);
		args.add(requestID);

		message.setTypeRequest(RequestType.NewChangeRequest);
		message.setArgs(args);
		return message;
	}

	/**
	 * This method handles any attached files received from the client.
	 *
	 * @param msg    The message received from the client.
	 * @param client The connection from which the message originated.
	 */
	public void handleAttachFile(MessageObject message, ConnectionToClient client) {
		if (message.getArgs().get(0) instanceof MyFile) {
			MyFile attachedFile = (MyFile) message.getArgs().get(0);
			String dir = System.getProperty("user.dir");
			// add check to see if the user is active or not
			String requestID = (String) message.getArgs().get(1);

			createFolder(requestID);

			File newFile = new File(dir + "\\RequestsAttachedFiles\\" + requestID + "\\" + attachedFile.getFileName());
			try {

				byte[] myByteArray = attachedFile.getMybytearray();
				FileOutputStream fos = new FileOutputStream(newFile);
				BufferedOutputStream bos = new BufferedOutputStream(fos);

				bos.write(myByteArray, 0, myByteArray.length);
				bos.flush();
				fos.flush();
				bos.close();
			} catch (FileNotFoundException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

	}

	/**
	 * this method handles a login request from the client by extracting the
	 * username and password from MessageObject checking if its in the the db and
	 * sending back the message to the client if it was found or not
	 * 
	 * @param message
	 * @param client
	 * @return MessageObject that should be sent back to the client, indicating
	 *         specific request response.
	 */
	public MessageObject handleLogin(MessageObject message, ConnectionToClient client) {
		try {
			return mysqlRequestHandler.checkUserCredentials(message.getArgs().get(0).toString(),
					message.getArgs().get(1).toString(), client);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * A method to search a request by id in the db and return its data
	 * 
	 * @param message
	 * @param client
	 * @return MessageObject that should be sent back to the client, indicating
	 *         specific request response.
	 */
	public MessageObject handleSearchRequest(MessageObject message, ConnectionToClient client) {
		try {
			return mysqlRequestHandler.searchRequest(message);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * adding function to change status in DB and return true or false to client if
	 * succeeded or not
	 * 
	 * @param message
	 * @param client
	 * @return MessageObject that should be sent back to the client, indicating
	 *         specific request response.
	 */
	public MessageObject handleChangeStatus(MessageObject message, ConnectionToClient client) {
		try {
			return mysqlRequestHandler.changeStatus((message));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public MessageObject handleViewRequestTable(MessageObject message, ConnectionToClient client) {
		try {
			return mysqlRequestHandler.viewRequestTable(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public void createFolder(String path) {

		String dir = System.getProperty("user.dir");
		File file = new File(dir + "\\RequestsAttachedFiles");
		if (!file.exists()) {
			if (file.mkdir()) {
				System.out.println("Directory is created: " + dir + " \\RequestsAttachedFiles");
			} else {
				System.out.println("Failed to create directory!: " + path);
			}
		}
		file = new File(dir + "\\RequestsAttachedFiles\\" + path);
		if (!file.exists()) {
			if (file.mkdir()) {
				System.out.println("Directory is created: " + dir + " \\RequestsAttachedFiles\\" + path);
			} else {
				System.out.println("Failed to create directory!: " + path);
			}
		}

	}



	/**
	 * This method handles the Details of Information Systems
	 * (informationSystemName, currentEvaluatorID)
	 */
	private MessageObject handleInformationSystemDetails(MessageObject message, ConnectionToClient client) {
		try {
			return mysqlRequestHandler.getInformationSystemDetails(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/** This method handles the Details of All Users (Name, ID) */
	private MessageObject handleAllUserDetails(MessageObject message, ConnectionToClient client) {
		try {
			return mysqlRequestHandler.getAllUserDetails(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * This method handles the Details of Users with Permanent Roles (roleName, ID)
	 */
	private MessageObject handlePermanentRolesDetails(MessageObject message, ConnectionToClient client) {
		try {
			return mysqlRequestHandler.getPermanentRolesDetails(message);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
