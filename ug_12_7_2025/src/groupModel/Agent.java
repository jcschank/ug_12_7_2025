/*
Copyright 2025 by Jeffrey C. Schank
Licensed under the Academic Free License version 3.0
See https://opensource.org/licenses/AFL-3.0 for more information
 */
package groupModel;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.grid.SparseGrid2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntBag;
import sim.util.distribution.Uniform;
/**
 * 
 * @author jcschank
 *  * @author jcschank
 * Agents play the ultimatum game to accumulate resources sufficient to reproduce.  After each 
 * reproductive episode, there is inter-birth interval (IBI) before the next reproductive event. 
 * The minimum IBI and the expected accumulation of resources  correspond.  If the
 * population is at capacity, an agent fails to reproduce and looses the resources 
 * required to reproduce.  Otherwise, an agent reproduces with mutation rate r.  If an
 * a mutation occurs, the offspring has a randomly selected offer different from the
 * parent agent.  When an agent is born, it disperses at rate m to another group locally, otherwise
 * it remains in the parent group.  If the group is larger than the maximum size for a 
 * group, the agent detecting the group is too large triggers a group fission event in which
 * approximately have of the agents are placed in the offspring group and the other agents
 * remain in the parent group. If a group is too small, agents disperse randomly to the nearst
 * groups.
 *
 *
 */

public class Agent implements Steppable {
	int x; //x location of group
	int y; //y location of group
	int age; //current age of agent
	int maxAge;//maximum age of agent
	double resources=0;
	double endowment=0;//store the endowment each step when dictator
	double offer =0;//amount offered, 0 ≤ offer ≤ 1
	double accept = 0.0; //for the ultimatum game
	double currentOffer = 0.0;
	double reproductiveCount = 0;//counts down minimum delay
	double io; //resources required to reproduce
	double ibiTau;//minimum length of gestation
	public Group group;//the group the agent is currently a member

