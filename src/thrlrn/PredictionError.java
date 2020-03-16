package thrlrn;

import java.util.Vector;

public class PredictionError {

	public Vector<Double> pred;
	public Vector<Double> act; 
	public int fn;
	
	public PredictionError() {
		pred = new Vector<Double>();
		act = new Vector<Double>();
	}
	
	
}
