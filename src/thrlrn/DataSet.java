package thrlrn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import com.opencsv.CSVReader;

public class DataSet {
	
	public double digits;
	
	// used to have the prediction value in Traffic, BPIC12/17 away from zero, as this gives
	// problems when normalizing / and comparing prediction and planned (at least that is my hypothesis!)
	public double offset = 0.0;
	
	// used to have a small addition to the variabce-based reliability estimates, such that they reach 1
	public double epsilon = 0.0;

	
	public int reliabilityType;
	public int combinationType;
	
	public Vector<Case> cases = new Vector<Case>();
	public int processLength;
	
	public double minReliability = 1.0;
	public double maxReliability = 0.0;
	
	boolean TTpatch = false;
	
	public DataSet(double _digits, double _offset, int _reliabilityType, int _combinationType, double _epsilon){
		digits = _digits;
		if(_offset < 0)
			TTpatch = true;
		else
			offset = _offset;
		
		epsilon = _epsilon;
		
		reliabilityType = _reliabilityType;
		combinationType = _combinationType;
	}
	
	// Read from the individual base model files and aggregate
	public void initRNNIndividual(String directory, Integer _processLength) throws Exception {
		processLength = _processLength;
		
		File[] files = new File(directory).listFiles();
		Reader[] myReader = new Reader[105];
		CSVReader[] myCSV = new CSVReader[105];
		
		int i = 0;
		int size = 0;
		
		// open all files
		for (File file : files) {
	        if (file.isDirectory()) {
	        	i++;
               	
	        	if(TTpatch)
	        		myReader[i] = new BufferedReader(new FileReader(new File(file, "0_results.edited.csv")));
	        	else
	        		myReader[i] = new BufferedReader(new FileReader(new File(file, "0-results.edited.csv")));
	        	
	    		myCSV[i] = new CSVReader(myReader[i], ',');
	        }
	    }
		size = i;

		// iterators (only way to do in Java): 
		// https://stackoverflow.com/questions/14917375/cannot-create-generic-array-of-how-to-create-an-array-of-mapstring-obje/14917529)
		Iterator<String[]>[] entries = (Iterator<String[]>[]) new Iterator[105];
		
		
		// skip the header
		for(i = 1; i <= size; i++) {
			 entries[i] = myCSV[i].iterator();
			 entries[i].next(); // skip the header
		}
		
		String[] entry;
		double pred = 0;
		double pred1 = 0;
		double act = 0;
		double plan = 0;
		boolean end = false;
		int pId = 0;
		int checkp = 0;
		
		double maxBAGV = 0;
		
		// cases
		int currentCaseId = -1;
		int runningCaseId = -1;
		Case currentCase = null;
		
		// process steps per case
		int nextProcessStep = 0;
		int currentProcessStep = 0;
		
		int nbrPos = 0;
		int nbrNeg = 0;
		
		double posRel = 0;
		double negRel = 0;
				
		Vector<Double> predV = null; // use for BAGV
		
		int once = 0;
		
		while(entries[1].hasNext())
		{
			pred = 0;
			
			nbrPos = 0;
			nbrNeg = 0;

			predV = new Vector();
			
			// loop over all models
			for(i = 1; i <= size; i++) {
					entry = entries[i].next();
					
					// ignore the rest of the file
					if(entry[0].equals("bucket_level")) {
						end = true;
						break;
					}

					// same for all models, so just do once
					if(i == 1) {
						plan = Double.parseDouble(entry[6]) + offset;
						act = Double.parseDouble(entry[5]) + offset;
						pId = Integer.parseInt(entry[0]);
						checkp = Integer.parseInt(entry[2]);
					}

					pred1 = Double.parseDouble(entry[4]) + offset;
					predV.add(pred1);
					pred = pred + pred1;
					
					if(pred1 > plan) {
						nbrPos++;
					} else {
						nbrNeg++;
					}
			}
			
			if(!end) {
			
				if(combinationType == 2) {
					double median = 0;
					Collections.sort(predV);
					int len = predV.size();
					if(len % 2 == 1)
						median = predV.elementAt(len/2+1);
					else
						median = (predV.elementAt(len/2) + predV.elementAt(len/2+1))/2.0;
					
					pred = median;
				}
				
				if(combinationType == 1) {
					pred = pred / (double)size;
				}
				
				// BAGV: loop again to compute
				double bagv = 0;
				double predX = 0;
				Iterator<Double> predI = predV.iterator();
	
				while(predI.hasNext()) {
					predX = predI.next();
					bagv += Math.pow( predX - pred, 2);
				}
				once = 1;
				
				double bagv1 = (bagv / (double)size);
				double bagv2 = Math.sqrt(bagv1);
				bagv = bagv2 / Math.abs(pred);

				// assign results to Case
				runningCaseId = pId; 
				if(runningCaseId != currentCaseId){
					currentCaseId = runningCaseId;	
												
					currentCase = new Case();
					cases.add(currentCase);
					currentCase.caseId = currentCaseId;
								
					// Initialize (to identify the checkpoints that have no data -- different process lengths!)
					for(int j = 1; j <= _processLength; j++) {
						currentCase.reliability[j] = -1;
						currentCase.predictedDuration[j] = -1;
					}
												
					currentCase.actualDuration = act;
					currentCase.plannedDuration = plan;
				}
	
				currentProcessStep = checkp;
						
				// stop at max. process length (the others have basically no data)
				if(currentProcessStep > processLength)
					continue;
				
//				// TODO: Traffic
//				if(currentProcessStep == 3)
//					continue;
//				if(currentProcessStep > 3)
//					currentProcessStep--;
				
				
				// remember the largest checkpoint (= caseLength)
				currentCase.caseLength = currentProcessStep;
				
				// MajCount / Binary Prediction
				posRel = (double)nbrPos/(double)size;
				negRel = (double)nbrNeg/(double)size;
				
				if(posRel > negRel)
					currentCase.binaryPrediction[currentProcessStep] = true;
				else
					currentCase.binaryPrediction[currentProcessStep] = false;

				// MajCount
				if(reliabilityType == 1) {	
					if(posRel > negRel)
						currentCase.reliability[currentProcessStep] = posRel/digits;
					else
						currentCase.reliability[currentProcessStep] = negRel/digits;
					
					if(currentCase.reliability[currentProcessStep] < minReliability)
						minReliability = currentCase.reliability[currentProcessStep];
					
					if(currentCase.reliability[currentProcessStep] > maxReliability)
						maxReliability = currentCase.reliability[currentProcessStep];
					
				}

				// BAGV
				// for the risk-based approach, also store separately
				// map to interval 0.5 -- 1 in order to be compatible with MajCount
				currentCase.numericReliability[currentProcessStep] = (epsilon+(0.5+(Math.exp(-bagv)/2.0)))/digits;
				if(reliabilityType == 2) {				
					currentCase.reliability[currentProcessStep] = currentCase.numericReliability[currentProcessStep];
				}

				// BAGV (Philipp)
				if(reliabilityType == 3) {				
					currentCase.reliability[currentProcessStep] = (1-(bagv/0.5193284961466699f))/digits;
					
//					if(bagv > maxBAGV) maxBAGV = bagv;
				}
				
				// Raw variance, completely without normalisation
				if(reliabilityType == 4) {
					// Reverse the last operation on bagv, to again have the standard deviation
					currentCase.reliability[currentProcessStep] = bagv*Math.abs(pred);
				}
				
				if(TTpatch)
					currentCase.predictedDuration[currentProcessStep] = pred - 70.0; 
					// 80.0 would lead to optimal MCC, but then this tweaks the correlation with reliability
				else
					currentCase.predictedDuration[currentProcessStep] = pred;
				
				if(currentCase.reliability[currentProcessStep] < minReliability)
					minReliability = currentCase.reliability[currentProcessStep];
				
				if(currentCase.reliability[currentProcessStep] > maxReliability)
					maxReliability = currentCase.reliability[currentProcessStep];

			} else {
				break;
			}
			
		}
//		System.out.println("MAXBAGV: "+maxBAGV);
		for(i = 1; i <= size; i++) {
			myCSV[i].close();
		}
		
	}

	
	public void initRNNSingle(CSVReader myCSV, Integer _processLength){
		processLength = _processLength;
		
		Iterator<String[]> entries = myCSV.iterator();
		
		String[] entry = null;
		entries.next(); // skip the header

		// cases
		int currentCaseId = -1;
		int runningCaseId = -1;
		Case currentCase = null;
		
		// process steps per case
		int nextProcessStep = 0;
		int currentProcessStep = 0;

		
		while(entries.hasNext()){
			entry = entries.next();
			
			// ignore the rest of the file
			if(entry[0].equals("bucket_level")) {
				break;
			}
			
			// case by case
			runningCaseId = Integer.parseInt(entry[0]); 
			if(runningCaseId != currentCaseId){
				currentCaseId = runningCaseId;	
								
				currentCase = new Case();
				cases.add(currentCase);
				currentCase.caseId = currentCaseId;
				
				// Take the *first* process step in case of loops
				for(int i = 1; i <= _processLength; i++) {
					currentCase.reliability[i] = -1;
					currentCase.predictedDuration[i] = -1;
				}
								
				double actualDuration = Double.parseDouble(entry[5].replace(',','.'));
				currentCase.actualDuration = actualDuration;
				
				double plannedDuration = Double.parseDouble(entry[6].replace(',','.'));
				currentCase.plannedDuration = plannedDuration;
			}
			
			/*
			// use the prefix length as the id for the process step, thereby being more consistent with the other
			// data sets
			*/
			currentProcessStep = Integer.parseInt(entry[2]);
			
			// stop at max. process length (the others have basically no data)
			if(currentProcessStep > processLength)
				continue;
			
			/*
			// process step by process step
			String[] s = entry[10].split(" ");
			// current process step is the last entry in the sequence of the prefix
			currentProcessStep = Integer.parseInt(s[s.length-1]);
*/
			
			currentCase.reliability[currentProcessStep] = 0.5f/digits;

			double predictedDuration = Double.parseDouble(entry[4].replace(',','.'));
			
			if(TTpatch)
				currentCase.predictedDuration[currentProcessStep] = predictedDuration - 70.0; 
				// 80.0 would lead to optimal MCC, but then this tweaks the correlation with reliability
			else
				currentCase.predictedDuration[currentProcessStep] = predictedDuration;
			
		}
	}
	
		
	public void init_QRF(CSVReader myCSV, Integer _processLength){
		processLength = _processLength;
		
		Iterator<String[]> entries = myCSV.iterator();
		
		boolean NA = false;
		boolean entry1 = false;
		
		String[] entry = null;
		entries.next(); // skip the header

		// cases
		int currentCaseId = 0;
		int runningCaseId = 0;
		Case currentCase = null;
		
		// process steps per case
		int nextProcessStep = 0;
		int currentProcessStep = 0;
		
		String s;
		double reliability;
		double predictedDuration;
		double actualDuration;
		
		while(entries.hasNext()){
			entry = entries.next();
			
			// case by case
			s = entry[0].replaceAll("\\s+","");
			if(s.isEmpty()) continue;
			
			
			runningCaseId = Integer.parseInt(s); 
			if(runningCaseId != currentCaseId){
				currentCaseId = runningCaseId;	
								
				currentCase = new Case();
				cases.add(currentCase);
				currentCase.caseId = currentCaseId;
				
				// Take the *first* process step in case of loops
				for(int i = 0; i <= _processLength; i++) {
					currentCase.reliability[i] = -1;
					currentCase.predictedDuration[i] = -1;
				}
								
				s = entry[2].replaceAll("\\s+","");
				actualDuration = Double.parseDouble(s.replace(',','.'));
				currentCase.actualDuration = actualDuration;
				
				//double plannedDuration = Double.parseDouble(entry[1].replace(',','.'));
				currentCase.plannedDuration = 1.5;
			}
			
			s = entry[1].replaceAll("\\s+","");
			// TODO: Somehow QRF/RF results include twice the earliest checkpoint
			currentProcessStep = Integer.parseInt(s)-1;
			
			// remember the largest checkpoint (= caseLength)
			currentCase.caseLength = currentProcessStep;
			
			// Take the *first* process step in case of loops
			if(currentCase.reliability[currentProcessStep] == -1) {
				s = entry[3].replaceAll("\\s+","");
				
				predictedDuration = Double.parseDouble(s.replace(',','.'));
				currentCase.predictedDuration[currentProcessStep] = predictedDuration;
							
				reliability = 0;
				s = entry[4].replaceAll("\\s+","");
				reliability = Double.parseDouble(s.replace(',','.'));

				currentCase.reliability[currentProcessStep] = (epsilon+ 
						(0.5+(Math.exp(-(reliability/Math.abs(predictedDuration))))/2.0))/digits;
			}			
		}
	}

	
	
