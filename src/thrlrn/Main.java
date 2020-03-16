package thrlrn;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.opencsv.CSVReader;

public class Main {

	public static boolean shuffle;
	public static boolean filter;
	public static boolean verbose;
	public static boolean exclude;
	public static RLCostModel costModel;

	public static void main(String[] args) throws Exception {
		List<Integer> processesMainParameters = new LinkedList<Integer>();
		int reliabiltyTypeMainParameter = 1;
		int datasetMainParameter = 1;
		shuffle = false;
		filter = false;
		verbose = false;
		exclude = false;
		costModel = new RLAlphaCostModel(Constants.ADAPT_COST, Constants.ACT_PENALTY, Constants.COMPENSATION_COST, Constants.MAX_ALPHA, Constants.MIN_ALPHA);
		for (String argRaw : args) {
			try {
				String arg = argRaw.trim().replace("-", "").replace("/", "").replace("\\", "").toLowerCase();
				if (arg.equals("s")) {
					shuffle = true;
				} else if (arg.equals("shuffle")) {
					shuffle = true;
				} else if (arg.equals("v")) {
					verbose = true;
				} else if (arg.equals("verbose")) {
					verbose = true;
				} else if (arg.equals("f")) {
					filter = true;
				} else if (arg.equals("filter")) {
					filter = true;
				} else if (arg.equals("exclude")) {
					exclude = true;
				} else if (arg.equals("e")) {
					exclude = true;
				} else if (arg.equals("rnn")) {
					datasetMainParameter = 1;
				} else if (arg.equals("qrf")) {
					datasetMainParameter = 2;
				} else if (arg.equals("mc")) {
					reliabiltyTypeMainParameter = 1;
				} else if (arg.equals("majcount")) {
					reliabiltyTypeMainParameter = 1;
				} else if (arg.equals("majoritycount")) {
					reliabiltyTypeMainParameter = 1;
				} else if (arg.equals("var")) {
					reliabiltyTypeMainParameter = 4;
				} else if (arg.equals("variance")) {
					reliabiltyTypeMainParameter = 4;
				} else if (arg.equals("bagv")) {
					reliabiltyTypeMainParameter = 2;
				} else if (arg.equals("bagvp")) {
					reliabiltyTypeMainParameter = 3;
				} else if (arg.equals("naive")) {
					costModel = new RLNaiveCostModel(Constants.ADAPT_COST, Constants.ACT_PENALTY,
							Constants.COMPENSATION_COST);
				} else if (arg.equals("alpha")) {
					costModel = new RLAlphaCostModel(Constants.ADAPT_COST, Constants.ACT_PENALTY,
							Constants.COMPENSATION_COST, Constants.MAX_ALPHA, Constants.MIN_ALPHA);
				} else if (arg.equals("probabilty")) {
					costModel = new RLAlphaProbabiltyCostModel(Constants.ADAPT_COST, Constants.ACT_PENALTY,
							Constants.COMPENSATION_COST, Constants.MAX_ALPHA, Constants.MIN_ALPHA);
				} else if (arg.startsWith("{") && arg.endsWith("}")) {
					arg = arg.replace("}", "").replace("{", "");
					String[] splitted = arg.split(",");
					for (int i = 0; i < splitted.length; i++) {
						processesMainParameters.add(Integer.parseInt(splitted[i].trim()));
					}
				} else {
					processesMainParameters.add(Integer.parseInt(arg));
				}
			} catch (NumberFormatException e) {
				System.err.println("Command line argument " + argRaw
						+ " is unknown! Only use \"s\", \"bagvp\", \"bagv\" \"mc\", \"f\", \"naive\", \"alpha\", an integer number or a list of integer numbers (\"{1,2,3}\") to modify the corresponding default behaviour!");
			}
		}
		
		if(processesMainParameters.size()==0) {
			for(int i=1;i<=4;i++) {
				processesMainParameters.add(i); 
			}
		} 
		
		
		for(int i=0; i<processesMainParameters.size(); i++) {
			System.out.println("Chosen Processes " + (i+1) + ": " + processesMainParameters.get(i));
		}
		System.out.println("Chosen Reliability Type: " + reliabiltyTypeMainParameter);
		System.out.println("Chosen Dataset: " + datasetMainParameter);
		System.out.println("Shuffle: " + shuffle);
		System.out.println("Filter: " + filter);
		System.out.println("Cost Model: " + costModel.getClass().getSimpleName());
		
		DataSetManager dsManager = new DataSetManager();
		for(int i=0; i<processesMainParameters.size(); i++) {
			dsManager.load(datasetMainParameter, processesMainParameters.get(i), reliabiltyTypeMainParameter);
		}

		int predictionType = 2; // 1 = Binary; 2 = Numeric
		Experiment exp = new Experiment(predictionType, false);

		exp.ExperimentG(dsManager);

		Toolkit.getDefaultToolkit().beep(); // Standard-Warnsignal muss bei Sound (Systemsteuerung) aktiviert sein

	}

}
