package com.server;

/*
 * Separate thread used for checking hitbox collisions to not congest the main object manager thread and the output of data, as well as updating and refreshing leaderboard
 */

public class Collision implements Runnable {

	private Server server;
	private ObjectManager om;
	
	//amount of ticks before the leaderboard is refreshed to clear any un-updated scores
	private final int REFRESH_TIMER = 1000000;
	private int timer = REFRESH_TIMER;
	
	public Collision(Server server, ObjectManager om) {
		this.server = server;
		this.om = om;
		
		//initialize and begin the thread for hitbox detection and updating the leaderboard
		Thread collisionThread = new Thread(this);
		collisionThread.start();
	}
	
	@Override
	public void run() {
		while(server.isRunning()) {
			//update leaderboard
			om.updateLeaderboard();
			//check for collisions
			om.collision();
			
			//update refresh timer and refresh leaderboard when refresh timer hits 0
			if(timer > 0) timer--;
			else if(timer <= 0) {
				timer = REFRESH_TIMER;
				om.refreshLeaderboard();
			}
		}
	}
	
}
