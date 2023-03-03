package com.server;

import java.awt.Rectangle;

/*
 * Parent class for all game-related objects
 * Each class will have an x, y value with an id
 * Each object will have a tick method and a getBounds method
 */

public abstract class GameObject {
	
	//co-ordinates
	protected float x, y;
	//identification
	protected ID id;
	
	public GameObject(float x, float y, ID id) {
		this.x = x;
		this.y = y;
		this.id = id;
	}
	
	//update (tick) all objects in a loop
	public abstract void tick();
	//returns a rectangle of each object which can then be used to check for hitbox collisions
	public abstract Rectangle getBounds();
	
}
