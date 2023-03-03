package com.server;

import java.awt.Color;
import java.awt.Rectangle;

/*
 * Standard AI that follow normal player behaviour and exclusively target only one colour depending on its own colour
 */

public class SimpleBot extends GameObject {
	
	private Server server;
	private ObjectManager om;
	
	private float velX, velY;
	private Color color;
	private int eliminations = 0;
	private final int BOT_ID;
	private String username;
	
	//data to determine if and where the bot is placed on the leaderboard
	private boolean onLeaderboard;
	private int placeOnLeaderboard;
	
	public SimpleBot(float x, float y, ID id, Color color, Server server, ObjectManager om, int index, String username) {
		super(x, y, id);
		this.color = color;
		this.server = server;
		this.om = om;
		BOT_ID = index;
		this.username = username;
	}

	@Override
	public void tick() {
		x += velX;
		y += velY;
		
		acquireTarget();
	}

	//locate the nearest target to follow
	private void acquireTarget() {
		//double buffer for comparing distance to closest distance
		double tempDist;
		//distance to closest target
		double closestDist = 0;
		//id of closest target
		int closestTarget = 0;
		//if the object being followed is a player or a bot
		boolean player = false;
		
		//red will taget green, green will target cyan, cyan will target red
		if(color == Color.RED) {
			//check for closest bots
			for(int i = 0; i < Server.MAX_BOTS; i++) {
				if(BOT_ID != i && om.getSimpleBot(i) != null && om.getSimpleBot(i).getColor() == "GREEN") {
					try {
						//distance formula
						tempDist = Math.sqrt(Math.pow((x - om.getSimpleBot(i).getX()), 2) + Math.pow((y - om.getSimpleBot(i).getY()), 2));
						//if distance is less than closest distance, set as the new closest distance
						if(closestDist == 0 || tempDist < closestDist) {
							closestDist = tempDist;
							closestTarget = i;
							player = false;
						}
					} catch(NullPointerException e) {
						
					}
				}
			}
			
			//check for closest players
			for(int i = 0; i < Server.MAX_CLIENTS; i++) {
				if(server.getUser(i) != null && om.playerColors[i] == "GREEN") {
					try {
						tempDist = Math.sqrt(Math.pow((x - om.data[i][0]), 2) + Math.pow((y - om.data[i][1]), 2));
						if(closestDist == 0 || tempDist < closestDist) {
							closestDist = tempDist;
							closestTarget = i;
							player = true;
						}
					} catch(NullPointerException e) {
						
					}
				}
			}
		} else if(color == Color.GREEN) {
			for(int i = 0; i < Server.MAX_BOTS; i++) {
				if(BOT_ID != i && om.getSimpleBot(i) != null && om.getSimpleBot(i).getColor() == "CYAN") {
					try {
						tempDist = Math.sqrt(Math.pow((x - om.getSimpleBot(i).getX()), 2) + Math.pow((y - om.getSimpleBot(i).getY()), 2));
						if(closestDist == 0 || tempDist < closestDist) {
							closestDist = tempDist;
							closestTarget = i;
							player = false;
						}
					} catch(NullPointerException e) {
						
					}
				}
			}
			
			for(int i = 0; i < Server.MAX_CLIENTS; i++) {
				if(server.getUser(i) != null && om.playerColors[i] == "CYAN") {
					try {
						tempDist = Math.sqrt(Math.pow((x - om.data[i][0]), 2) + Math.pow((y - om.data[i][1]), 2));
						if(closestDist == 0 || tempDist < closestDist) {
							closestDist = tempDist;
							closestTarget = i;
							player = true;
						}
					} catch(NullPointerException e) {
						
					}
				}
			}
		} else if(color == Color.CYAN) {
			for(int i = 0; i < Server.MAX_BOTS; i++) {
				if(BOT_ID != i && om.getSimpleBot(i) != null && om.getSimpleBot(i).getColor() == "RED") {
					try {
						tempDist = Math.sqrt(Math.pow((x - om.getSimpleBot(i).getX()), 2) + Math.pow((y - om.getSimpleBot(i).getY()), 2));
						if(closestDist == 0 || tempDist < closestDist) {
							closestDist = tempDist;
							closestTarget = i;
							player = false;
						}
					} catch(NullPointerException e) {
						
					}
				}
			}
			
			for(int i = 0; i < Server.MAX_CLIENTS; i++) {
				if(server.getUser(i) != null && om.playerColors[i] == "RED") {
					try {
						tempDist = Math.sqrt(Math.pow((x - om.data[i][0]), 2) + Math.pow((y - om.data[i][1]), 2));
						if(closestDist == 0 || tempDist < closestDist) {
							closestDist = tempDist;
							closestTarget = i;
							player = true;
						}
					} catch(NullPointerException e) {
						
					}
				}
			}
		}
		
		float diffX = 0;
		float diffY = 0;
		//calculate the difference in X and Y
		if(player) {
			diffX = x - om.data[closestTarget][0];
			diffY = y - om.data[closestTarget][1];
		} else if(!player) {
			if(om.getSimpleBot(closestTarget) != null) diffX = x - om.getSimpleBot(closestTarget).getX();
			if(om.getSimpleBot(closestTarget) != null) diffY = y - om.getSimpleBot(closestTarget).getY();
		} else {
			diffX = 0;
			diffY = 0;
		}
		
		//formula to set speed and follow the co-ordinates of the closest player/bot
		//*5 is the speed modifier
		velX = (float) ((-1.0/closestDist) * diffX)*5;
		velY = (float) ((-1.0/closestDist) * diffY)*5;
	}
	
	@Override
	public Rectangle getBounds() {
		return new Rectangle((int)x, (int)y, 32, 32);
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public void setX(float x) {
		this.x = x;
	}
	
	public void getY(float y) {
		this.y = y;
	}
	
	//convert and return the color object into a string of the color
	public String getColor() {
		if(color == Color.RED) {
			return "RED";
		} else if(color == Color.GREEN) {
			return "GREEN";
		} else if(color == Color.CYAN) {
			return "CYAN";
		}
		return "BLUE";
	}
	
	public int getElims() {
		return eliminations;
	}
	
	public void addElim() {
		eliminations++;
	}
	
	public int getId() {
		return BOT_ID;
	}
	
	public String getUsername() {
		return username;
	}
	
	public boolean isOnLeaderboard() {
		return onLeaderboard;
	}
	
	public void setOnLeaderboard(boolean onLeaderboard) {
		this.onLeaderboard = onLeaderboard;
	}
	
	public int getPlaceOnLeaderboard() {
		return placeOnLeaderboard;
	}
	
	public void setPlaceOnLeaderboard(int place) {
		placeOnLeaderboard = place;
	}
	
}
