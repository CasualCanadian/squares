package com.server;

import java.io.IOException;
import java.net.Socket;

/*
 * Command methods for the server
 */

public class ServerCommand {
	
	private Server server;
	
	public ServerCommand(Server server) {
		this.server = server;
	}
	
	//close the server, notify all clients and close all the sockets
	public void closeServer(User[] user, Socket[] clientSocket) throws IOException {
		System.out.println("Server closing...");
		for(int i = 0; i < Server.MAX_CLIENTS; i++) {
			if(user[i] != null) {
				user[i].getDataOutputStream().writeUTF("<Message from Server> Server closing...");
				clientSocket[i].close();
			}
		}
	}
	
	//return list of all current users and info
	public void getUsers(User[] user, Socket[] socket) {
		int numUsers = 0;
		
		//if server allows for showing IPs
		if(server.getIPStatus()) {
			for(int i = 0; i < Server.MAX_CLIENTS; i++) {
				if(user[i] != null) {
					System.out.printf("User[%d]: Username: " + user[i].getUsername() + "\n", i);
					numUsers++;
				}
			}
		//if server is hiding IPs
		} else if(!server.getIPStatus()) {
			for(int i = 0; i < Server.MAX_CLIENTS; i++) {
				if(user[i] != null) {
					System.out.printf("User[%d]: Username: " + user[i].getUsername() + " / IP: " + user[i].getIPAddress() + " / Socket: " + socket[i] + "\n", i);
					numUsers++;
				}
			}
		}
		
		//if no users have been reached notify the server
		if(numUsers == 0) {
			System.out.println("No current users");
		}
	}
	
}
