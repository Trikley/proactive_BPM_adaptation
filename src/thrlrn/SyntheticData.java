package thrlrn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;


public class SyntheticData {
	
	public static void generate(int checkpoints, int cases, double posCases) throws Exception {
		
		double pos = 0;
		double acc = 0;
		
		boolean actualViolation = false;
		boolean predictedViolation = false;
		
		Random randCase = new Random();
		double rdCase = 0;
		
		Random randAcc = new Random();
		double rdAcc = 0;
		
		Random randRel = new Random();
		double rdRel = 0;
		
		double rel = 0;
		double relRes = 0;
		
		Writer out = new BufferedWriter(new FileWriter("./dat-synth/data-DE.csv"));
		
		out.write("caseID;eventID;violation_actual;violation_predicted;reliability\n");
		
		for(int caseId = 1; caseId <= cases; caseId++) {

			// determine if pos / negative case
			rdCase = randCase.nextDouble();
			if(rdCase <= posCases)
				actualViolation = true;
			else
				actualViolation = false;	
			
			for(int i = 1; i <= checkpoints; i++) {
				pos = i / (double)checkpoints;
				
				// determine if prediction correct
				rdAcc = randAcc.nextDouble();
				acc = accuracyProb(pos);
				
				if(rdAcc <= acc) {
					predictedViolation = actualViolation;
				} else {
					predictedViolation = !actualViolation;
				}
				
				// determine reliability (75% of all reliability values should be higher or equal than rel -- see boxplots)
				rel = reliability(pos);
				rdRel = randRel.nextDouble();
				if(rdRel <= .75) {
					relRes = rel + (1-rel)*rdRel/.75f;
				} else {
					relRes = (1-rdRel)/.25f*rel;
				}
				
				out.write(""+caseId+";"+i+";"+actualViolation+";"+predictedViolation+";"+String.format(Locale.GERMAN, "%.3g", relRes)+"\n");
			}
		}
		
		out.close();
		
	}
	
	public static double reliability(double pos) {

		// Compute Log Normal Distribution -- parameters estimated based on SEAA Paper and XLS
	    // m is the scale parameter: this is the mean of the normally distributed natural logarithm of this distribution,
	    // s is the shape parameter: this is the standard deviation of the normally distributed natural logarithm of this distribution. 
		AbstractRealDistribution distr = new LogNormalDistribution(-1, .5); // scale and shape constructor.

		double CDF = 0;
		double rel = 0;

	    // ...this method represents the (cumulative) distribution function (CDF) for this distribution.
	    CDF = distr.cumulativeProbability(pos);
	    rel = 0.91+CDF*(0.98-0.91);
	    
	    return(rel);
	}
	
	
	public static double accuracyProb(double pos) {

		// Compute Log Normal Distribution -- parameters estimated based on SEAA Paper and XLS
	    // m is the scale parameter: this is the mean of the normally distributed natural logarithm of this distribution,
	    // s is the shape parameter: this is the standard deviation of the normally distributed natural logarithm of this distribution. 
		AbstractRealDistribution distr = new LogNormalDistribution(-1, 0.2); // scale and shape constructor.

		double CDF = 0;
		double acc = 0;
		double mcc = 0;

	    // ...this method represents the (cumulative) distribution function (CDF) for this distribution.
	    CDF = distr.cumulativeProbability(pos);
	    // mcc based on regression from C2K data
	    mcc = 0.05+CDF*(0.62-0.05);
	    // acc = 0.5 means an MCC of 0, as exactly 50/50 chance of getting it right
	    acc = 0.5 + 0.5*mcc;
	    
	    return(acc);
	}
	

}
