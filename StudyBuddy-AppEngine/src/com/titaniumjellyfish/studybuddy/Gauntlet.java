package com.titaniumjellyfish.studybuddy;

import java.util.ArrayList;
import java.util.List;

import com.titaniumjellyfish.studybuddy.model.NoiseMap;
import com.titaniumjellyfish.studybuddy.model.Room;

public class Gauntlet {

	/**
	 * run some tests on other code
	 */
	public static void main(String[] args){
		List<String> comments = new ArrayList<String>();
		comments.add("so good!");
		comments.add("&hacker=true");
		List<NoiseMap> noises = new ArrayList<NoiseMap>();
		noises.add(new NoiseMap(1.5, 1234, true));
		Room r = new Room(43.704243, -72.286606, "Sudikoff", "003", 16, 1.5, 3.9, 1, 82, comments, noises, null);
		
		System.out.println("Room r in json:");
		System.out.println(r.toJSON());
//		System.out.println("\nRoom's default key: "+r.getKey());
	}
}
