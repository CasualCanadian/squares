package com.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Multiplayer game
 * 
 * @since June 16, 2020
 * @author G. Carey
 */

public class Server implements Runnable {

	//maximum number of clients able to join the server at the same time
	public static final int MAX_CLIENTS = 50;
	//maximum number of bots
	public static final int MAX_BOTS = 50;
	
	private User[] user = new User[MAX_CLIENTS];
	private ProbePacket[] probePacket = new ProbePacket[MAX_CLIENTS];
	
	//server and client sockets
	private ServerSocket serverSocket;
	private Socket socket;
	private Socket[] clientSocket = new Socket[MAX_CLIENTS];
	//data input and output streams
	private DataOutputStream outputStream;
	private DataInputStream inputStream;
	private int port;
	
	private ServerCommand serverCommand;
	private ServerFileManager serverFileManager;
	private ObjectManager objectManager;
	
	private Thread serverThread;
	private Thread objectManagerThread;
	private Thread[] probePacketThread = new Thread[MAX_CLIENTS];
	private boolean serverRunning;
	//number of current active clients
	private int numClients = 0;
	private InetAddress clientIP;
	private boolean hideIP = false;
	
	//size of the map
	public static int mapSize = 4000;
	
	//scanner for server command input
	private Scanner scanner;
	
	public static void main(String[] args) {
		new Server();
	}
	
