package com.server;

/*
 * A temporary object of the top 10 players/bots in the server
 * Will store the player/bot's data to display on the client-side leaderboard
 */

public class TopUsers {

	private String username;
	private int eliminations;
	private String color;
	private boolean isPlayer;
	private int id;
	
	public TopUsers(String name, int elims, String color, boolean isPlayer, int id) {
		username = name;
		eliminations = elims;
		this.color = color;
		this.isPlayer = isPlayer;
		this.id = id;
	}
	
	public String getUsername() {
		return username;
	}
	
	public int getElims() {
		return eliminations;
	}
	
	public String getColor() {
		return color;
	}
	
	public boolean isPlayer() {
		return isPlayer;
	}
	
	public int getId() {
		return id;
	}
}
