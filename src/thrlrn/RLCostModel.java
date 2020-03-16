package thrlrn;

public interface RLCostModel {

	long calculateAdaptationCost(boolean adapted, boolean violation, int caseLength, int stepInCase);
}
