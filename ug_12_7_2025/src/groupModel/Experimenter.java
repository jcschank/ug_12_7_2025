/*
Copyright 2022 by Jeffrey C. Schank
Licensed under the Academic Free License version 3.0
See https://opensource.org/licenses/AFL-3.0 for more information
 */
package groupModel;
/**
 * The experimenter class extends the observer class in MASONplus7.  Experimenters
 * collect and save data to files.  They also manage charts.
 */
import observer.Bin;
import observer.Observer;
import sim.util.Bag;
import sim.engine.SimState;
import sweep.ParameterSweeper;
import sweep.SimStateSweep;

public class Experimenter extends Observer{
	public Environment state = null;//handle to the environment
	public double offer = 0;//used for average offers
	public double accept = 0.0;//used for average accept
	public double reject = 0.0;
	public double rejectG = 0.0;
	public double nO = 0;//count agents for average offers
	public double nA = 0.0;//count agents for average accepts
	public double nR = 0.0;//number reject + not reject
	public double nRG = 0.0;
	public double nRs = 0.0; //number of possible rejection events sampled
	public Bin fitness;//for binning agent fitness based on offer
	public Bin offers;//frequency of offers
	public Bin accepts;//frequency of accept thresholds
	public Bin rejects; //frequency that offers are rejected
	public Bin clusters; //measures the density of groups for each group
	double[] values = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25};
	public Bin clusterOffers;//mean offer for cluster size
	public Bin clusterN; //total number of agents in cluster
	public double offerAtT = 0;//offers at the selected round
	public double acceptAtT = 0;
	public double nAtTo = 0;//total agents at the selected round for offer
	public double nAtTa = 0;//total agents at the selected round for accept
	public double dispersion = 0.0;
	public double dispersionN = 0.0;
	/**
	 * Experimenter constructor.
	 * @param fileName
	 * @param folderName
	 * @param state
	 * @param sweeper
	 * @param precision
	 * @param headers
	 */
	public Experimenter(String fileName, String folderName, SimStateSweep state, ParameterSweeper sweeper,
			String precision, String[] headers) {
		super(fileName, folderName, state, sweeper, precision, headers);
		this.state = (Environment)state;//set the enviroment to the internal state

	}
	
	public void initialize(Environment state, double[] offerArray) {
		this.reSet();
		this.setEstate(state);//pass the experimenter the Environment
		this.initializeFitnessBins(offerArray);
		this.initializeOfferBins(offerArray);
		this.initializeAcceptBins(offerArray);
		this.initializeRejectBins(offerArray);
		this.intializeClusterBins(values);
	}


	/**
	 * reset offers and n to 0.
	 */
	public void reSet() {
		offer=0;
		accept = 0.0;
		reject = 0.0;
		nO=0;
		nA= 0.0;
		nR = 0.0;
		dispersion = 0.0;
		dispersionN = 0.0;
	}

	/**
	 * reset offerAtT and nAtT to 0.
	 */
	public void reSetAtT() {
		offerAtT = 0;
		acceptAtT = 0;
		nAtTo = 0;
		nAtTa = 0;
	}

	public void setEstate(Environment state) {
		this.state = state;
	}

	/**
	 * Calculate mean offers at a given round.
	 * @param state
	 * @return
	 */
	public double getMeanOfferAtT(Environment state) {
		Bag groups = state.sparseSpace.getAllObjects();
		for(int i = 0;i<groups.numObjs;i++) {
			Group g = (Group)groups.objs[i];
			for(int j=0;j<g.members.numObjs;j++) {
				Agent a = (Agent)g.members.objs[j];
				offerAtT += a.offer;
				nAtTo++;
			}
		}
		if(nAtTo>0)
			return offerAtT/nAtTo;
		else
			return 0.0;
	}
	
	/**
	 * Calculate mean offers at a given round.
	 * @param state
	 * @return
	 */
	public double getMeanAcceptAtT(Environment state) {
		Bag groups = state.sparseSpace.getAllObjects();
		for(int i = 0;i<groups.numObjs;i++) {
			Group g = (Group)groups.objs[i];
			for(int j=0;j<g.members.numObjs;j++) {
				Agent a = (Agent)g.members.objs[j];
				acceptAtT += a.accept;
				nAtTa++;
			}
		}
		if(nAtTa>0)
			return acceptAtT/nAtTa;
		else
			return 0.0;
	}


	/**
	 * If there are no more groups, then stop the experimenter.
	 * @param state
	 */
	public void stop(Environment state) {
		Bag a = state.sparseSpace.getAllObjects();
		if(a == null || a.numObjs == 0) event.stop();
	}

	public void initializeFitnessBins(double[] mutationArray) {
		fitness = new Bin(mutationArray);
	}

	public void initializeOfferBins(double[] mutationArray) {
		offers= new Bin(mutationArray);
	}
	
	public void initializeAcceptBins(double[] mutationArray) {
		accepts= new Bin(mutationArray);
	}
	
	public void initializeRejectBins(double[] mutationArray) {
		rejects= new Bin(mutationArray);
	}

	public void intializeClusterBins(double[] values) {
		this.clusters = new Bin(values);
		this.clusterOffers = new Bin(values);
		this.clusterN = new Bin(values);
	}

	public void recordCluster(double n) {
		clusters.binDataF(n,true);	
	}

	public void recordCluster(double n, double meanoffer, double numAgents) {
		clusters.binDataF(n,true);	
		clusterOffers.binData(n, meanoffer, true);
		clusterN.binData(n, numAgents, true);
	}


	public double sizeCluster (Bag cluster) {
		double n = 0;
		for(int i=0;i<cluster.numObjs;i++) {
			Group g = (Group)cluster.objs[i];
			n+= g.members.numObjs;
		}
		return n;
	}

	public double meanOfferCluster (Bag cluster) {
		double n = 0;
		double offer =0.0;
		for(int i=0;i<cluster.numObjs;i++) {
			Group g = (Group)cluster.objs[i];
			n+= g.members.numObjs;
			for(int j=0;j<g.members.numObjs;j++) {
				Agent a = (Agent)g.members.objs[j];
				offer += a.offer;

			}
		}
		if(n >0)
			return offer/n;
		else
			return 0.0;
	}


	public void getClusterSizes(Environment state) {
		if(state.burnIn <= state.schedule.getSteps()) {
			Bag groups = new Bag();
			Bag cluster = new Bag();
			groups.addAll(state.sparseSpace.getAllObjects());//add the groups
			while(groups.numObjs > 0) {
				Group g = (Group)groups.pop(); //get top most object
				Bag neighbors = state.sparseSpace.getMooreNeighbors(g.x, g.y, 1, state.sparseSpace.TOROIDAL, false);
				cluster.add(g);
				cluster.addAll(neighbors);
				cluster = cluster(state,cluster,neighbors);
				double meanOffer = meanOfferCluster(cluster);
				double numAgents = sizeCluster(cluster);
				groups.removeAll(cluster);
				recordCluster(cluster.numObjs, meanOffer, numAgents);
				cluster.clear();
			}
		}


		return;
	}

	public Bag cluster(Environment state, Bag cluster, Bag toCheck ) {
		Bag nextToCheck = new Bag();
		for(int i = 0; i < toCheck.numObjs; i++) {
			Group g = (Group)toCheck.objs[i];
			Bag neighbors = state.sparseSpace.getMooreNeighbors(g.x, g.y, 1, state.sparseSpace.TOROIDAL, false);
			for(int j = 0; j<neighbors.numObjs;j++) {
				if(!cluster.contains(neighbors.objs[j])) {
					cluster.add(neighbors.objs[j]);
					nextToCheck.add(neighbors.objs[j]);
				}
			}
		}//end for
		if(nextToCheck.numObjs > 0) {
			return cluster(state,cluster,nextToCheck);
		}
		else {
			return cluster;
		}
	}

	
	public double sampleRejection(Environment state) {
		if(state.burnIn <= state.schedule.getSteps()) {
			//this.initializeRejectBins(state.offerArray);//We need to do this because we are doing sampling
			nRs = 0.0; //reset to zero for next sample
			Bag population = new Bag();
			Bag allGroups = state.sparseSpace.getAllObjects();
			for(int i=0;i<allGroups.numObjs;i++) {
				Group g = (Group)allGroups.objs[i];
				for (int j=0;j<g.members.numObjs;j++)
					population.add(g.members.objs[j]);
			}
			Bag sample = new Bag();
			population.shuffle(state.random);//randomly shuffle population
			int i = 0;
			while(population.numObjs>0 && i < state.rejectionSampleSize) {
				sample.add(population.objs[i]);
				i++;
			}

			Bag proposers = new Bag();
			Bag recipients = new Bag();
			if(!(sample.numObjs >= state.rejectionSampleSize)) {
				System.out.println(sample.numObjs);
				return 0;
			}
			sample.shuffle(state.random);
			for(int j = 0;j< sample.numObjs;j+=2) {
				proposers.add(sample.objs[j]);
				recipients.add(sample.objs[j+1]);
			}
			//System.out.println(proposers.numObjs);
			proposers.shuffle(state.random);
			recipients.shuffle(state.random);

			double reject = 0.0, n = 0.0;
			for(int j =0; j< proposers.numObjs;j++) {
				Agent p = (Agent)proposers.objs[j];
				Agent r =  (Agent)recipients.objs[j];
				if(!(p.offer >= r.accept)) {
					reject++;
					rejects.binData(p.offer);
				}
				rejects.binDataN(p.offer);
				n++;
			}
			double x = reject/n;
			nRs = n;

			return reject/n;
		}
		return 0;
	}
	
	
	public double getRejectionRate() {
		if(nR > 0)
			return reject/nR;
		else
			return 0.0;
	}
	
	public void recordOffspring(Agent a) {
		if(state.burnIn <= state.schedule.getSteps()) {
			fitness.binData(a.offer, a.offspring);

		}
	}

	public void recordOffer(Agent a) {
		if(state.burnIn <= state.schedule.getSteps()) {
			offer+= a.offer;
			nO++;
		}
	}

	public void recordOffers(Agent a) {
		if(state.burnIn <= state.schedule.getSteps())
			offers.binDataF(a.offer);
	}
	
	public void recordAccept(Agent a) {
		if(state.burnIn <= state.schedule.getSteps()) {
			accept+= a.accept;
			nA++;
		}
	}

	public void recordAccepts(Agent a) {
		if(state.burnIn <= state.schedule.getSteps())
			accepts.binDataF(a.accept);
	}

	public void recordDispersion(boolean dispersed) {
		if(dispersed)
			this.dispersion++;
		this.dispersionN++;
	}

	public double meanDispersion() {
		if(dispersionN>0)
			return dispersion/dispersionN;
		else
			return 0.0;
	}


	double variance(double sum, double square, double n){
		double var=0;
		final double mean = sum/n;
		if(n > 3){
			var = (square/n - mean*mean)/(n-1);//sample correction
			if(var > 0)
				return var;
			else
				return 0;
		}
		else{
			return 0;
		}
	}

	public double variancePop(Environment state) {
		double sum = 0;
		double sum2 = 0;
		double n = 0;
		Bag agents = state.sparseSpace.getAllObjects();

		for(int i=0; i< agents.numObjs; i++) {
			Group g = (Group)agents.objs[i];
			for(int j=0;j<g.members.numObjs;j++) {
				Agent a = (Agent)g.members.objs[j];
				if(a.offer > 0) {
					sum += 1.0;
					sum2 += 1.0;//since it is 1, we waste nothing by not squaring
				}
				n++;
			}
		}
		return variance( sum, sum2, n);
	}

	/**
	 * This method saves data to a data matrix for later saving to file if
	 * parameter sweeps are run.
	 * @return
	 */
	public boolean nextInterval() {
		if(!state.paramSweeps)
			return false; //exit if we are not saving data in a sweep
		data.add((double)(state.sparseSpace.getAllObjects().numObjs));//add the number of groups
		data.add(offers.getMean());//add average offer
		data.add(offers.getSD());
		data.add(getMeanOfferAtT(state));
		data.add(accepts.getMean());//add average accept
		data.add(accepts.getSD());
		data.add(getMeanAcceptAtT(state));
		data.add(sampleRejection(state));
		//offer distribution
		double[] offerDis = offers.getFrequencyDis();
		for(int i=0;i<offerDis.length;i++) {
			data.add(offerDis[i]);
		}
		//rejection distribution
		double[] rejectDis = rejects.bin;
		double[] rejectDisN = rejects.binN;
		for(int i=0;i<rejectDis.length;i++) {
			if(rejectDisN[i] > 0)
				data.add(rejectDis[i]/rejectDisN[i]);
			else
				data.add(0.0);
		}
		//accept distribution
		double[] acceptDis = accepts.getFrequencyDis();
		for(int i=0;i<acceptDis.length;i++) {
			data.add(acceptDis[i]);
		}
		
		data.add(meanDispersion() );
		getClusterSizes( state);
		double[] fCluster = clusters.getFrequencyDis();
		double[] meanOffers = this.clusterOffers.getMeanDis();
		double[] meanNumAgents = this.clusterN.getMeanDis();
		data.add(clusters.getMean(fCluster));
		data.add(Math.sqrt(clusters.getVariance(fCluster)));//standard deviation
		for(int i=0;i<fCluster.length;i++) {
			data.add(fCluster[i]);
		}
		for(int i=0;i<meanOffers.length;i++) {
			data.add(meanOffers[i]);
		}
		for(int i=0;i<meanNumAgents.length;i++) {
			data.add(meanNumAgents[i]);
		}
		reSetAtT();//reset for next time

		return true;	
	}

	/**
	 * Calculates mean offer for time series chart.
	 * @param state
	 */
	public void meanOfferAcceptance(Environment state) {
		double offer = 0, accept=0.0;
		Bag agents = state.sparseSpace.getAllObjects();
		int n = 0;
		for(int i=0; i< agents.numObjs; i++) {
			Group g = (Group)agents.objs[i];
			for(int j=0;j<g.members.numObjs;j++) {
				Agent a = (Agent)g.members.objs[j];
				offer += a.currentOffer;
				if(a.accept > 0)
					accept+= a.accept;
				n++;
			}

		}
		offer = offer/(double)n;
		accept = accept/(double)n;
		double reject = sampleRejection(state);
		this.rejectG=0.0;
		this.nRG = 0.0;

		double time = (double)state.schedule.getTime();//get the current time
		this.upDateTimeChart(0,time, offer, true, 1000);//update the chart with up to a 1000 milisecond delay
		this.upDateTimeChart(1,time, accept, true, 1000);//update the chart with up to a 1000 milisecond delay
		this.upDateTimeChart(2,time, reject, true, 1000);//update the chart with up to a 1000 milisecond delay
	}

	public double meanOffer() {
		if(nO>0)
			return offer/nO;
		else
			return 0.0;
	}

	public int countAgents(Environment state){
		int n = 0;
		Bag agents = state.sparseSpace.getAllObjects();
		for(int i=0; i< agents.numObjs; i++) {
			Group g = (Group)agents.objs[i];
			n+=g.members.numObjs;
		}
		return n;
	}

	/**
	 * Plots the fequency distribution of agents for offer levels.
	 * @param state
	 */
	public void offerLevels(Environment state) {
		Bag agents = state.sparseSpace.getAllObjects();
		int n = countAgents(state);
		double[] data = new double[n];
		int k = 0;
		for(int i=0;i<agents.numObjs;i++) {
			Group g = (Group)agents.objs[i];

			for(int j=0;j<g.members.numObjs;j++) {
				Agent a = (Agent)g.members.objs[j];
				data[k] = a.currentOffer;
				k++;
			}

		} 

		this.upDateHistogramChart(0,(int)state.schedule.getSteps(), data, 1000);//give it the data with a 1000 milisecond delay
	}
	
	/**
	 * Plots the fequency distribution of agents for offer levels.
	 * @param state
	 */
	public void acceptLevels(Environment state) {
		Bag agents = state.sparseSpace.getAllObjects();
		int n = countAgents(state);
		double[] data = new double[n];
		int k = 0;
		for(int i=0;i<agents.numObjs;i++) {
			Group g = (Group)agents.objs[i];

			for(int j=0;j<g.members.numObjs;j++) {
				Agent a = (Agent)g.members.objs[j];
				data[k] = a.accept;
				k++;
			}

		} 

		this.upDateHistogramChart(2,(int)state.schedule.getSteps(), data, 1000);//give it the data with a 1000 milisecond delay
	}


	/**
	 * Step method for experimenter, which handles collecting and displaying data.
	 */
	public void step(SimState state) {
		super.step(this.state);
		if(step %this.state.dataSamplingInterval == 0) {//If a sampling interval, record data{
			nextInterval();
			if(this.state.gui.arrayChartTypeXY )//only if true
				meanOfferAcceptance(this.state);
			if(this.state.gui.arrayChartTypeH) {//only if true
				offerLevels(this.state);
				acceptLevels(this.state);
			}
		}
	}
}
