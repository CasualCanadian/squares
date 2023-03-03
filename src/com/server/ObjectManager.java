package com.server;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Random;

/*
 * Update and manage all game objects, update positions of all game objects to be sent and rendered by each client
 */

public class ObjectManager implements Runnable {
	
	//co-ordinates of both hardbots
	public float[][] pos = new float[2][2];
	//arrays of data that each player will have
	//co-ordinates
	public float[][] data = new float[Server.MAX_CLIENTS][2];
	//colour
	public String[] playerColors = new String[Server.MAX_CLIENTS];
	//number of eliminations
	public int[] eliminations = new int[Server.MAX_CLIENTS];
	public boolean[] onLeaderboard = new boolean[Server.MAX_CLIENTS];
	public int[] placeOnLeaderboard = new int[Server.MAX_CLIENTS];
	
	//array of top users from the leaderboard
	public TopUsers[] topUsers = new TopUsers[10];
	
	//array of simplebots
	private SimpleBot[] simpleBot = new SimpleBot[Server.MAX_BOTS];
	//array of the 3 main colours to initialize a simplebot with
	private Color[] colors = {Color.RED, Color.GREEN, Color.CYAN};
	
	private Server server;
	private ServerFileManager serverFileManager;
	private HardBot hardBot1;
	private HardBot hardBot2;
	
	//random variable
	private Random r = new Random();
	
