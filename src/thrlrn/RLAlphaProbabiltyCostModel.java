package thrlrn;

import java.util.Random;

public class RLAlphaProbabiltyCostModel implements RLCostModel{

	double adaptCost;
	double compensationCost;
	double actPenalty;
	double maxAlpha;
	double minAlpha;
	
	Random rng;
	
	public RLAlphaProbabiltyCostModel(double adaptCost, double actPenalty, double compensationCost, double maxAlpha, double minAlpha) {
		super();
		this.adaptCost = adaptCost;
		this.actPenalty = actPenalty;
		this.compensationCost = compensationCost;
		this.maxAlpha = maxAlpha;
		this.minAlpha = minAlpha;
		rng = new Random();
	}

	@Override
	public long calculateAdaptationCost(boolean adapted, boolean violation, int caseLength, int stepInCase) {
		double alpha = maxAlpha
				- (((float) (maxAlpha - minAlpha) / (double) (caseLength - 1)) * (stepInCase - 1));
		
		float success = alpha >= rng.nextFloat() ? 1f : 0f;
		
		if(adapted) {
			if(violation) {
				double cost = ((double) actPenalty * (1f - success)) + (double) adaptCost;
				return (long) (cost);
			}else {
				return (long) ((double) adaptCost
						+ (double) compensationCost * (success));
			}
		}else {
			if(violation) {
				return (long) actPenalty;
			}else {
				return 0L;
			}
		}
	}
	
	
}
