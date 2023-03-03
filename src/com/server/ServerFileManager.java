package com.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/*
 * Manages all of the server files
 */

public class ServerFileManager {

	//text file with banned IPs
	private File restrictedServerIPList;
	//text file with temporarily banned IPs
	private File tempRestrictedServerIPList;
	//text file with entire chat history
	private File chatHistory;
	//text file with random bot names to use
	private File botNames;
	
	//random variable
	private Random r = new Random();
	
	public ServerFileManager() throws IOException {
		restrictedServerIPList = new File("RestrictedServerIPList.txt");
		tempRestrictedServerIPList = new File("TempRestrictedServerIPList.txt");
		chatHistory = new File("ChatHistory.txt");
		botNames = new File("AINamesList.txt");
		
		//refresh temporary restricted IPs file at the beginning of each session
		if(tempRestrictedServerIPList.exists()) {
			tempRestrictedServerIPList.delete();
		}
		
		try {
			tempRestrictedServerIPList.createNewFile();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//write the user's IP to be banned in the file
	public void restrictIP(String ip) {
		FileWriter fw;
		try {
			fw = new FileWriter(restrictedServerIPList, true);
			PrintWriter pw = new PrintWriter(fw);
			
			pw.println(ip);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//write the user's IP to be temporarily banned in the file
	public void tempRestrictIP(String ip) {
		FileWriter fw;
		try {
			fw = new FileWriter(tempRestrictedServerIPList, true);
			PrintWriter pw = new PrintWriter(fw);
			
			pw.println(ip);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//loop through all banned IPs in each file and check if the user's IP has been restricted
	@SuppressWarnings("resource")
	public boolean isRestrictedIP(String ip) throws IOException {
		FileReader fr = new FileReader(restrictedServerIPList);
		BufferedReader br = new BufferedReader(fr);
		FileReader fr1 = new FileReader(tempRestrictedServerIPList);
		BufferedReader br1 = new BufferedReader(fr1);
		
		//check permanent ban file
		String restrictedIP = br.readLine();
		while(restrictedIP != null) {
			if(restrictedIP.equals(ip)) return true;
			else restrictedIP = br.readLine();
		}
		
		//check temporary ban file
		restrictedIP = br1.readLine();
		while(restrictedIP != null) {
			if(restrictedIP.equals(ip)) return true;
			else restrictedIP = br1.readLine();
		}
		return false;
	}
	
	/*
	 * Write a line to the chat history file
	 * Used for keeping track of chat history and referencing old messages
	 */
	public void writeLine(String message) {
		try {
			FileWriter fw = new FileWriter(chatHistory, true);
			PrintWriter pw = new PrintWriter(fw);
			
			pw.println(message);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//return a random name from the botNames text file
	@SuppressWarnings("resource")
	public String generateName() {
		int index = r.nextInt(51)+1;
		
		FileReader fr;
		BufferedReader br;
		try {
			fr = new FileReader(botNames);
			br = new BufferedReader(fr);
			
			String tempName = "";
			for(int i = 0; i < index; i++) {
				try {
					tempName = br.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return tempName;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return "Bot";
	}
	
}