	public Server() {
		//initialize scanner for system input
		scanner = new Scanner(System.in);
		
		//initialize server commands and the server file manager
		try {
			serverCommand = new ServerCommand(this);
			serverFileManager = new ServerFileManager();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		//input valid port number to host server
		System.out.print("Enter port number to host server...");
		while(!(port/1 == port) || port < 1 || port > 65535) {
			try {
				port = scanner.nextInt();
			} catch(Exception e) {
			}
			//fail safe if port is invalid
			if(!(port/1 == port) || port < 1 || port > 65535) {
				System.out.println("Invalid port");
				System.out.print("Enter a valid port number...(1 - 65535)");
				scanner.nextLine();
			}
		}
		scanner.hasNextLine();
		System.out.println("Server Starting...");
		
		//attempt to host server with port, set running condition to true
		try {
			serverSocket = new ServerSocket(port);
			serverRunning = true;
		} catch(IOException e) {
			e.printStackTrace();
			System.err.printf("Unable to start server with port: %d\n", port);
		}
		System.out.printf("Server started with port: %d\n", port);
		
		//initialize and start main server thread for commands and messages
		serverThread = new Thread(this);
		serverThread.start();
		
		//initialize the object manager to manage and update all game objects
		objectManager = new ObjectManager(this, serverFileManager);
		objectManagerThread = new Thread(objectManager);
		objectManagerThread.start();
		
		//accept and handle clients
		while(serverRunning) {
			//begin accepting clients
			try {
				socket = serverSocket.accept();
				clientIP = socket.getInetAddress();
				numClients++;
				
				//initialize data and input and output streams with client socket for exchanging data with the client
				outputStream = new DataOutputStream(socket.getOutputStream());
				inputStream = new DataInputStream(socket.getInputStream());
			} catch(IOException e) {
				System.out.println("Client failed to connect");
			}
			
			try {
				//is the client's IP is on the banned list
				if(!serverFileManager.isRestrictedIP(clientIP.toString()) && numClients <= MAX_CLIENTS) {
					if(!hideIP) System.out.printf("Client connection from: " + clientIP + ": %s\n", numClients + "/" + MAX_CLIENTS);
					else if(hideIP) System.out.printf("Client connected: %s\n", numClients + "/" + MAX_CLIENTS);
					
					for(int i = 0; i < MAX_CLIENTS; i++) {
						if(user[i] == null) {
							user[i] = new User(inputStream, outputStream, user, clientIP, i, serverFileManager, objectManager);
							probePacket[i] = new ProbePacket(this, outputStream, i);
							probePacketThread[i] = new Thread(probePacket[i]);
							probePacketThread[i].start();
							clientSocket[i] = socket;
							
							Thread userThread = new Thread(user[i]);
							userThread.start();
							break;
						}
					}
				//is the client's IP is on the temporary banned list
				} else if(serverFileManager.isRestrictedIP(clientIP.toString()) && numClients <= MAX_CLIENTS) {
					outputStream.writeUTF("[banned]");
					socket.close();
					socket = null;
					if(!hideIP) System.out.println("Restricted user from: " + clientIP + " attempted to join the server");
					else if(hideIP) System.out.println("Restricted user attempted to join the server");
					//if the server is full
				} else if(numClients > MAX_CLIENTS) {
					outputStream.writeUTF("[full]");
					socket.close();
					socket = null;
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	@Override
	public void run() {
		while(serverRunning) {
			//wait for next server command
			String command = new String(scanner.nextLine());
			if(command.length() > 0 && command .charAt(0) == '/') {
				enterCommand(command);
			} else if(command.length() > 0 && command.charAt(0) != '/') {
				int clientsReached = 0;
				for(int i = 0; i < MAX_CLIENTS; i++) {
					//loop through entire array of users, if user is not null then send the message
					if(user[i] != null) {
						try {
							user[i].getDataOutputStream().writeUTF("<Message from Server> " + command);
						} catch (IOException e) {
							disconnectClient(i);
						}
						clientsReached++;
					}
					//no clients have been reached, notify that the server is empty
					if(i == MAX_CLIENTS-1 && clientsReached == 0) System.out.println("No clients connected");
				}
			}
		}
	}
	
	public boolean isRunning() {
		return serverRunning;
	}
	
	public boolean getIPStatus() {
		return hideIP;
	}
	
	public User getUser(int index) {
		return user[index];
	}
	
	//disconnect a client and close all user-related sockets, threads, and objects
	public void disconnectClient(int userNum) {
		if(user[userNum] != null) {
			user[userNum].setStatus(false);
			user[userNum] = null;
			probePacket[userNum] = null;
			probePacketThread[userNum] = null;
			numClients--;
			objectManager.onLeaderboard[userNum] = false;
			//try closing the socket
			try {
				if(clientSocket[userNum] != null) {
					clientSocket[userNum].close();
					clientSocket[userNum] = null;
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
			System.out.printf("User[%d] has disconnected\n", userNum);
		}
	}
	
	public void enterCommand(String command) {
		//specify which user to execute the command on
		int userID;
		switch(command) {
		case "/close server":
			//close the server and disconnect all current clients
			try {
				serverCommand.closeServer(user, clientSocket);
				serverRunning = false;
				System.out.println("Server successfully closed.");
				System.out.println("All users disconnected.");
				System.exit(1);
			} catch(IOException e) {
				e.printStackTrace();
				System.err.println("Unable to close server");
			}
			break;
		case "/kick user":
			//disconnect the specified user from the current session
			System.out.print("UserID: ");
			userID = scanner.nextInt();
			try {
				if(user[userID] != null) {
					user[userID].getDataOutputStream().writeUTF("[kicked]");
					user[userID].setStatus(false);
					user[0] = null;
					probePacket[0] = null;
					probePacketThread[0] = null;
					clientSocket[0].close();
					clientSocket[0] = null;
					System.out.println("User[" + userID + "] has been kicked from the server");
					for(int i = 0; i < MAX_CLIENTS; i++) {
						if(user[i] != null) {
							try {
								user[i].getDataOutputStream().writeUTF("<Message from Server> User[" + userID + "] has been kicked from the server");
							} catch (IOException e) {
								disconnectClient(i);
							}
						}
					}
				} else System.out.println("User[" + userID + "] not found");
			} catch(IOException e) {
				e.printStackTrace();
			}
			break;
		case "/temp ban user":
			//temporarily restrict the user's IP for the current session
			System.out.print("UserID: ");
			userID = scanner.nextInt();
			try {
				if(user[userID] != null) {
					user[userID].getDataOutputStream().writeUTF("[banned]");
					serverFileManager.tempRestrictIP(user[userID].getIPAddress().toString());
					user[userID].setStatus(false);
					user[userID] = null;
					probePacket[userID] = null;
					probePacketThread[userID] = null;
					clientSocket[userID].close();
					clientSocket[userID] = null;
					System.out.println("User[" + userID + "] has been temporarily banned");
					for(int i = 0; i < MAX_CLIENTS; i++) {
						if(user[i] != null) {
							try {
								user[i].getDataOutputStream().writeUTF("<Message from Server> User[" + userID + "] has been temporarily banned from the server");
							} catch (IOException e) {
								disconnectClient(i);
							}
						}
					}
				} else System.out.println("User[" + userID + "] not found");
			} catch(IOException e) {
				e.printStackTrace();
			}
			break;
		case "/ban user":
			//permanently ban the user's IP from the server address
			System.out.print("UserID: ");
			userID = scanner.nextInt();
			try {
				if(user[userID] != null) {
					user[userID].getDataOutputStream().writeUTF("[banned]");
					serverFileManager.restrictIP(user[userID].getIPAddress().toString());
					user[userID].setStatus(false);
					user[userID] = null;
					probePacket[userID] = null;
					probePacketThread[userID] = null;
					clientSocket[userID].close();
					clientSocket[userID] = null;
					System.out.println("User[" + userID + "] has been permanently banned");
					for(int i = 0; i < MAX_CLIENTS; i++) {
						if(user[i] != null) {
							try {
								user[i].getDataOutputStream().writeUTF("<Message from Server> User[" + userID + "] has been banned from the server");
							} catch (IOException e) {
								disconnectClient(i);
							}
						}
					}
				} else System.out.println("User[" + userID + "] not found");
			} catch(IOException e) {
				e.printStackTrace();
			}
			break;
		case "/notify user":
			//send a direct message to the specified user
			System.out.print("UserID: ");
			userID = scanner.nextInt();
			scanner.nextLine();
			System.out.print("Private Message to " + user[userID].getUsername() + ": ");
			String directMessage = scanner.nextLine();
			try {
				if(user[userID] != null) {
					user[userID].getDataOutputStream().writeUTF("<Message from Server> " + directMessage);
				} else {
					System.out.println("User[" + userID + "] not found");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case "/commands":
			//display a list of all server commands
			System.out.println("\n                     SERVER COMMANDS");
			System.out.println("\"/close server\": Close the server");
			System.out.println("\"/kick user\": Kick the specified user from the server");
			System.out.println("\"/temp ban user\": Temporarily ban the user for the current session");
			System.out.println("\"/ban user\": Permanently ban the specifed user");
			System.out.println("\"/notify user\": Send a private message to the specified user");
			System.out.println("\"/num clients\": Return the number of connected clients");
			System.out.println("\"/sockets\": Return a list of all current client socket connections");
			System.out.println("\"/threads\": Return the number of active running server-side threads");
			System.out.println("\"/show user IP\": Hide client IPs on the server console");
			System.out.println("\"/hide user IP\": Hide client IPs on the server console");
			System.out.println("\"/user run status\": Return the thread running status of the specified user");
			System.out.println("\"/commands\": Show this list\n");
			break;
		case "/num clients":
			//display the number of current clients
			System.out.println("Current number of connected clients: " + numClients + "/" + MAX_CLIENTS);
			break;
		case "/sockets":
			//display a list of all client-side sockets
			for(int i = 0; i < clientSocket.length; i++) {
				if(clientSocket[i] != null) System.out.println(clientSocket[i]);
			}
			break;
		case "/threads":
			//display the number of all active server-side threads
			System.out.println("Active threads: " + java.lang.Thread.activeCount() + "/" + ((MAX_CLIENTS*2)+3));
			break;
		case "/users":
			//display a list of a string representation of all user objects
			serverCommand.getUsers(user, clientSocket);
			break;
		case "/show user IP":
			//allow client IPs to be shown on the server console
			hideIP = false;
			break;
		case "/hide user IP":
			//hide client IPs from being shown on the server console
			hideIP = true;
			break;
		case "/user run status":
			//check if the user's thread is running (Used for debugging purposes)
			System.out.print("UserID: ");
			userID = scanner.nextInt();
			scanner.nextLine();
			if(user[userID] != null) {
				System.out.println("User[" + userID + "]: " + user[userID].getStatus());
			} else {
				System.out.println("User[" + userID + "] not found");
			}
			break;
		default:
			//server has entered an invalid command
			System.err.println("Invalid command");
			System.err.printf("Error: command \"%s\" not found\n", command);
		}
	}

}
