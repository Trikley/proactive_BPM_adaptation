package thrlrn;

public class RLNaiveCostModel implements RLCostModel{

	double adaptCost;
	double compensationCost;
	double actPenalty;
	
	public RLNaiveCostModel(double adaptCost, double actPenalty, double compensationCost) {
		super();
		this.adaptCost = adaptCost;
		this.actPenalty = actPenalty;
		this.compensationCost = compensationCost;
	}

	@Override
	public long calculateAdaptationCost(boolean adapted, boolean violation, int caseLength, int stepInCase) {
		if(adapted) {
			if(violation) {
				return (long) adaptCost;
			}else {
				return (long) ((double) adaptCost + (double) compensationCost);
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