	public ObjectManager(Server server, ServerFileManager serverFileManager) {
		this.server = server;
		this.serverFileManager = serverFileManager;
		
		//initialize both hardbots with random co-ordinates
		hardBot1 = new HardBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.HardBot, this, server);
		hardBot2 = new HardBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.HardBot, this, server);
		
		//initialize all simplebots with random co-ordinates and colour
		for(int i = 0; i < Server.MAX_BOTS; i++) {
			simpleBot[i] = new SimpleBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.SimpleBot, colors[r.nextInt(3)], server, this, i, serverFileManager.generateName());
		}
		
		//intialize the first 10 top users
		for(int i = 0; i < 10; i++) {
			topUsers[i] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
			simpleBot[i].setOnLeaderboard(true);
			simpleBot[i].setPlaceOnLeaderboard(i);
		}
		
		//create the collision-checking and leaderboard updating object
		new Collision(server, this);
	}

	/*
	 * Game loop
	 * Explained in more detail in com/client/GameClient(170)
	 */
	@Override
	public void run() {
		while(server.isRunning()) {
			long lastTime = System.nanoTime();
			double amountOfTicks = 60.0;
			double ns = 1000000000 / amountOfTicks;
			double delta = 0;
			long timer = System.currentTimeMillis();
			while(server.isRunning()) {
				long now = System.nanoTime();
				delta += (now - lastTime) / ns;
				lastTime = now;
				while(delta >= 1) {
					tick();
					delta--;
				}
				if(server.isRunning())
				if(System.currentTimeMillis() - timer > 1000) {
					timer += 1000;
				}
			}
		}
	}
	
	//update and tick all game objects
	public void tick() {
		//loop through all linked list e
		//update all simplebots
		for(int i = 0; i < Server.MAX_BOTS; i++) {
			if(simpleBot[i] != null) simpleBot[i].tick();
		}
		
		//update hardbots
		hardBot1.tick();
		hardBot2.tick();
		
		//update hardbot co-ordinate arrays
		pos[0][0] = hardBot1.getX();
		pos[0][1] = hardBot1.getY();
		
		pos[1][0] = hardBot2.getX();
		pos[1][1] = hardBot2.getY();
	}
	
	/*
	 * Refresh the leaderboard to clear any "dead" players/bots
	 */
	public void refreshLeaderboard() {
		//reset the leaderboard state of all players/bots currently on the leaderboard
		for(int i = 0; i < 10; i++) {
			if(topUsers[i].isPlayer()) onLeaderboard[topUsers[i].getId()] = false;
			else if(!topUsers[i].isPlayer()) simpleBot[topUsers[i].getId()].setOnLeaderboard(false);
		}
		
		//intialize the first 10 top users
		for(int i = 0; i < 10; i++) {
			topUsers[i] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
			simpleBot[i].setOnLeaderboard(true);
			simpleBot[i].setPlaceOnLeaderboard(i);
		}
	}
	
	/*
	 * Update the leaderboard
	 */
	public void updateLeaderboard() {
		//true if the player/bot is already on the leaderboard
		boolean alreadyOnLeaderboard;
		
		//update all players on the leaderboard
		for(int i = 0; i < Server.MAX_CLIENTS; i++) {
			if(server.getUser(i) != null) {
				if(onLeaderboard[i]) alreadyOnLeaderboard = true;
				else alreadyOnLeaderboard = false;
				
				//if the player surpasses the number of the top user's eliminations
				if(eliminations[i] > topUsers[0].getElims()) {
					//if the player is already on the leaderboard in a position lower than 1st but surpasses the top number of eliminations, 
					//grants access to the player to replace the top user
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 0) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					//if the player is not already in 1st place and is not yet on the leaderboard
					if(!(topUsers[0].isPlayer() && topUsers[0].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						//remove the last player from the leaderboard
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						//move all of the players down by one place
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = topUsers[3];
						topUsers[3] = topUsers[2];
						topUsers[2] = topUsers[1];
						topUsers[1] = topUsers[0];
						//initialize the player as a new top user
						topUsers[0] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 0;
						alreadyOnLeaderboard = true;
						//if the player is already in 1st place, update the top user
					} else if(topUsers[0].isPlayer() && topUsers[0].getId() == server.getUser(i).getUserID()) {
						topUsers[0] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[1].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 1) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[1].isPlayer() && topUsers[1].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = topUsers[3];
						topUsers[3] = topUsers[2];
						topUsers[2] = topUsers[1];
						topUsers[1] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 1;
						alreadyOnLeaderboard = true;
					} else if(topUsers[1].isPlayer() && topUsers[1].getId() == server.getUser(i).getUserID()) {
						topUsers[1] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[2].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 2) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[2].isPlayer() && topUsers[2].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = topUsers[3];
						topUsers[3] = topUsers[2];
						topUsers[2] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 2;
						alreadyOnLeaderboard = true;
					} else if(topUsers[2].isPlayer() && topUsers[2].getId() == server.getUser(i).getUserID()) {
						topUsers[2] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[3].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 3) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[3].isPlayer() && topUsers[3].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = topUsers[3];
						topUsers[3] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 3;
						alreadyOnLeaderboard = true;
					} else if(topUsers[3].isPlayer() && topUsers[3].getId() == server.getUser(i).getUserID()) {
						topUsers[3] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[4].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 4) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[4].isPlayer() && topUsers[4].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 4;
						alreadyOnLeaderboard = true;
					} else if(topUsers[4].isPlayer() && topUsers[4].getId() == server.getUser(i).getUserID()) {
						topUsers[4] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[5].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 5) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[5].isPlayer() && topUsers[5].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 5;
						alreadyOnLeaderboard = true;
					} else if(topUsers[5].isPlayer() && topUsers[5].getId() == server.getUser(i).getUserID()) {
						topUsers[5] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[6].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 6) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[6].isPlayer() && topUsers[6].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 6;
						alreadyOnLeaderboard = true;
					} else if(topUsers[6].isPlayer() && topUsers[6].getId() == server.getUser(i).getUserID()) {
						topUsers[6] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[7].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 7) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[7].isPlayer() && topUsers[7].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 7;
						alreadyOnLeaderboard = true;
					} else if(topUsers[7].isPlayer() && topUsers[7].getId() == server.getUser(i).getUserID()) {
						topUsers[7] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[8].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 8) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[8].isPlayer() && topUsers[8].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 8;
						alreadyOnLeaderboard = true;
					} else if(topUsers[8].isPlayer() && topUsers[8].getId() == server.getUser(i).getUserID()) {
						topUsers[8] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				} else if(eliminations[i] > topUsers[9].getElims()) {
					if(onLeaderboard[i] && placeOnLeaderboard[i] > 9) {
						removeTopUser(placeOnLeaderboard[i]);
						alreadyOnLeaderboard = false;
					}
					if(!(topUsers[9].isPlayer() && topUsers[9].getId() == server.getUser(i).getUserID()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						onLeaderboard[i] = true;
						placeOnLeaderboard[i] = 9;
						alreadyOnLeaderboard = true;
					} else if(topUsers[9].isPlayer() && topUsers[9].getId() == server.getUser(i).getUserID()) {
						topUsers[9] = new TopUsers(server.getUser(i).getUsername(), eliminations[i], playerColors[i], true, server.getUser(i).getUserID());
						alreadyOnLeaderboard = true;
					}
				}
			}
		}
		
		//reset the place on the leaderboard of all players/bots currently on the leaderboard
				if(topUsers[9].isPlayer()) placeOnLeaderboard[topUsers[9].getId()] = 9;
				else if(!topUsers[9].isPlayer()) simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
				if(topUsers[8].isPlayer()) placeOnLeaderboard[topUsers[8].getId()] = 8;
				else if(!topUsers[8].isPlayer()) simpleBot[topUsers[8].getId()].setPlaceOnLeaderboard(8);
				if(topUsers[7].isPlayer()) placeOnLeaderboard[topUsers[7].getId()] = 7;
				else if(!topUsers[7].isPlayer()) simpleBot[topUsers[7].getId()].setPlaceOnLeaderboard(7);
				if(topUsers[6].isPlayer()) placeOnLeaderboard[topUsers[6].getId()] = 6;
				else if(!topUsers[6].isPlayer()) simpleBot[topUsers[6].getId()].setPlaceOnLeaderboard(6);
				if(topUsers[5].isPlayer()) placeOnLeaderboard[topUsers[5].getId()] = 5;
				else if(!topUsers[5].isPlayer()) simpleBot[topUsers[5].getId()].setPlaceOnLeaderboard(5);
				if(topUsers[4].isPlayer()) placeOnLeaderboard[topUsers[4].getId()] = 4;
				else if(!topUsers[4].isPlayer()) simpleBot[topUsers[4].getId()].setPlaceOnLeaderboard(4);
				if(topUsers[3].isPlayer()) placeOnLeaderboard[topUsers[3].getId()] = 3;
				else if(!topUsers[3].isPlayer()) simpleBot[topUsers[3].getId()].setPlaceOnLeaderboard(3);
				if(topUsers[2].isPlayer()) placeOnLeaderboard[topUsers[2].getId()] = 2;
				else if(!topUsers[2].isPlayer()) simpleBot[topUsers[2].getId()].setPlaceOnLeaderboard(2);
				if(topUsers[1].isPlayer()) placeOnLeaderboard[topUsers[1].getId()] = 1;
				else if(!topUsers[1].isPlayer()) simpleBot[topUsers[1].getId()].setPlaceOnLeaderboard(1);
				if(topUsers[0].isPlayer()) placeOnLeaderboard[topUsers[0].getId()] = 0;
				else if(!topUsers[0].isPlayer()) simpleBot[topUsers[0].getId()].setPlaceOnLeaderboard(0);
		
		//update all bots on the leaderboard
		for(int i = 0; i < Server.MAX_BOTS; i++) {
			if(simpleBot[i] != null) {
				if(simpleBot[i].isOnLeaderboard()) alreadyOnLeaderboard = true;
				else alreadyOnLeaderboard = false;
				
				if(simpleBot[i].getElims() > topUsers[0].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 0) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[0].isPlayer() && topUsers[0].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = topUsers[3];
						topUsers[3] = topUsers[2];
						topUsers[2] = topUsers[1];
						topUsers[1] = topUsers[0];
						topUsers[0] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(0);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[0].isPlayer() && topUsers[0].getId() == simpleBot[i].getId())) {
						topUsers[0] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[1].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 1) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[1].isPlayer() && topUsers[1].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = topUsers[3];
						topUsers[3] = topUsers[2];
						topUsers[2] = topUsers[1];
						topUsers[1] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(1);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[1].isPlayer() && topUsers[1].getId() == simpleBot[i].getId())) {
						topUsers[1] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[2].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 2) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[2].isPlayer() && topUsers[2].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = topUsers[3];
						topUsers[3] = topUsers[2];
						topUsers[2] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(2);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[2].isPlayer() && topUsers[2].getId() == simpleBot[i].getId())) {
						topUsers[2] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[3].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 3) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[3].isPlayer() && topUsers[3].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = topUsers[3];
						topUsers[3] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(3);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[3].isPlayer() && topUsers[3].getId() == simpleBot[i].getId())) {
						topUsers[3] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[4].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 4) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[4].isPlayer() && topUsers[4].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = topUsers[4];
						topUsers[4] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(4);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[4].isPlayer() && topUsers[4].getId() == simpleBot[i].getId())) {
						topUsers[4] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[5].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 5) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[5].isPlayer() && topUsers[5].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = topUsers[5];
						topUsers[5] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(5);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[5].isPlayer() && topUsers[5].getId() == simpleBot[i].getId())) {
						topUsers[5] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[6].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 6) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[6].isPlayer() && topUsers[6].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = topUsers[6];
						topUsers[6] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(6);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[6].isPlayer() && topUsers[6].getId() == simpleBot[i].getId())) {
						topUsers[6] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[7].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 7) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[7].isPlayer() && topUsers[7].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = topUsers[7];
						topUsers[7] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(7);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[7].isPlayer() && topUsers[7].getId() == simpleBot[i].getId())) {
						topUsers[7] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[8].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 8) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[8].isPlayer() && topUsers[8].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = topUsers[8];
						topUsers[8] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(8);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[8].isPlayer() && topUsers[8].getId() == simpleBot[i].getId())) {
						topUsers[8] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				} else if(simpleBot[i].getElims() > topUsers[9].getElims()) {
					if(simpleBot[i].isOnLeaderboard() && simpleBot[i].getPlaceOnLeaderboard() > 9) {
						removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
						alreadyOnLeaderboard = false;
					}
					if((!topUsers[9].isPlayer() && topUsers[9].getId() != simpleBot[i].getId()) && !alreadyOnLeaderboard) {
						if(topUsers[9].isPlayer()) {
							onLeaderboard[topUsers[9].getId()] = false;
							placeOnLeaderboard[topUsers[9].getId()] = 9;
						} else if(!topUsers[9].isPlayer()) {
							simpleBot[topUsers[9].getId()].setOnLeaderboard(false);
							simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
						}
						
						topUsers[9] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						simpleBot[i].setOnLeaderboard(true);
						simpleBot[i].setPlaceOnLeaderboard(9);
						alreadyOnLeaderboard = true;
					} else if((!topUsers[9].isPlayer() && topUsers[9].getId() == simpleBot[i].getId())) {
						topUsers[9] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
						alreadyOnLeaderboard = true;
					}
				}
			}
		}
		
		//reset the place on the leaderboard of all players/bots currently on the leaderboard
		if(topUsers[9].isPlayer()) placeOnLeaderboard[topUsers[9].getId()] = 9;
		else if(!topUsers[9].isPlayer()) simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
		if(topUsers[8].isPlayer()) placeOnLeaderboard[topUsers[8].getId()] = 8;
		else if(!topUsers[8].isPlayer()) simpleBot[topUsers[8].getId()].setPlaceOnLeaderboard(8);
		if(topUsers[7].isPlayer()) placeOnLeaderboard[topUsers[7].getId()] = 7;
		else if(!topUsers[7].isPlayer()) simpleBot[topUsers[7].getId()].setPlaceOnLeaderboard(7);
		if(topUsers[6].isPlayer()) placeOnLeaderboard[topUsers[6].getId()] = 6;
		else if(!topUsers[6].isPlayer()) simpleBot[topUsers[6].getId()].setPlaceOnLeaderboard(6);
		if(topUsers[5].isPlayer()) placeOnLeaderboard[topUsers[5].getId()] = 5;
		else if(!topUsers[5].isPlayer()) simpleBot[topUsers[5].getId()].setPlaceOnLeaderboard(5);
		if(topUsers[4].isPlayer()) placeOnLeaderboard[topUsers[4].getId()] = 4;
		else if(!topUsers[4].isPlayer()) simpleBot[topUsers[4].getId()].setPlaceOnLeaderboard(4);
		if(topUsers[3].isPlayer()) placeOnLeaderboard[topUsers[3].getId()] = 3;
		else if(!topUsers[3].isPlayer()) simpleBot[topUsers[3].getId()].setPlaceOnLeaderboard(3);
		if(topUsers[2].isPlayer()) placeOnLeaderboard[topUsers[2].getId()] = 2;
		else if(!topUsers[2].isPlayer()) simpleBot[topUsers[2].getId()].setPlaceOnLeaderboard(2);
		if(topUsers[1].isPlayer()) placeOnLeaderboard[topUsers[1].getId()] = 1;
		else if(!topUsers[1].isPlayer()) simpleBot[topUsers[1].getId()].setPlaceOnLeaderboard(1);
		if(topUsers[0].isPlayer()) placeOnLeaderboard[topUsers[0].getId()] = 0;
		else if(!topUsers[0].isPlayer()) simpleBot[topUsers[0].getId()].setPlaceOnLeaderboard(0);
	}
	
	/*
	 * Remove a player/object from the leaderboard
	 */
	public void removeTopUser(int index) {
		//move up all top users by one place
		switch(index) {
		case 0:
			topUsers[0] = topUsers[1];
			topUsers[1] = topUsers[2];
			topUsers[2] = topUsers[3];
			topUsers[3] = topUsers[4];
			topUsers[4] = topUsers[5];
			topUsers[5] = topUsers[6];
			topUsers[6] = topUsers[7];
			topUsers[7] = topUsers[8];
			topUsers[8] = topUsers[9];
			break;
		case 1:
			topUsers[1] = topUsers[2];
			topUsers[2] = topUsers[3];
			topUsers[3] = topUsers[4];
			topUsers[4] = topUsers[5];
			topUsers[5] = topUsers[6];
			topUsers[6] = topUsers[7];
			topUsers[7] = topUsers[8];
			topUsers[8] = topUsers[9];
			break;
		case 2:
			topUsers[2] = topUsers[3];
			topUsers[3] = topUsers[4];
			topUsers[4] = topUsers[5];
			topUsers[5] = topUsers[6];
			topUsers[6] = topUsers[7];
			topUsers[7] = topUsers[8];
			topUsers[8] = topUsers[9];
			break;
		case 3:
			topUsers[3] = topUsers[4];
			topUsers[4] = topUsers[5];
			topUsers[5] = topUsers[6];
			topUsers[6] = topUsers[7];
			topUsers[7] = topUsers[8];
			topUsers[8] = topUsers[9];
			break;
		case 4:
			topUsers[4] = topUsers[5];
			topUsers[5] = topUsers[6];
			topUsers[6] = topUsers[7];
			topUsers[7] = topUsers[8];
			topUsers[8] = topUsers[9];
			break;
		case 5:
			topUsers[5] = topUsers[6];
			topUsers[6] = topUsers[7];
			topUsers[7] = topUsers[8];
			topUsers[8] = topUsers[9];
			break;
		case 6:
			topUsers[6] = topUsers[7];
			topUsers[7] = topUsers[8];
			topUsers[8] = topUsers[9];
			break;
		case 7:
			topUsers[7] = topUsers[8];
			topUsers[8] = topUsers[9];
			break;
		case 9:
			topUsers[8] = topUsers[9];
			break;
		default:
			return;
		}
		
		//replace the 10th leaderboard place with a random bot
		for(int i = 0; i < Server.MAX_BOTS; i++) {
			if(simpleBot[i] != null) {
				if(!simpleBot[i].isOnLeaderboard() ) {
					topUsers[9] = new TopUsers(simpleBot[i].getUsername(), simpleBot[i].getElims(), simpleBot[i].getColor(), false, simpleBot[i].getId());
					simpleBot[i].setOnLeaderboard(true);
					simpleBot[i].setPlaceOnLeaderboard(9);
					break;
				}
			}
		}
		
		//reset the place on the leaderboard of all players/bots currently on the leaderboard
		if(topUsers[9].isPlayer()) placeOnLeaderboard[topUsers[9].getId()] = 9;
		else if(!topUsers[9].isPlayer()) simpleBot[topUsers[9].getId()].setPlaceOnLeaderboard(9);
		if(topUsers[8].isPlayer()) placeOnLeaderboard[topUsers[8].getId()] = 8;
		else if(!topUsers[8].isPlayer()) simpleBot[topUsers[8].getId()].setPlaceOnLeaderboard(8);
		if(topUsers[7].isPlayer()) placeOnLeaderboard[topUsers[7].getId()] = 7;
		else if(!topUsers[7].isPlayer()) simpleBot[topUsers[7].getId()].setPlaceOnLeaderboard(7);
		if(topUsers[6].isPlayer()) placeOnLeaderboard[topUsers[6].getId()] = 6;
		else if(!topUsers[6].isPlayer()) simpleBot[topUsers[6].getId()].setPlaceOnLeaderboard(6);
		if(topUsers[5].isPlayer()) placeOnLeaderboard[topUsers[5].getId()] = 5;
		else if(!topUsers[5].isPlayer()) simpleBot[topUsers[5].getId()].setPlaceOnLeaderboard(5);
		if(topUsers[4].isPlayer()) placeOnLeaderboard[topUsers[4].getId()] = 4;
		else if(!topUsers[4].isPlayer()) simpleBot[topUsers[4].getId()].setPlaceOnLeaderboard(4);
		if(topUsers[3].isPlayer()) placeOnLeaderboard[topUsers[3].getId()] = 3;
		else if(!topUsers[3].isPlayer()) simpleBot[topUsers[3].getId()].setPlaceOnLeaderboard(3);
		if(topUsers[2].isPlayer()) placeOnLeaderboard[topUsers[2].getId()] = 2;
		else if(!topUsers[2].isPlayer()) simpleBot[topUsers[2].getId()].setPlaceOnLeaderboard(2);
		if(topUsers[1].isPlayer()) placeOnLeaderboard[topUsers[1].getId()] = 1;
		else if(!topUsers[1].isPlayer()) simpleBot[topUsers[1].getId()].setPlaceOnLeaderboard(1);
	}
	
	/*
	 * Check for collisions
	 */
	public void collision() {
		//temporary rectangles to use for a player
		Rectangle tempRec;
		Rectangle tempRec2;
		//check hitboxes all players
		for(int i = 0; i < Server.MAX_CLIENTS; i++) {
			if(server.getUser(i) != null) {
				if(server.getUser(i).isPlaying()) {
					tempRec = new Rectangle((int)data[i][0], (int)data[i][1], 32, 32);
					//if the player rectangle intersects either hardbot rectangle
					if(tempRec.getBounds().intersects(hardBot1.getBounds()) || tempRec.getBounds().intersects(hardBot2.getBounds())) {
						//eliminate the player and reset variables and arrays
						try {
							server.getUser(i).getDataOutputStream().writeUTF("[eliminated]" + "AI" + "/" + "BLACK");
							server.getUser(i).setPlaying(false);
							server.getUser(i).getDataOutputStream().flush();
							eliminations[i] = 0;
							//reset position on leaderboard
							if(onLeaderboard[i]) {
								removeTopUser(placeOnLeaderboard[i]);
								onLeaderboard[i] = false;
								placeOnLeaderboard[i] = 9;
							}
						} catch (IOException e) {
							server.disconnectClient(i);
						}
					}
					
					for(int c = 0; c < Server.MAX_CLIENTS; c++) {
						if(server.getUser(c) != null && c != i) {
							if(server.getUser(c).isPlaying()) {
								tempRec2 = new Rectangle((int)data[c][0], (int)data[c][1], 32, 32);
								//if one player intersects another player
								if(tempRec.getBounds().intersects(tempRec2.getBounds())) {
									if(playerColors[i].equals("RED") && playerColors[c].equals("CYAN")) {
										try {
											server.getUser(i).getDataOutputStream().writeUTF("[eliminated]" + server.getUser(c).getUsername() + "/" + playerColors[c]);
											eliminations[i] = 0;
											eliminations[c]++;
											server.getUser(c).getDataOutputStream().writeUTF("[elim+]");
											server.getUser(i).setPlaying(false);
											server.getUser(i).getDataOutputStream().flush();
											if(onLeaderboard[i]) {
												removeTopUser(placeOnLeaderboard[i]);
												onLeaderboard[i] = false;
												placeOnLeaderboard[i] = 9;
											}
										} catch (IOException e) {
											server.disconnectClient(i);
										}
									} else if(playerColors[i].equals("GREEN") && playerColors[c].equals("RED")) {
										try {
											server.getUser(i).getDataOutputStream().writeUTF("[eliminated]" + server.getUser(c).getUsername() + "/" + playerColors[c]);
											eliminations[i] = 0;
											eliminations[c]++;
											server.getUser(c).getDataOutputStream().writeUTF("[elim+]");
											server.getUser(i).setPlaying(false);
											server.getUser(i).getDataOutputStream().flush();
											if(onLeaderboard[i]) {
												removeTopUser(placeOnLeaderboard[i]);
												onLeaderboard[i] = false;
												placeOnLeaderboard[i] = 9;
											}
										} catch (IOException e) {
											server.disconnectClient(i);
										}
									} else if(playerColors[i].equals("CYAN") && playerColors[c].equals("GREEN")) {
										try {
											server.getUser(i).getDataOutputStream().writeUTF("[eliminated]" + server.getUser(c).getUsername() + "/" + playerColors[c]);
											eliminations[i] = 0;
											eliminations[c]++;
											server.getUser(c).getDataOutputStream().writeUTF("[elim+]");
											server.getUser(i).setPlaying(false);
											server.getUser(i).getDataOutputStream().flush();
											if(onLeaderboard[i]) {
												removeTopUser(placeOnLeaderboard[i]);
												onLeaderboard[i] = false;
												placeOnLeaderboard[i] = 9;
											}
										} catch (IOException e) {
											server.disconnectClient(i);
										}
									}
								}
							}
						}
					}

					//check hitboxes all bots
					for(int c = 0; c < Server.MAX_BOTS; c++) {
						if(playerColors[i] != null && simpleBot[c] != null) {
							if(tempRec.getBounds().intersects(simpleBot[c].getBounds())) {
								if(playerColors[i].equals("RED") && simpleBot[c].getColor().equals("GREEN")) {
									try {
										if(simpleBot[c].isOnLeaderboard()) {
											removeTopUser(simpleBot[c].getPlaceOnLeaderboard());
											simpleBot[c].setOnLeaderboard(false);
										}
										simpleBot[c] = null;
										simpleBot[c] = new SimpleBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.SimpleBot, colors[r.nextInt(3)], server, this, c, serverFileManager.generateName());
										eliminations[i]++;
										server.getUser(i).getDataOutputStream().writeUTF("[elim+]");
									} catch (IOException e) {
										server.disconnectClient(i);
									}
									break;
								} else if(playerColors[i].equals("GREEN") && simpleBot[c].getColor().equals("CYAN")) {
									try {
										if(simpleBot[c].isOnLeaderboard()) {
											removeTopUser(simpleBot[c].getPlaceOnLeaderboard());
											simpleBot[c].setOnLeaderboard(false);
										}
										simpleBot[c] = null;
										simpleBot[c] = new SimpleBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.SimpleBot, colors[r.nextInt(3)], server, this, c, serverFileManager.generateName());
										eliminations[i]++;
										server.getUser(i).getDataOutputStream().writeUTF("[elim+]");
									} catch (IOException e) {
										server.disconnectClient(i);
									}
									break;
								} else if(playerColors[i].equals("CYAN") && simpleBot[c].getColor().equals("RED")) {
									try {
										if(simpleBot[c].isOnLeaderboard()) {
											removeTopUser(simpleBot[c].getPlaceOnLeaderboard());
											simpleBot[c].setOnLeaderboard(false);
										}
										simpleBot[c] = null;
										simpleBot[c] = new SimpleBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.SimpleBot, colors[r.nextInt(3)], server, this, c, serverFileManager.generateName());
										eliminations[i]++;
										server.getUser(i).getDataOutputStream().writeUTF("[elim+]");
									} catch (IOException e) {
										server.disconnectClient(i);
									}
									break;
								}
								
								if(playerColors[i].equals("RED") && simpleBot[c].getColor().equals("CYAN")) {
									try {
										server.getUser(i).getDataOutputStream().writeUTF("[eliminated]" + simpleBot[c].getUsername() + "/" + simpleBot[c].getColor());
										eliminations[i] = 0;
										simpleBot[c].addElim();
										server.getUser(i).setPlaying(false);
										if(onLeaderboard[i]) {
											removeTopUser(placeOnLeaderboard[i]);
											onLeaderboard[i] = false;
											placeOnLeaderboard[i] = 9;
										}
									} catch (IOException e) {
										server.disconnectClient(i);
									}
								} else if(playerColors[i].equals("GREEN") && simpleBot[c].getColor().equals("RED")) {
									try {
										server.getUser(i).getDataOutputStream().writeUTF("[eliminated]" + simpleBot[c].getUsername() + "/" + simpleBot[c].getColor());
										eliminations[i] = 0;
										simpleBot[c].addElim();
										server.getUser(i).setPlaying(false);
										if(onLeaderboard[i]) {
											removeTopUser(placeOnLeaderboard[i]);
											onLeaderboard[i] = false;
											placeOnLeaderboard[i] = 9;
										}
									} catch (IOException e) {
										server.disconnectClient(i);
									}
								} else if(playerColors[i].equals("CYAN") && simpleBot[c].getColor().equals("GREEN")) {
									try {
										server.getUser(i).getDataOutputStream().writeUTF("[eliminated]" + simpleBot[c].getUsername() + "/" + simpleBot[c].getColor());
										eliminations[i] = 0;
										simpleBot[c].addElim();
										server.getUser(i).setPlaying(false);
										if(onLeaderboard[i]) {
											removeTopUser(placeOnLeaderboard[i]);
											onLeaderboard[i] = false;
											placeOnLeaderboard[i] = 9;
										}
									} catch (IOException e) {
										server.disconnectClient(i);
									}
								}
							}
						}
					}
				}
			}
		}
		for(int i = 0; i < Server.MAX_BOTS; i++) {
			if(simpleBot[i] != null && (simpleBot[i].getBounds().intersects(hardBot1.getBounds()) || simpleBot[i].getBounds().intersects(hardBot2.getBounds()))) {
				if(simpleBot[i].isOnLeaderboard()) {
					removeTopUser(simpleBot[i].getPlaceOnLeaderboard());
					simpleBot[i].setOnLeaderboard(false);
				}
				simpleBot[i] = null;
				simpleBot[i] = new SimpleBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.SimpleBot, colors[r.nextInt(3)], server, this, i, serverFileManager.generateName());
			}
			
			if(simpleBot[i] != null) {
				for(int c = 0; c < Server.MAX_BOTS; c++) {
					if(c != i && simpleBot[c] != null) {
						if(simpleBot[i].getColor().equals("RED") && simpleBot[c].getColor().equals("GREEN")) {
							if(simpleBot[i].getBounds().intersects(simpleBot[c].getBounds())) {
								if(simpleBot[c].isOnLeaderboard()) {
									removeTopUser(simpleBot[c].getPlaceOnLeaderboard());
									simpleBot[c].setOnLeaderboard(false);
								}
								simpleBot[c] = null;
								simpleBot[c] = new SimpleBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.SimpleBot, colors[r.nextInt(3)], server, this, c, serverFileManager.generateName());
								simpleBot[i].addElim();
							}
						} else if(simpleBot[i].getColor().equals("GREEN") && simpleBot[c].getColor().equals("CYAN")) {
							if(simpleBot[i].getBounds().intersects(simpleBot[c].getBounds())) {
								if(simpleBot[c].isOnLeaderboard()) {
									removeTopUser(simpleBot[c].getPlaceOnLeaderboard());
									simpleBot[c].setOnLeaderboard(false);
								}
								simpleBot[c] = null;
								simpleBot[c] = new SimpleBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.SimpleBot, colors[r.nextInt(3)], server, this, c, serverFileManager.generateName());
								simpleBot[i].addElim();
							}
						} else if(simpleBot[i].getColor().equals("CYAN") && simpleBot[c].getColor().equals("RED")) {
							if(simpleBot[i].getBounds().intersects(simpleBot[c].getBounds())) {
								if(simpleBot[c].isOnLeaderboard()) {
									removeTopUser(simpleBot[c].getPlaceOnLeaderboard());
									simpleBot[c].setOnLeaderboard(false);
								}
								simpleBot[c] = null;
								simpleBot[c] = new SimpleBot(r.nextInt(Server.mapSize)-Server.mapSize/2, r.nextInt(Server.mapSize)-Server.mapSize/2, ID.SimpleBot, colors[r.nextInt(3)], server, this, c, serverFileManager.generateName());
								simpleBot[i].addElim();
							}
						}
					}
				}
			}
		}
	}
	
	public SimpleBot getSimpleBot(int index) {
		return simpleBot[index];
	}
	
}
