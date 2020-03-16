package thrlrn;

public class Case {

	public int caseId; 
	public int caseLength;
	
	// whole case
	public double plannedDuration; 
	public double actualDuration; 

	
	// check-point dependent
	public double[] predictedDuration = new double[310]; 
	public boolean[] binaryPrediction = new boolean[310];

	public double[] reliability = new double[310]; 
	public double[] numericReliability = new double[310]; 
	

}
