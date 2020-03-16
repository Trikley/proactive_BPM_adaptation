package thrlrn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.opencsv.CSVReader;

public class DataSetManager {

	Queue<DataSet> dataSetsQueue;
	Map<Integer, DataSet> dataSetsMap;
	
	
	public DataSetManager() {
		super();
		this.dataSetsQueue = new LinkedList<DataSet>();
		this.dataSetsMap = new HashMap<Integer, DataSet>();
	}

	//int process // in reference to array below
	//int dataset 1 = RNN; 2 = QRF; 3 = RF; 4 = RNN Individuell
	//int reliabilityType // 1 = MajCount (und QRF-Variance); 2 = BAGV; 3 = BAGV
		// (normiert via min/max = Philipp)
	public void load(int dataset, int process, int reliabilityType) {
		if(dataSetsMap.containsKey(calculateParameterHash(dataset, process, reliabilityType))) {
			dataSetsQueue.add(dataSetsMap.get(calculateParameterHash(dataset, process, reliabilityType)));
			return;
		}
		if(dataset == -1) {
			DataSet ds = DummyData.generateDummyTrainingDataSet(50000, 5, 100);

			dataSetsQueue.add(ds);
			dataSetsMap.put(calculateParameterHash(dataset, process, reliabilityType), ds);
			return;
		}
		
		String inputFileName = "";
		int checkpoints = 0;
		String methodName = "";
		String label = "";
		int promille = 0;

		int aggregate = 1; // 1 = use aggregated files, 2 = read individually and aggregate in code, 3 =
							// treat files separately
		double offset = 0; // used to move the 0/1 predictions (in Traffic, BPIC) away from the zero, as
							// this makes problems for BAGV (which is normailized via pred)
		double epsilon = 0; // used to shift variance based estimates so they reach 1
		Reader myReader;
		CSVReader myCSV;

		try {

			// ExperimentG(Experiment exp, DataSet ds, String filename,
			// int corr, int acc, boolean rel, boolean pdf, boolean sav, boolean sit, ...)
			// throws Exception {
			// corr:
			// 1: X = reliability; Y = MCC (for all predictions with reliability X)
			// 2: X = reliability; Y = RAE - Relative Absolute Error (for all predictions
			// with reliability X)
			// 3: X = reliability; Y = error (absolute error);
			// acc: 1: MCC per checkpoint
			// 2: RAE per checkpoint
			// 10: MCC total
			// 20: RAE total

			double digits = 1f; // DEPCRECATED: set to 10f for correlation; and 1f for savings

			int corr = 0; // 1 = MCC (+ F-Metric), 2 = RAE
			int acc = 0; // 10 = MCC, 20 = RAE; pro Checkpoint 1 = MCC; 2 = RAE
			int sav = 2; // 1 = compute savings matrix; 2 = REINFORCEMENT LEARNING
			boolean sit = false; // compute situations matrix

			//int reliabilityType // 1 = MajCount (und QRF-Variance); 2 = BAGV; 3 = BAGV
																// (normiert via min/max = Philipp)
			int combinationType = 1; // 1 = Mean; 2 = Median

			int combFunction = 1; // 1 = individual reliability (MajCt or BAGV); 2 = combined reliability

			Object[][] config = {

					{ (dataset / 1) * (process / 1), "Cargo2000-all", 21 - 1, "models-c2k", "NN", 18, 2, 0.0, 0.03 }, // #1313

					{ (dataset / 1) * (process / 2), "Traffic-all", 6 - 1, "models-traffic", "NN", 141, 2, 0.5, 0.01 }, // #50117

					{ (dataset / 1) * (process / 3), "BPIC2012-all", 49 - 1, "models-bpic", "NN", 58, 2, 0.5, 0.01 }, // #4361

					{ (dataset / 1) * (process / 4), "BPIC2017-all", 73 - 1, "models-bpic17", "NN", 232, 2, 0.5, 0.02 }, // #10500

					{ (dataset / 1) * (process / 5), "TransformingTransport-all", 274 - 1, "models-tt", "NN", 15, 3,
							-1.0, 0.06 }, // #90
			};

			for (int i = 0; i < config.length; i++) {
				if (((Integer) config[i][0]) == 1) {
					label = (String) config[i][1];
					checkpoints = (Integer) config[i][2];
					inputFileName = (String) config[i][3];
					methodName = (String) config[i][4];
					promille = (Integer) config[i][5];
					aggregate = (Integer) config[i][6];
					offset = (Double) config[i][7];
					epsilon = (Double) config[i][8];
				}
			}

			CostModel cm = new CostModel(combFunction);

			// files with ensemble predictions
			if (aggregate == 1) {
				System.out.println(inputFileName);

				myReader = new BufferedReader(new FileReader(inputFileName));
				myCSV = new CSVReader(myReader, ';'); // use ; as separator due to German style CSV
				DataSet ds = new DataSet(digits, offset, reliabilityType, combinationType, epsilon);

				// call method via reflection
				Integer integ = 1;
				Class[] ar = { myCSV.getClass(), integ.getClass() };
				Method method = ds.getClass().getMethod(methodName, ar);
				method.invoke(ds, myCSV, checkpoints);
				myCSV.close();

				dataSetsQueue.add(ds);
				dataSetsMap.put(calculateParameterHash(dataset, process, reliabilityType), ds);
			}
			// compute ensemble predictions from base models
			if (aggregate == 2) {
				System.out.println(inputFileName);

				DataSet ds = new DataSet(digits, offset, reliabilityType, combinationType, epsilon);
				ds.initRNNIndividual(inputFileName, checkpoints);

				dataSetsQueue.add(ds);
				dataSetsMap.put(calculateParameterHash(dataset, process, reliabilityType), ds);
			}
			// check for individual base models (e.g., accuracy)
			if (aggregate == 3) {
				File[] files = new File(inputFileName).listFiles();

				// iterate over all files
				for (File file : files) {
					if (file.isDirectory()) {
						System.out.print("***** " + file.getName() + "\t");

						if (offset == -1) // TTPatch
							myReader = new BufferedReader(new FileReader(new File(file, "0_results.edited.csv")));
						else
							myReader = new BufferedReader(new FileReader(new File(file, "0-results.edited.csv")));
						myCSV = new CSVReader(myReader, ',');

						DataSet ds = new DataSet(digits, offset, reliabilityType, combinationType, epsilon);
						ds.initRNNSingle(myCSV, checkpoints);
						
						dataSetsQueue.add(ds);
						dataSetsMap.put(calculateParameterHash(dataset, process, reliabilityType), ds);
					}

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public DataSet nextDataset() {
		DataSet result = dataSetsQueue.poll();
		dataSetsQueue.add(result);
		return result;
	}
	
	private int calculateParameterHash(int... ps) {
		StringBuilder result = new StringBuilder();
		for(int i=0; i<ps.length; i++) {
			result.append(ps[i]);
		}
		return Integer.parseInt(result.toString());
	}
}
