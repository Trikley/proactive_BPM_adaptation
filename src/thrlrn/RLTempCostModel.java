package thrlrn;

public class RLTempCostModel implements RLCostModel{

	double adaptCost;
	double compensationCost;
	double actPenalty;
	double maxAlpha;
	double minAlpha;
	
	public RLTempCostModel(double adaptCost, double actPenalty, double compensationCost, double maxAlpha, double minAlpha) {
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
				return (long)(alpha*1000L);
			}else {
				return (long)(-alpha*1000L);
			}
		}else {
			if(violation) {
				return 1000L;
			}else {
				return -1000L;
			}
		}
	}
}
