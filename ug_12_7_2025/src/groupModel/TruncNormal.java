package groupModel;

import ec.util.MersenneTwisterFast;

public class TruncNormal {
	MersenneTwisterFast random;
	double mean;
	double sd;
	double lower;
	double upper;
	public TruncNormal(MersenneTwisterFast random, double mean, double sd, double lower, double upper) {
		super();
		this.random = random;
		this.mean = mean;
		this.sd = sd;
		this.lower = lower;
		this.upper = upper;
	}

	public double nextTN() {
		double number = mean + random.nextGaussian()* sd;
		while(number < lower || number > upper) {
			number = mean + random.nextGaussian()* sd;
		}
		//System.out.println(number + "  "+mean+ "  "+sd +"  "+lower+" "+upper);
		return number;
	}
	
}
