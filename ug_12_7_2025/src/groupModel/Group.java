/*
Copyright 2020 by Jeffrey C. Schank
Licensed under the Academic Free License version 3.0
See https://opensource.org/licenses/AFL-3.0 for more information
 */
package groupModel;
/**
 * This class implements groups located in space.  Groups only handle playing the
 * nDG.
 */
import java.awt.Color;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.Bag;
import sweep.GUIStateSweep;

public class Group implements Steppable {
	int x; //x location
	int y; //y location
	Bag members = null; //contains the members of the group
	public Stoppable event;//so the group can be removed from the schedule
	Environment state;
	Bag proposers = new Bag();
	Bag responders = new Bag();
	Bag  accepters = new Bag();

	public double getShare() {
		double share = 0;
		for (int i=0;i<members.numObjs;i++) {
			Agent a = (Agent)members.objs[i];
			share += a.offer;
		}
		return share/(double)members.numObjs;
	}

	public Group(Environment state, int x, int y, Bag members) {
		super();
		this.x = x;
		this.y = y;
		this.members = members;
		this.state = state;
		if(!state.paramSweeps)
			setColor();
		for(int i=0;i<this.members.numObjs;i++) {
			Agent a = (Agent)this.members.objs[i];
			a.setGroup(this);
		}
	}

	public void setColor(){
		double offer = 0;
		for (int i=0;i<members.numObjs;i++) {
			Agent a = (Agent)members.objs[i];
			offer+= a.offer;
		}
		offer = offer/(double)members.numObjs;
		double[] oA =state.offerArray;
		Color[] colors = {Color.RED/*0.0*/,Color.ORANGE/*0.1*/,Color.YELLOW/*0.2*/,Color.MAGENTA/*0.3*/,Color.GREEN/*0.4*/,Color.BLUE/*0.5*/,
				Color.CYAN/*0.6*/,Color.BLACK/*0.6<>1*/,Color.BLACK/*0.6<>1*/,Color.BLACK/*0.6<>1*/,Color.BLACK/*0.6<>1*/};
		Color c = Color.BLACK;
		for(int i=0;i<colors.length;i++) {
			if(offer <= oA[i]) {
				c = colors[i];
				break;
			}
		}
		OvalPortrayal2D o = new OvalPortrayal2D(c);
		GUIStateSweep guiState = (GUIStateSweep)state.gui;
		guiState.agentsPortrayalSparseGrid.setPortrayalForObject(this, o);

	}	

	

	/**
	 * This method implements the UG at the group level. The proposers and responders
	 * for each round are randomly determined and sorted into proposer and responder 
	 * bags. The recipients are randomly shuffled and then the proposers play the
	 * responders in order until the responders are exhausted.  For odd sized groups,
	 * they the odd agent plays a ug half game.
	 */

	public void ug() {
		if(this.members.numObjs == 1) {
			System.out.println("Singleton");
			return; //we are done
		}
		proposers.clear();//bags used for dictators and recipients.  Created at
		responders.clear();//construction time and cleared for each round of play.
		Bag players = new Bag(members);
		players.shuffle(state.random);
		if(state.random.nextBoolean(0.5)) {//if odd, half the time there will be too many proposer
			//else too many recipients by 1
			Agent a; 
			for(int i =0;i<players.numObjs/2;i++) {
				a = (Agent)players.objs[i];//cast each as an agent
				proposers.add(a);
			}
			for(int i =players.numObjs/2;i<players.numObjs;i++) {
				a = (Agent)players.objs[i];//cast each as an agent
				responders.add(a);

			}
		}
		else {
			Agent a; 
			for(int i =0;i<players.numObjs/2;i++) {
				a = (Agent)players.objs[i];//cast each as an agent
				responders.add(a);
			}
			for(int i =players.numObjs/2;i<players.numObjs;i++) {
				a = (Agent)players.objs[i];//cast each as an agent
				proposers.add(a);

			}
		}


		for (int i=0; i<proposers.numObjs;i++) {//start with the first proposer and work through the array
			Agent p = (Agent)proposers.objs[i];//cast as a proposer agent
			Agent r = null;
			double offer = 0.0;
			if(i < responders.numObjs) {//if there are no more recipients go to else
				r = (Agent)responders.objs[i];
			}
			else {//play a half game, get a random recipient
				r = (Agent)responders.objs[state.random.nextInt(responders.numObjs)];
				if(p.offer >= r.accept) {
					p.getResourceTN(state);
					offer = p.offer(state) * p.endowment;
					p.resources += (p.endowment - offer) ; //we are done
				}
				break;//we can break the loop at this point
			}

			if(p.offer >= r.accept) {//now play
				p.getResourceTN(state);
				offer = p.offer(state) * p.endowment;
				p.resources+=(p.endowment- offer);//the amount of the resource endowment the dictator keeps
				r.resources+= offer;//give it the offer
			}
			else {
				//nothing for both
			}

		}
		if(proposers.numObjs < responders.numObjs) {//for cases with more recipients
			Agent p = (Agent)proposers.objs[state.random.nextInt(proposers.numObjs)]; //get a random proposer
			Agent r = (Agent)responders.objs[responders.numObjs-1]; //get the odd responder, the last one
			if(p.offer >= r.accept) {
				p.getResourceTN(state); //getResource
				r.resources+= p.endowment*p.offer;

			}
		}

		proposers.clear();//clear the bags just in case an agent dies, it won't
		responders.clear();//hang around for a while
	}


	/**
	 * Handles the end of a group, when it has no members.
	 * @param state
	 */
	public boolean die(Environment state) {
		if(members == null || members.numObjs==0) {
			state.sparseSpace.remove(this);
			event.stop();
			return true;
		}
		return false;
	}
	
	public void groupDisperse(Environment state) {
		if(members.numObjs < state.minGroupSize) {
			Agent a = (Agent)members.objs[0];//there is at least one
			a.groupDisperse(state);//disperse
			die(state);
			System.out.println("Group dispersed");
		}
	}

	/**
	 * Step method for groups.  First check to see if it is empty.  Second,
	 * if not empty, play the nDG.
	 */
	public void step(SimState state) {
		Environment eState = (Environment)state;
		if(die(eState))
			return;//if no members
		groupDisperse(eState);//if too few
		ug();
	}
}