	public void init_QRF_C2K(CSVReader myCSV, Integer _processLength){
		processLength = _processLength;
		
		Iterator<String[]> entries = myCSV.iterator();
		
		boolean NA = false;
		
		String[] entry = null;
		entries.next(); // skip the header

		// cases
		int currentCaseId = 0;
		int runningCaseId = 0;
		Case currentCase = null;
		
		// process steps per case
		int nextProcessStep = 0;
		int currentProcessStep = 0;
		
		String s;
		double reliability;
		double predictedDuration;
		double actualDuration;
		double plannedDuration;
		
		while(entries.hasNext()){
			entry = entries.next();
			
			// case by case
			s = entry[0].replaceAll("\\s+","");
			if(s.isEmpty()) continue;
			
			
			runningCaseId = Integer.parseInt(s); 
			if(runningCaseId != currentCaseId){
				currentCaseId = runningCaseId;	
								
				currentCase = new Case();
				cases.add(currentCase);
				currentCase.caseId = currentCaseId;
				
				// Take the *first* process step in case of loops
				for(int i = 1; i <= _processLength; i++) {
					currentCase.reliability[i] = -1;
					currentCase.predictedDuration[i] = -1;
				}
								
				s = entry[3].replaceAll("\\s+","");
				actualDuration = Double.parseDouble(s.replace(',','.'));
				currentCase.actualDuration = actualDuration;

				s = entry[2].replaceAll("\\s+","");
				plannedDuration = Double.parseDouble(s.replace(',','.'));
				currentCase.plannedDuration = plannedDuration;
			}
			
			s = entry[1].replaceAll("\\s+","");
			// TODO: Somehow QRF/RF results include twice the earliest checkpoint
			currentProcessStep = Integer.parseInt(s) - 1;
			
			// remember the largest checkpoint (= caseLength)
			currentCase.caseLength = currentProcessStep;
			
			// Take the *first* process step in case of loops
			if(currentCase.reliability[currentProcessStep] == -1) {
				s = entry[4].replaceAll("\\s+","");
				
				// PATCH due to not a number predictions of QRF (for BPIC17)
				predictedDuration = 1;
				if(s.equals("NA")) {
					if(NA == false) {
						System.out.println("WARNING: NA values in the dataset!!!");
						NA = true;
					}
					NA = true;
					currentCase.predictedDuration[currentProcessStep] = 1;
				} else {
					predictedDuration = Double.parseDouble(s.replace(',','.'));
					currentCase.predictedDuration[currentProcessStep] = predictedDuration;
				}
			
				reliability = 0;
				if(s.equals("NA")) {
					currentCase.reliability[currentProcessStep] = 0;
				} else {
					s = entry[5].replaceAll("\\s+","");
					reliability = Double.parseDouble(s.replace(',','.'));
					// Add epsilon: 0.05
					currentCase.reliability[currentProcessStep] = (epsilon+ 
							(0.5+(Math.exp(-(reliability/Math.abs(predictedDuration))))/2.0))/digits;
				}
			}			
		}
	}

	
	public void init_RF(CSVReader myCSV, Integer _processLength){
		processLength = _processLength;
		
		Iterator<String[]> entries = myCSV.iterator();
		
		boolean NA = false;
		boolean entry1 = false;
		
		String[] entry = null;
		entries.next(); // skip the header

		// cases
		int currentCaseId = 0;
		int runningCaseId = 0;
		Case currentCase = null;
		
		// process steps per case
		int nextProcessStep = 0;
		int currentProcessStep = 0;
		
		String s;
		double reliability;
		double predictedDuration;
		double actualDuration;
		
		while(entries.hasNext()){
			entry = entries.next();
			
			// case by case
			s = entry[0].replaceAll("\\s+","");
			if(s.isEmpty()) continue;
			
			
			runningCaseId = Integer.parseInt(s); 
			if(runningCaseId != currentCaseId){
				currentCaseId = runningCaseId;	
								
				currentCase = new Case();
				cases.add(currentCase);
				currentCase.caseId = currentCaseId;
				
				// Take the *first* process step in case of loops
				for(int i = 0; i <= _processLength; i++) {
					currentCase.reliability[i] = -1;
					currentCase.predictedDuration[i] = -1;
				}
								
				s = entry[2].replaceAll("\\s+","");
				actualDuration = Double.parseDouble(s.replace(',','.'));
				currentCase.actualDuration = actualDuration;
				
				//double plannedDuration = Double.parseDouble(entry[1].replace(',','.'));
				currentCase.plannedDuration = 1.5;
			}
			
			s = entry[1].replaceAll("\\s+","");
			// TODO: Somehow QRF/RF results include twice the earliest checkpoint
			currentProcessStep = Integer.parseInt(s)-1;
			
			// remember the largest checkpoint (= caseLength)
			currentCase.caseLength = currentProcessStep;
			
			// Take the *first* process step in case of loops
			if(currentCase.reliability[currentProcessStep] == -1) {
				s = entry[3].replaceAll("\\s+","");
				
				predictedDuration = Double.parseDouble(s.replace(',','.'));
				currentCase.predictedDuration[currentProcessStep] = predictedDuration;
							
				reliability = 0;
				s = entry[4].replaceAll("\\s+","");
				reliability = Double.parseDouble(s.replace(',','.'));

				currentCase.reliability[currentProcessStep] = (0.5+(reliability/2.0))/digits;
			}			
		}
	}
	
	
	public void init_RF_C2K(CSVReader myCSV, Integer _processLength){
		processLength = _processLength;
		
		Iterator<String[]> entries = myCSV.iterator();
		
		boolean NA = false;
		
		String[] entry = null;
		entries.next(); // skip the header

		// cases
		int currentCaseId = 0;
		int runningCaseId = 0;
		Case currentCase = null;
		
		// process steps per case
		int nextProcessStep = 0;
		int currentProcessStep = 0;
		
		String s;
		double reliability;
		double predictedDuration;
		double actualDuration;
		double plannedDuration;
		
		while(entries.hasNext()){
			entry = entries.next();
			
			// case by case
			s = entry[0].replaceAll("\\s+","");
			if(s.isEmpty()) continue;
			
			
			runningCaseId = Integer.parseInt(s); 
			if(runningCaseId != currentCaseId){
				currentCaseId = runningCaseId;	
								
				currentCase = new Case();
				cases.add(currentCase);
				currentCase.caseId = currentCaseId;
				
				// Take the *first* process step in case of loops
				for(int i = 1; i <= _processLength; i++) {
					currentCase.reliability[i] = -1;
					currentCase.predictedDuration[i] = -1;
				}
								
				s = entry[3].replaceAll("\\s+","");
				actualDuration = Double.parseDouble(s.replace(',','.'));
				currentCase.actualDuration = actualDuration;

				s = entry[2].replaceAll("\\s+","");
				plannedDuration = Double.parseDouble(s.replace(',','.'));
				currentCase.plannedDuration = 1.5f;
			}
			
			s = entry[1].replaceAll("\\s+","");
			// TODO: Somehow QRF/RF results include twice the earliest checkpoint
			currentProcessStep = Integer.parseInt(s)-1;
			
			// remember the largest checkpoint (= caseLength)
			currentCase.caseLength = currentProcessStep;
			
			// Take the *first* process step in case of loops
			if(currentCase.reliability[currentProcessStep] == -1) {
				s = entry[4].replaceAll("\\s+","");
				
				// PATCH due to not a number predictions of QRF (for BPIC17)
				predictedDuration = 1;
				if(s.equals("NA")) {
					if(NA == false) {
						System.out.println("WARNING: NA values in the dataset!!!");
						NA = true;
					}
					NA = true;
					currentCase.predictedDuration[currentProcessStep] = 1;
				} else {
					predictedDuration = Double.parseDouble(s.replace(',','.'));
					currentCase.predictedDuration[currentProcessStep] = predictedDuration;
				}
			
				reliability = 0;
				if(s.equals("NA")) {
					currentCase.reliability[currentProcessStep] = 0;
				} else {
					s = entry[5].replaceAll("\\s+","");
					reliability = Double.parseDouble(s.replace(',','.'));
					// Add epsilon: 0.05
					currentCase.reliability[currentProcessStep] = (0.1+ 
							(0.5+(Math.exp(-(reliability/Math.abs(predictedDuration))))/2.0))/digits;
				}
			}			
		}
	}
	
	

}

