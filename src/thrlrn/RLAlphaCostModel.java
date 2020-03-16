package thrlrn;

import java.util.Random;

public class RLAlphaCostModel implements RLCostModel{

	double adaptCost;
	double compensationCost;
	double actPenalty;
	double maxAlpha;
	double minAlpha;
	
	public RLAlphaCostModel(double adaptCost, double actPenalty, double compensationCost, double maxAlpha, double minAlpha) {
		super();
		this.adaptCost = adaptCost;
		this.actPenalty = actPenalty;
		this.compensationCost = compensationCost;
		this.maxAlpha = maxAlpha;
		this.minAlpha = minAlpha;
	}

	@Override
	public long calculateAdaptationCost(boolean adapted, boolean violation, int caseLength, int stepInCase) {
		double alpha = maxAlpha
				- (((float) (maxAlpha - minAlpha) / (double) (caseLength - 1)) * (stepInCase - 1));
		
		if(adapted) {
			if(violation) {
				return (long) (((double) actPenalty * (1f - alpha)) + (double) adaptCost);
			}else {
				return (long) ((double) adaptCost
						+ (double) compensationCost * (alpha));
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
