package com.server;

import java.io.DataOutputStream;
import java.io.IOException;

/*
 * Dedicated thread to send packets (one per user) to check if the client-server connection is still valid
 * If no response, then the client has disconnected and the server will close the socket connection and other client-specific related objects
 */

public class ProbePacket implements Runnable {

	private Server server;
	private boolean running;
	
	//client output stream to send out data
	private DataOutputStream outputStream;
	
	private int userID;
	
	public ProbePacket(Server server, DataOutputStream out, int userID) {
		this.server = server;
		this.outputStream = out;
		this.userID = userID;
		running = true;
	}

	@Override
	public void run() {
		while(running) {
			//wait 3 seconds
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//send a specific arrangement of numbers as a string and wait for a client response
			//if the message cannot be sent, then close the socket and stop this thread
			try {
				outputStream.writeUTF("0165472398476");
			} catch(IOException e) {
				server.disconnectClient(userID);
				running = false;
			}
		}
	}
	
}
