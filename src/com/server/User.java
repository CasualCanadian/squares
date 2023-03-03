package com.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

/*
 * Creates a new dedicated thread for handling server-client communications
 */

public class User implements Runnable{
	
	@SuppressWarnings("unused")
	private InetAddress ip;
	//unique ID that the server can use to reference this specific user
	private final int USER_ID;
	
	//input and output streams for sending and receiving data
	private DataInputStream in;
	private DataOutputStream out;
	
	//array of other users
	private User[] user = new User[Server.MAX_CLIENTS];
	private ServerFileManager fileManager;
	private ObjectManager om;
	
	private boolean running;
	//true if the user is playing
	private boolean playing;
	
	private String username;
	private String color;
	
	public User(DataInputStream in, DataOutputStream out, User[] user, InetAddress ip, int id, ServerFileManager fileManager, ObjectManager om) {
		this.in = in;
		this.out = out;
		this.user = user;
		this.ip = ip;
		USER_ID = id;
		this.fileManager = fileManager;
		this.om = om;
		
		//reset leaderboard status of the new user
		om.onLeaderboard[USER_ID] = false;
	}

	@Override
	public void run() {
		running = true;
		try {
			//initialize a user name to be seen by on the server by other players
			username = in.readUTF();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		while(running) {
			try {
				//wait for an incoming message from the client
				String message = in.readUTF();
				//write the message in the server's chat history file
				fileManager.writeLine(message);
				
				//packet that will send out all data of each player to the client to be rendered
				String packet = "[data]";
				for(int c = 0; c < Server.MAX_CLIENTS; c++) {
					if(user[c] != null && c != USER_ID) {
						if(user[c].isPlaying()) packet += "[" + om.data[c][0] + "/" + om.data[c][1] + "/" + user[c].getUsername() + "/" + om.playerColors[c] + "/" + om.eliminations[c] + "]";
						else packet += "[null]";
					} else if(user[c] == null && c != USER_ID) {
						packet += "[null]";
					}
				}
				
				//packet that will send out all data of each bot to the client to be rendered
				String packet2 = "[data2]";
				for(int i = 0; i < Server.MAX_BOTS; i++) {
					if(om.getSimpleBot(i) != null) {
						try {
							packet2 += "[" + om.getSimpleBot(i).getX() + "/" + om.getSimpleBot(i).getY() + "/" + om.getSimpleBot(i).getUsername() + "/" + om.getSimpleBot(i).getColor() + "/" + om.getSimpleBot(i).getElims() + "]";
						} catch(NullPointerException e) {
							packet2 += "[null]";
						}
					} else if(om.getSimpleBot(i) == null) {
						packet2 += "[null]";
					}
				}
				
				//packet that will send out data of top 10 players/bots
				String leaderboardPacket = "[score]";
				for(int i = 0; i < 10; i++) {
					leaderboardPacket += "[" + om.topUsers[i].getUsername() + "/" + om.topUsers[i].getElims() + "/" + om.topUsers[i].getColor() + "]";
				}
				
				//send out all of the packets to the client
				out.writeUTF(packet);
				out.writeUTF(packet2);
				out.writeUTF(leaderboardPacket);
				//send out the hardbot data
				out.writeUTF("[hardbot]" + "[" + om.pos[0][0] + "/" + om.pos[0][1] + "]" + "[" + om.pos[1][0] + "/" + om.pos[1][1] + "]");
				
				//write all messages to all other current clients connected to the server
				for(int i = 0; i < Server.MAX_CLIENTS; i++) {
					if(user[i] != null && !message.equals(null)) {
						//if the message is a standard text message to be sent to other clients
						if(USER_ID != i && !message.startsWith("[data]") && !message.startsWith("[play]") && !message.startsWith("[!play]")) {
							if(color != null) user[i].out.writeUTF("[" + color + "]" + "<" + username + "> " + message);
							else user[i].out.writeUTF("[WHITE]" + "<" + username + "> " + message);
						}
						//string contains the client's x and y coordinates
						if(message.startsWith("[data]")) decodeString(message);
						//the client has pressed the play button
						if(message.startsWith("[play]")) playing = true;
						//the client is no longer in the game
						//reset all client-related variables and arrays
						if(message.startsWith("[!play]")) {
							playing = false;
							om.data[USER_ID][0] = 0f;
							om.data[USER_ID][1] = 0f;
							om.playerColors[USER_ID] = null;
							if(om.onLeaderboard[USER_ID] == true) om.removeTopUser(om.placeOnLeaderboard[USER_ID]);
							om.onLeaderboard[USER_ID] = false;
							om.eliminations[USER_ID] = 0;
							color = null;
							out.flush();
						}
					}
				}
			} catch(IOException e) {
			}
		}
	}
	
	//decode the incoming string and store the data in an array
	private void decodeString(String str) {
		int indexOfSlash = str.indexOf(47);
		
		//separate the different data types from the string
		om.data[USER_ID][0] = Float.parseFloat(str.substring(6, indexOfSlash));
		om.data[USER_ID][1] = Float.parseFloat(str.substring(indexOfSlash+1, str.indexOf(47, indexOfSlash+1)));
		indexOfSlash = str.indexOf(47, indexOfSlash+1);
		
		switch(str.substring(indexOfSlash+1, str.length())) {
		case "java.awt.Color[r=255,g=0,b=0]":
			om.playerColors[USER_ID] = "RED";
			color = "RED";
			break;
		case "java.awt.Color[r=0,g=255,b=255]":
			om.playerColors[USER_ID] = "CYAN";
			color = "CYAN";
			break;
		case "java.awt.Color[r=0,g=255,b=0]":
			om.playerColors[USER_ID] = "GREEN";
			color = "GREEN";
			break;
		}
	}
	
	public DataOutputStream getDataOutputStream() {
		return out;
	}
	
	public int getUserID() {
		return USER_ID;
	}
	
	public InetAddress getIPAddress() {
		return ip;
	}
	
	public boolean getStatus() {
		return running;
	}
	
	public void setStatus(boolean running) {
		this.running = running;
	}
	
	public String getUsername() {
		return username;
	}
	
	public boolean isPlaying() {
		return playing;
	}
	
	public String getColor() {
		return color;
	}
	
	//set this user's playing status to false and reset variables and arrays
	public void setPlaying(boolean status) {
		playing = status;
		om.data[USER_ID][0] = 0f;
		om.data[USER_ID][1] = 0f;
		om.playerColors[USER_ID] = null;
		color = null;
	}
	
}
