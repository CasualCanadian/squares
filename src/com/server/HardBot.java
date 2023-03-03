package com.server;

import java.awt.Rectangle;

/*
 * AI that will chase around players of all colours and can not be eliminated out of the game
 * Used to control quantities of different colours, to prevent one colour from becoming dominant
 */

public class HardBot extends GameObject {
	
	private ObjectManager om;
	private Server server;
	
	private float velX, velY;
	
	public HardBot(float x, float y, ID id, ObjectManager om, Server server) {
		super(x, y, id);
		this.om = om;
		this.server = server;
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
		
		//check for closest bots
		for(int i = 0; i < Server.MAX_BOTS; i++) {
			if(om.getSimpleBot(i) != null) {
				//distance formula
				tempDist = Math.sqrt(Math.pow((x - om.getSimpleBot(i).getX()), 2) + Math.pow((y - om.getSimpleBot(i).getY()), 2));
				//if distance is less than closest distance, set as the new closest distance
				if(closestDist == 0 || tempDist < closestDist) {
					closestDist = tempDist;
					closestTarget = i;
					player = false;
				}
			}
		}
		
		//check for closest players
		for(int i = 0; i < Server.MAX_CLIENTS; i++) {
			if(server.getUser(i) != null) {
				tempDist = Math.sqrt(Math.pow((x - om.data[i][0]), 2) + Math.pow((y - om.data[i][1]), 2));
				if(closestDist == 0 || tempDist < closestDist) {
					closestDist = tempDist;
					closestTarget = i;
					player = true;
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
		//*7 is the speed modifier
		velX = (float) ((-1.0/closestDist) * diffX)*7;
		velY = (float) ((-1.0/closestDist) * diffY)*7;
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
	
}
