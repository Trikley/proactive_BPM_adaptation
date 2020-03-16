package thrlrn;

import java.util.Iterator;

public class CostModel {

	int combFunction = 0;
	
	public CostModel(int _combFunction) {
		combFunction = _combFunction;
	}
	
	public double compCost(double delta) {

			if(delta <= 0)
				return 0;
			else
				return 1.0; 

	}
	
	public long computeStaticCosts(DataSet ds, int checkpoint, double alpha, double lambda, double thr, long penalty, int predictionType, boolean costBased){

		long cost = 0;
		
		Case myCase = null;
		Iterator<Case> cases = ds.cases.iterator();

		boolean pred = false;
		

		double delta;
		double actDelta;
		
		
		double actPenalty;
		double adaptCost;
		
		while(cases.hasNext()){
			myCase = cases.next();

			if(myCase.reliability[checkpoint] != -1) {
			
				if(predictionType == 1)
					pred = myCase.binaryPrediction[checkpoint];
				
				if(predictionType == 2)
					pred = (myCase.predictedDuration[checkpoint] > myCase.plannedDuration);
				
				delta = (myCase.predictedDuration[checkpoint]-myCase.plannedDuration) / myCase.plannedDuration;
				actDelta = (myCase.actualDuration-myCase.plannedDuration) / myCase.plannedDuration;
				
				actPenalty = compCost(actDelta)*penalty;
	
				adaptCost = penalty*lambda;
				
				if(pred == true) {				
					// Bei statischem Checkpoint sollte man eigentlich immer adaptieren; unabhängig von Threshold!
					cost += (long)(((double)actPenalty*(1-alpha)) + (double)adaptCost);
									
				} else {
					if(myCase.actualDuration > myCase.plannedDuration)
							cost += actPenalty;
				}
			}			
		}
		
		return cost;	
	}
	
	
	public long computeDynamicCosts(DataSet ds, double lambda, double thr, long penalty, 
			double minAlpha, double maxAlpha, int predictionType, boolean thresBased){

		long cost = 0;
		
		boolean pred = false;
		
		Case myCase = null;
		Iterator<Case> cases = ds.cases.iterator();
		double alpha;
		
		double delta;
		double actDelta;
		
		double risk; 
		
		double actPenalty;
		double adaptCost;

		boolean adapted;
		
		while(cases.hasNext()){
			myCase = cases.next();
			
			adapted = false;
			
			for(int i = 1; i < myCase.caseLength; i++) {
				alpha = maxAlpha-((float)(maxAlpha-minAlpha)/(double)(myCase.caseLength-1) * (i-1));
			
				if(predictionType == 1)
					pred = myCase.binaryPrediction[i];
				
				if(predictionType == 2)
					pred = (myCase.predictedDuration[i] > myCase.plannedDuration);

				actDelta = (myCase.actualDuration-myCase.plannedDuration) / myCase.plannedDuration;
				
				actPenalty = penalty;
				adaptCost  = penalty*lambda;
				
				if(thresBased) {
					// consider threshold
					if(!adapted) {
						// ADAPT
						boolean above = false;
						if(combFunction == 1)
							above = myCase.reliability[i] >= thr;
							
						if(combFunction == 2)
							above = myCase.reliability[i] >= thr && myCase.numericReliability[i] >= thr;
							
						if( pred == true  && above) 
						{	
							// if true positive:
							if(myCase.actualDuration > myCase.plannedDuration)
								cost += (long)(((double)actPenalty*(1-alpha)) + (double)adaptCost);
							// if false positive: also consider compensation costs (say, same as adaptCost)
							if(myCase.actualDuration <= myCase.plannedDuration) {
								cost += (long)((double)adaptCost*(alpha) + (double)actPenalty*(1-alpha) + 
										+ (double)adaptCost);
							}						
							
							adapted = true;
						}
					}
				}
			}
			
			// compute the costs at the final checkpoint when no adaptation has been made thus far
			actDelta = (myCase.actualDuration-myCase.plannedDuration) / myCase.plannedDuration;
			actPenalty = penalty;
			
			if(adapted == false) {
				if(myCase.actualDuration > myCase.plannedDuration) {
					cost += actPenalty;
				}
			}
			
		}	
		return cost;	
	}
}