	/* Variables used for calculations*/
	Environment state;//store access to the environment
	public Stoppable event;//allows the removal of an agent from the schedule
	IntBag xPos = new IntBag();//used for finding locations
	IntBag yPos = new IntBag();//used for finding locations
	Bag locations = new Bag();//used for finding locations
	Bag groups = new Bag();//used for storing groups
	double offspring = 0.0; //for counting offspring successfully produced
	Uniform uniform = null;



	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}
	
	public double getResourceTN (Environment state) {
		return this.endowment = state.tnormal.nextTN();
	}

	public double getResourceU (Environment state) {
		return this.endowment = uniform.nextDouble();
	}

	/**
	 * Agent constructor.
	 * @param state  //pass the environment
	 * @param offer  //offer level for the agent
	 * @param startup //if at startup, do startup stuff
	 * @param resources //initial resources if any
	 * @param x //location of group agent belongs to
	 * @param y
	 */
	public Agent(Environment state,double offer,double accept, boolean startup, double resources, int x, int y) {
		super();

		this.state = state;
		this.x = x;
		this.y = y;
		this.resources = resources;
		this.offer = offer;
		this.accept = accept;
		this.uniform = state.uniform;

		maxAge = (int)(state.averageAge + state.random.nextGaussian()*state.sdAge*state.averageAge); //percent of average age
		// set the maximum age
		if(startup) {
			age = state.random.nextInt(maxAge);
			//if at startup, set the initial age in the uniform random distribution between 0 and maxAge
		} else {
			age = 0;//if not startup, initial age is 0s
		}


		io = state.io;
		ibiTau = state.ibiTau; //set ibi
	}

	/**
	 * Helper method that counts agents within all groups.
	 * @param state
	 * @return
	 */
	public int countAgents(Environment state){
		int n = 0;
		Bag agents = state.sparseSpace.getAllObjects();
		for(int i=0; i< agents.numObjs; i++) {
			Group g = (Group)agents.objs[i];
			n+=g.members.numObjs;
		}
		return n;
	}

	public Double2D chromosome(Environment state) {
		if(state.sex) {
			if(this.group.members.numObjs < 2) {//get mate from cluster
				System.out.println("Singleton Sex");
				groupDisperse(this.state);
				return null;
			}
			Agent mate = (Agent)this.group.members.objs[state.random.nextInt(this.group.members.numObjs)];
			while(!this.equals(mate)) {
				mate = (Agent)this.group.members.objs[state.random.nextInt(this.group.members.numObjs)];
			}
			if(state.random.nextBoolean(0.5)) {
				return new Double2D(this.offer, mate.accept);
			} else
			{
				return new Double2D(mate.offer, this.accept);
			}
		}
		else {
			return new Double2D(this.offer, this.accept);//asexual reproduction
		}
	}

	/**
	 * Reproductive method for agents.
	 * @param state
	 * @param searchRadius
	 * @return
	 */
	public Agent reproduce(Environment state, final int searchRadius){
		if (state.maxN <= countAgents(state)) {//test for number of agents
			ibiTau = state.ibiTau;//reset the gestationPeriod to the base rate
			if(state.carryOver)
				resources -=  io;//truncate resources
			else
				resources = 0.0;
			return null;//No agent was created, to return null and agent starts over
		}
		Group g=null; //offspring's  group
		boolean dispersed = false;
		if(state.random.nextBoolean(state.dispersalRate)) {
			g = findGroupLocal( state,x, y, state.sparseSpace.TOROIDAL, searchRadius, false);
			//finds a random group within the parent group's search radius
			if (g == null) {//if there isn't one, set the group to the parent group
				g = this.group;
			}
			else
				dispersed = true;
		}
		else {
			g = this.group;//set offspring group to the parent group
		}

		Double2D b = mutationStrategies(state);
		if(b == null) {
			return null; //only could happen for sex
		}
		state.experimenter.recordDispersion(dispersed);
		double offer = b.x;
		double accept = b.y;
		if(state.carryOver)
			resources -=  io;//truncate resources
		else
			resources = 0.0;
		double offspringResources = 0.0;//offspring resources are set to 0
		Agent a = new Agent(state,offer,accept, false, offspringResources, g.x, g.y);//create an agent
		a.accept = accept;
		offspring++;//count offspring
		a.setGroup(g);//set it to the selected group
		g.members.add(a); //add it to the group
		a.x = g.x;//set location
		a.y = g.y;
		a.event = state.schedule.scheduleRepeating(a);//schedule agent
		return a; //return the agent

	}


	public Double2D mutationStrategies(Environment state) {
		Double2D chromosome = chromosome( state);
		if(chromosome == null) {
			return null;//can happen for sex
		}
		double offer ;
		double accept;
		if(state.sex) {
			offer = chromosome.x;
			accept = chromosome.y;
		}
		else {
			offer = this.offer;
			accept = this.accept;
		}
		Bag b = new Bag();

		if(state.random.nextBoolean(state.mutationRate)) {
			accept = state.offerArray[state.random.nextInt(state.offerArray.length)];//less than or equal to equity
		}

		if(state.random.nextBoolean(state.mutationRate)) {
			offer = state.offerArray[state.random.nextInt(state.offerArray.length)];//less than or equal to equity
		}


		b.add(offer);
		b.add(accept);
		return new Double2D(offer,accept);
	}

	public double accept(Environment state, double endowment) {
		return accept * endowment;
	}

	public double offer(Environment state) {
		this.currentOffer = offer;
		return this.currentOffer;
	}

	/**
	 * When an agent dies, this method records data, removes the agent
	 * from the schedule, and removes the agent from its group.
	 * @param state
	 */
	public void die(Environment state) {
		state.experimenter.recordOffspring(this);//record the number of offsrping produced
		state.experimenter.recordOffers(this);//records the offer for mean calculation
		state.experimenter.recordOffer(this);//bins the offer
		state.experimenter.recordAccepts(this);//records the offer for mean calculation
		state.experimenter.recordAccept(this);//bins the offer
		event.stop();//remove the agent from the schedule
		group.members.remove(this);//remove self from group
		if(state.dynamicColorGroup) group.setColor();//this recolors a group
	}

	/**
	 * Helper method to find an empty, random location within the search radius of a group.
	 * Used when a group fissions in the method groupFission.
	 * @param state
	 * @param x
	 * @param y
	 * @param mode
	 * @param searchRadius
	 * @param includeOrigin
	 * @return
	 */
	public Int2D randomUniqueLocation(Environment state,final int x, final int y, final int mode, final int searchRadius, boolean includeOrigin){
		xPos.clear();
		yPos.clear();
		locations.clear();
		state.sparseSpace.getMooreLocations(x, y, searchRadius, mode, includeOrigin, xPos, yPos);
		if(xPos.numObjs == 0)
			return null;
		//we want to randomize the selection of a new empty cell, so the first/second pass method should be fast
		final int k = state.random.nextInt(xPos.numObjs); //a random starting position
		for(int i=0;i<xPos.numObjs;i++) {//find all empty locations
			if(state.sparseSpace.getObjectsAtLocation(xPos.objs[i], yPos.objs[i])==null) {
				locations.add( new Int2D(xPos.objs[i], yPos.objs[i]));
			}
		}
		if(locations.numObjs>0) {//If there is at least one empty location, return one randomly
			return (Int2D)locations.objs[state.random.nextInt(locations.numObjs)];
		}
		else
			return null;//if there were no returns in the first and second passes, then there are no empty locations
	}

	/**
	 * Finds a random group within the search radius of the calling group if one exists.
	 * @param state
	 * @param x
	 * @param y
	 * @param mode
	 * @param searchRadius
	 * @param includeOrigin
	 * @return
	 */
	public Group findGroupLocal(Environment state,final int x, final int y, final int mode, final int searchRadius, boolean includeOrigin){
		Bag groups;
		groups = state.sparseSpace.getMooreNeighbors(x, y, searchRadius, mode, includeOrigin);
		if(groups.numObjs == 0)
			return null;
		//we want to randomize the selection of a occupied empty cell, so the first/second pass method should be fast
		//check for empty groups and remove them
		Bag nonEmptyGroups = new Bag();
		for(int i=0;i<groups.numObjs;i++) {
			Group g = (Group)groups.objs[i];
			if(g.members.numObjs > 0) {
				nonEmptyGroups.add(g);
			}
		}
		if(nonEmptyGroups.numObjs == 0)
			return null;
		
		final int k = state.random.nextInt(nonEmptyGroups.numObjs); //a random group
		return (Group)nonEmptyGroups.objs[k];
	}

	/**
	 * Finds a random group nearest to calling group if one exists.
	 * @param state
	 * @param x
	 * @param y
	 * @param mode
	 * @param searchRadius
	 * @param includeOrigin
	 * @return
	 */
	public Group findGroupNearest(Environment state,final int x, final int y, final int mode){
		if(state.sparseSpace.getAllObjects().numObjs <2)//make sure there is at least one other group
			return null;
		Bag groups;
		int i = 1; //starting search radius
		groups = state.sparseSpace.getMooreNeighbors(x, y, i, mode, false);
		Group g = null;
		while(groups.numObjs == 0 || g == null) {//loop till at least one is found
			i++;//increment search radius
			groups = state.sparseSpace.getMooreNeighbors(x, y, i, mode, false);
			if(groups.numObjs > 0) {
				groups.shuffle(state.random);
				for(int j=0;j<groups.numObjs;j++) {
					Group o = (Group)groups.objs[j];
					if(o.members.numObjs > 0) {
						g = o;
						break;
					}
				}
			}
		}

		return g;
	}

	/**
	 * Creates an offspring group if an empty space exists within the parent group radius. The offspring
	 * group is created by randomly selecting agents from the parent group with probability 0.5.  Thus, on average,
	 * groups are evenly split.
	 * @param state
	 */
	public void groupFission(Environment state) {
		if(group.members.numObjs> state.maxGroupSize) {
			Int2D xy = null;
			if(state.random.nextBoolean(state.globalGroupDispersion)) {
				int x = state.random.nextInt(state.gridWidth);
				int y = state.random.nextInt(state.gridHeight);
				int test = 0;
				while(state.sparseSpace.getObjectsAtLocation(x, y) != null) {//don't get caught in a loop
					if(test >= 1000) {
						System.out.println("No space found after 10000 attempts");
						break;//break while statement
					}
					x = state.random.nextInt(state.gridWidth);
					y = state.random.nextInt(state.gridHeight);
					test++;
				}
				xy = new Int2D(x,y);
			}
			else {
				xy = randomUniqueLocation(state,x,y,SparseGrid2D.TOROIDAL,state.groupRadius,false);
				if(xy == null)
					return;
			}
			Bag newMembers = new Bag();
			Bag oldMembers = new Bag();
			int count = 0;
			final int minGroupSize = state.minGroupSize;
			while (newMembers.numObjs <= minGroupSize || oldMembers.numObjs <= minGroupSize) {
				if(count > 0) {
					newMembers.clear();
					oldMembers = new Bag(group.members);
				}
				for(int i=0;i<oldMembers.numObjs;i++) {
					if(state.random.nextBoolean(0.5)) {
						Agent a = (Agent)oldMembers.objs[i];
						a.x = xy.x;
						a.y = xy.y;
						newMembers.add(a);
						oldMembers.remove(a);
					}
				}
				count++;
			}

			if (newMembers.numObjs <= 1 || oldMembers.numObjs <= 1) {
				System.out.println("Singleton fission 2");
			}
			group.members = oldMembers;//just make the members the oldMembers bag
			Group g = new Group(state,xy.x,xy.y,newMembers);
			g.event = state.schedule.scheduleRepeating(state.schedule.getTime()+1,1,g);//schdule after agents
			state.sparseSpace.setObjectLocation(g, xy.x, xy.y);
			//Make sure the groups are big enough

		}

	}

	/**
	 * Members of a group disperse to other groups within the agent disperse
	 * radius if the number of members falls below the minGroupSize. 
	 * @param state
	 */
	public void groupDisperse(Environment state) {
		Bag members = group.members;
		if(members.numObjs< state.minGroupSize) {
			Group g = findGroupNearest( state,group.x, group.y, state.sparseSpace.TOROIDAL);
			if(g==null) return;//nowhere to go
			for(int i=0;i<members.numObjs;i++) {
				Agent a = (Agent)members.objs[i];
				a.x = g.x;
				a.y = g.y;
				a.setGroup(g);
				g.members.add(a);
			}
			members.clear();//clear the group members, will die when called
		}
	}

	/**
	 * Step method first assesses age, handles reproduction, and increments age. Playing the nDG
	 * is accomplished at the level of the group.
	 */
	public void step(SimState state) {
		if(age >= maxAge) {
			die(this.state); 
			groupDisperse(this.state);//if group is too small after death of a member, disperse
			return;
		}
		if(this.state.ibi) {
			reproductiveCount++;
			if (reproductiveCount >= ibiTau && resources >=  io) {
				reproduce(this.state,this.state.dispersalRadius);
				reproductiveCount=0.0;
				groupFission(this.state);//check if group too large after possible birth of a member.
			} 
		}
		else {

			if (resources >=  io) { 
				reproduce(this.state,this.state.dispersalRadius);
				groupFission(this.state);//check if group too large after possible birth of a member.
			} 
		}

		age++;

	}

}
