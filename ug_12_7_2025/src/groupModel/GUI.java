/*
Copyright 2020 by Jeffrey C. Schank
Licensed under the Academic Free License version 3.0
See https://opensource.org/licenses/AFL-3.0 for more information
 */
package groupModel;
/**
 * Graphical user interface for simulations.  It requires MASON (19) and MASONplus7.
 */
import java.awt.Color;
import sweep.GUIStateSweep;
import sweep.SimStateSweep;

public class GUI extends GUIStateSweep {

	/**
	 * Constructor
	 * @param state
	 * @param gridWidth
	 * @param gridHeight
	 * @param backdrop
	 * @param agentDefaultColor
	 * @param agentPortrayal
	 */
	public GUI(SimStateSweep state, int gridWidth, int gridHeight, Color backdrop, Color agentDefaultColor,
			boolean agentPortrayal) {
		super(state, gridWidth, gridHeight, backdrop, agentDefaultColor, agentPortrayal);
	}

	static boolean keyExists(String key, String[] args)
	{
		for(int x=0;x<args.length;x++)
			if (args[x].equalsIgnoreCase(key))
				return true;
		return false;
	}

	static String argumentForKey(String key, String[] args)
	{
		for(int x=0;x<args.length-1;x++) {  // if a key has an argument, it can't be the last string
			if (args[x].equalsIgnoreCase(key)) {
				return args[x + 1];
			}
		}
		return null;
	}

	public static void main(String[] args) {
		final boolean noCharts = keyExists("-nocharts", args);//check to see if nocharts: used for parameter sweeps
		if(!noCharts) {
			//default charts
			String[] title = {"Offers", "Acceptance Threshold", "Rejection Rate"};
			String[] x = {"Steps", "Steps", "Steps"};
			String[] y = {"Mean Offer","Mean Acceptance","Frequency"};
			GUI.initializeArrayTimeSeriesChart(3, title, x, y);
			//GUI.initializeTimeSeriesChart( "Offers", "Steps", "Mean Offer");
			
			String[] title2 = {"Offer Distribution", "Strategies","Acceptance Distribution"};//A string array, where every entry is the title of a chart
			String[] x2 = {"Offer Levels","Strategy", "Accept Levels"};//A string array, where every entry is the x-axis title
			String[] y2 = {"Number of Agents","Number of Agents","Number of Agents"};//A string array, where every entry is the y-axis title
			int[] bins = {13,13,13,13};
			GUI.initializeArrayHistogramChart(3, title2, x2, y2, bins /*new int[10]*/);
			//GUI.initializeHistogramChart( "Offers", "Offer Levels", "# Agents",13);
		}

		final String runTimeFileName = argumentForKey("-runfile",args);//for non-default runtime files
		if(runTimeFileName==null) {
			GUI.initialize(Environment.class, Experimenter.class, GUI.class, 400, 400, Color.WHITE, Color.RED, false, spaces.SPARSE);
		}//the static initialize method creates instances of the Environment, Experimenter, and GUI classes using Java reflection
		else {
			GUI.initialize(Environment.class, Experimenter.class, GUI.class, 400, 400, Color.WHITE, Color.RED, false, spaces.SPARSE,runTimeFileName);
		}
	}
}
