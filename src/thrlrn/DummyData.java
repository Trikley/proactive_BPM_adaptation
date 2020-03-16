package thrlrn;

import java.util.List;
import java.util.Random;

public class DummyData {

	public static DataSet generateDummyTrainingDataSet(int size, int minCheckpoints, int maxCheckpoints) {
		Random rng = new Random();
		DataSet result = new DataSet(0, 0, 0, 0, 0);
		for (int i = 0; i < size; i++) {
			Case trainingCase = new Case();
			trainingCase.caseId = i + 1;
			trainingCase.plannedDuration = 1.0;
			float checkpoints = (float) (rng.nextInt(maxCheckpoints-minCheckpoints) + minCheckpoints);
			trainingCase.caseLength = (int) checkpoints;
			float randFloat = rng.nextFloat();
			if (randFloat >= 0.5f) {
				trainingCase.actualDuration = 0.5;
				for (int c = 1; c <= checkpoints; c++) {
					float accuracyCoefficient = ((((float) (c-1)) / (checkpoints-1f)) *3f) + 1f;
					float predictedDuration = 0.5f + ((float) Math.pow(rng.nextDouble(), accuracyCoefficient));
					trainingCase.predictedDuration[c] = predictedDuration;
					trainingCase.binaryPrediction[c] = predictedDuration > 1f;
					boolean predictionTrue = predictedDuration < 1f;
					double reliabilty;
					if (predictionTrue) {
						reliabilty = 1. - ((float) Math.pow(rng.nextDouble(), accuracyCoefficient));
					} else {
						reliabilty = ((float) Math.pow(rng.nextDouble(), accuracyCoefficient));
					}
					trainingCase.reliability[c] = reliabilty;
				}
			} else {
				trainingCase.actualDuration = 1.5;
				for (int c = 1; c <= checkpoints; c++) {
					float accuracyCoefficient = ((((float) (c-1)) / (checkpoints-1f)) * 3f) + 1f;
					float predictedDuration = 1.5f - ((float) Math.pow(rng.nextDouble(), accuracyCoefficient));
					trainingCase.predictedDuration[c] = predictedDuration;
					trainingCase.binaryPrediction[c] = predictedDuration > 1f;
					boolean predictionTrue = predictedDuration > 1f;
					double reliabilty;
					if (predictionTrue) {
						reliabilty = 1. - ((float) Math.pow(rng.nextDouble(), accuracyCoefficient));
					} else {
						reliabilty = ((float) Math.pow(rng.nextDouble(), accuracyCoefficient));
					}
					trainingCase.reliability[c] = reliabilty;
				}
			}
			result.cases.add(trainingCase);
		}
		return result;
	}

	public static void testDataSet(DataSet data) {
		List<Case> cases = data.cases;
		float[] sumReliability = new float[101];
		for (int i = 0; i < sumReliability.length; i++) {
			sumReliability[i] = -1f;
		}
		float[] sumPredictionDifference = new float[101];
		for (int i = 0; i < sumReliability.length; i++) {
			sumPredictionDifference[i] = -1f;
		}
		float[] checkpointCount = new float[101];
		for (int i = 0; i < checkpointCount.length; i++) {
			checkpointCount[i] = 0;
		}
		
		float[] sumReliabilityTrue = new float[101];
		for (int i = 0; i < sumReliabilityTrue.length; i++) {
			sumReliabilityTrue[i] = -1f;
		}
		float[] sumPredictionDifferenceTrue = new float[101];
		for (int i = 0; i < sumReliabilityTrue.length; i++) {
			sumPredictionDifferenceTrue[i] = -1f;
		}
		float[] checkpointCountTrue = new float[101];
		for (int i = 0; i < checkpointCountTrue.length; i++) {
			checkpointCountTrue[i] = 0;
		}

		float[] sumReliabilityWithViolation = new float[101];
		for (int i = 0; i < sumReliabilityWithViolation.length; i++) {
			sumReliabilityWithViolation[i] = -1f;
		}
		float[] sumPredictionDifferenceWithViolation = new float[101];
		for (int i = 0; i < sumReliabilityWithViolation.length; i++) {
			sumPredictionDifferenceWithViolation[i] = -1f;
		}
		float[] checkpointCountWithViolation = new float[101];
		for (int i = 0; i < checkpointCountWithViolation.length; i++) {
			checkpointCountWithViolation[i] = 0;
		}
		
		float[] sumReliabilityTrueWithViolation = new float[101];
		for (int i = 0; i < sumReliabilityTrueWithViolation.length; i++) {
			sumReliabilityTrueWithViolation[i] = -1f;
		}
		float[] sumPredictionDifferenceTrueWithViolation = new float[101];
		for (int i = 0; i < sumReliabilityTrueWithViolation.length; i++) {
			sumPredictionDifferenceTrueWithViolation[i] = -1f;
		}
		float[] checkpointCountTrueWithViolation = new float[101];
		for (int i = 0; i < checkpointCountTrueWithViolation.length; i++) {
			checkpointCountTrueWithViolation[i] = 0;
		}
		

		float[] sumReliabilityNoViolation = new float[101];
		for (int i = 0; i < sumReliabilityNoViolation.length; i++) {
			sumReliabilityNoViolation[i] = -1f;
		}
		float[] sumPredictionDifferenceNoViolation = new float[101];
		for (int i = 0; i < sumReliabilityNoViolation.length; i++) {
			sumPredictionDifferenceNoViolation[i] = -1f;
		}
		float[] checkpointCountNoViolation = new float[101];
		for (int i = 0; i < checkpointCountNoViolation.length; i++) {
			checkpointCountNoViolation[i] = 0;
		}
		
		float[] sumReliabilityTrueNoViolation = new float[101];
		for (int i = 0; i < sumReliabilityTrueNoViolation.length; i++) {
			sumReliabilityTrueNoViolation[i] = -1f;
		}
		float[] sumPredictionDifferenceTrueNoViolation = new float[101];
		for (int i = 0; i < sumReliabilityTrueNoViolation.length; i++) {
			sumPredictionDifferenceTrueNoViolation[i] = -1f;
		}
		float[] checkpointCountTrueNoViolation = new float[101];
		for (int i = 0; i < checkpointCountTrueNoViolation.length; i++) {
			checkpointCountTrueNoViolation[i] = 0;
		}

		for (Case c : cases) {
			for (int checkpoint = 1; checkpoint <= c.caseLength; checkpoint++) {
				double predictionDifference = Math.abs((c.predictedDuration[checkpoint] - c.actualDuration));
				boolean predictionTrue = (c.binaryPrediction[checkpoint] == c.actualDuration>c.plannedDuration);
				sumPredictionDifference[checkpoint] += predictionDifference;
				sumReliability[checkpoint] += c.reliability[checkpoint];
				checkpointCount[checkpoint]++;
				if(predictionTrue) {
					sumPredictionDifferenceTrue[checkpoint] += predictionDifference;
					sumReliabilityTrue[checkpoint] += c.reliability[checkpoint];
					checkpointCountTrue[checkpoint]++;
				}

				if (c.actualDuration > c.plannedDuration) {
					sumPredictionDifferenceWithViolation[checkpoint] += predictionDifference;
					sumReliabilityWithViolation[checkpoint] += c.reliability[checkpoint];
					checkpointCountWithViolation[checkpoint]++;
					if(predictionTrue) {
						sumPredictionDifferenceTrueWithViolation[checkpoint] += predictionDifference;
						sumReliabilityTrueWithViolation[checkpoint] += c.reliability[checkpoint];
						checkpointCountTrueWithViolation[checkpoint]++;
					}
				} else {
					sumPredictionDifferenceNoViolation[checkpoint] += predictionDifference;
					sumReliabilityNoViolation[checkpoint] += c.reliability[checkpoint];
					checkpointCountNoViolation[checkpoint]++;
					if(predictionTrue) {
						sumPredictionDifferenceTrueNoViolation[checkpoint] += predictionDifference;
						sumReliabilityTrueNoViolation[checkpoint] += c.reliability[checkpoint];
						checkpointCountTrueNoViolation[checkpoint]++;
					}
				}
			}
		}

		StringBuilder checkpointCountOut = new StringBuilder();
		StringBuilder reliabiltyOut = new StringBuilder();
		StringBuilder predictionDifferenceOut = new StringBuilder();
		
		StringBuilder checkpointCountTrueOut = new StringBuilder();
		StringBuilder reliabiltyTrueOut = new StringBuilder();
		StringBuilder predictionDifferenceTrueOut = new StringBuilder();
		StringBuilder checkpointCountFalseOut = new StringBuilder();
		StringBuilder reliabiltyFalseOut = new StringBuilder();
		StringBuilder predictionDifferenceFalseOut = new StringBuilder();
		
		StringBuilder checkpointCountWithViolationOut = new StringBuilder();
		StringBuilder reliabiltyWithViolationOut = new StringBuilder();
		StringBuilder predictionDifferenceWithViolationOut = new StringBuilder();
		
		StringBuilder checkpointCountWithViolationTrueOut = new StringBuilder();
		StringBuilder reliabiltyWithViolationTrueOut = new StringBuilder();
		StringBuilder predictionDifferenceWithViolationTrueOut = new StringBuilder();
		StringBuilder checkpointCountWithViolationFalseOut = new StringBuilder();
		StringBuilder reliabiltyWithViolationFalseOut = new StringBuilder();
		StringBuilder predictionDifferenceWithViolationFalseOut = new StringBuilder();
		
		StringBuilder checkpointCountNoViolationOut = new StringBuilder();
		StringBuilder reliabiltyNoViolationOut = new StringBuilder();
		StringBuilder predictionDifferenceNoViolationOut = new StringBuilder();
		
		StringBuilder checkpointCountNoViolationTrueOut = new StringBuilder();
		StringBuilder reliabiltyNoViolationTrueOut = new StringBuilder();
		StringBuilder predictionDifferenceNoViolationTrueOut = new StringBuilder();
		StringBuilder checkpointCountNoViolationFalseOut = new StringBuilder();
		StringBuilder reliabiltyNoViolationFalseOut = new StringBuilder();
		StringBuilder predictionDifferenceNoViolationFalseOut = new StringBuilder();
		
		for (int i = 0; i < 101; i++) {
			if (checkpointCount[i] == 0) {
				checkpointCountOut.append("Checkpoint " + i + " never reached!\n");
			} else {
				checkpointCountOut.append("Checkpoint " + i + " reached " + checkpointCount[i] + " times!\n");
				float averageReliabilty = sumReliability[i] / checkpointCount[i];
				float averagePredictionDifference = sumPredictionDifference[i] / checkpointCount[i];
				reliabiltyOut.append("Average Reliability of checkpoint " + i + " is " + averageReliabilty + "\n");
				predictionDifferenceOut.append("Average Difference in Prediction of checkpoint " + i + " is "
						+ averagePredictionDifference + "\n");
			}
			if (checkpointCountTrue[i] == 0) {
				checkpointCountTrueOut.append("Checkpoint " + i + " never true!\n");
			} else {
				checkpointCountTrueOut.append("Checkpoint " + i + " is true " + checkpointCountTrue[i]/checkpointCount[i] + "!\n");
				float averageReliabiltyTrue = sumReliabilityTrue[i] / checkpointCountTrue[i];
				float averagePredictionDifferenceTrue = sumPredictionDifferenceTrue[i] / checkpointCountTrue[i];
				reliabiltyTrueOut.append("Average True Reliability of checkpoint " + i + " is " + averageReliabiltyTrue + "\n");
				predictionDifferenceTrueOut.append("Average True Difference in Prediction of checkpoint " + i + " is "
						+ averagePredictionDifferenceTrue + "\n");
			}
			if (checkpointCount[i]-checkpointCountTrue[i] == 0) {
				checkpointCountFalseOut.append("Checkpoint " + i + " never false!\n");
			} else {
				checkpointCountFalseOut.append("Checkpoint " + i + " is false " + (checkpointCount[i]-checkpointCountTrue[i])/checkpointCount[i] + "!\n");
				float averageReliabiltyFalse = (sumReliability[i]-sumReliabilityTrue[i]) / (checkpointCount[i]-checkpointCountTrue[i]);
				float averagePredictionDifferenceFalse = (sumPredictionDifference[i]-sumPredictionDifferenceTrue[i]) / (checkpointCount[i]-checkpointCountTrue[i]);
				reliabiltyFalseOut.append("Average False Reliability of checkpoint " + i + " is " + averageReliabiltyFalse + "\n");
				predictionDifferenceFalseOut.append("Average False Difference in Prediction of checkpoint " + i + " is "
						+ averagePredictionDifferenceFalse + "\n");
			}
			
			
			if (checkpointCountWithViolation[i] == 0) {
				checkpointCountWithViolationOut.append("Checkpoint " + i + " had no violations!\n");
			} else {
				checkpointCountWithViolationOut.append("Checkpoint " + i + " included " + checkpointCountWithViolation[i]/checkpointCount[i] + " violations!\n");
				float averageReliabiltyWithViolation = sumReliabilityWithViolation[i] / checkpointCountWithViolation[i];
				float averagePredictionDifferenceWithViolation = sumPredictionDifferenceWithViolation[i]
						/ checkpointCountWithViolation[i];
				reliabiltyWithViolationOut.append("Average Reliability of Violations of checkpoint " + i + " is "
						+ averageReliabiltyWithViolation + "\n");
				predictionDifferenceWithViolationOut
						.append("Average Difference in Prediction of Violations of checkpoint " + i + " is "
								+ averagePredictionDifferenceWithViolation + "\n");
			}
			if (checkpointCountTrueWithViolation[i] == 0) {
				checkpointCountWithViolationTrueOut.append("Checkpoint " + i + " never true with violation!\n");
			} else {
				checkpointCountWithViolationTrueOut.append("Checkpoint " + i + " is true with violation " + checkpointCountTrueWithViolation[i]/checkpointCountWithViolation[i] + "!\n");
				float averageReliabiltyTrueWithViolation = sumReliabilityTrueWithViolation[i] / checkpointCountTrueWithViolation[i];
				float averagePredictionDifferenceTrueWithViolation = sumPredictionDifferenceTrueWithViolation[i] / checkpointCountTrueWithViolation[i];
				reliabiltyWithViolationTrueOut.append("Average True Reliability of checkpoint " + i + " with violation is " + averageReliabiltyTrueWithViolation + "\n");
				predictionDifferenceWithViolationTrueOut.append("Average True Difference in Prediction of checkpoint " + i + " with violation is "
						+ averagePredictionDifferenceTrueWithViolation + "\n");
			}
			if (checkpointCountWithViolation[i]-checkpointCountTrueWithViolation[i] == 0) {
				checkpointCountWithViolationFalseOut.append("Checkpoint " + i + " never false with violation!\n");
			} else {
				checkpointCountWithViolationFalseOut.append("Checkpoint " + i + " is false with violation " + (checkpointCountWithViolation[i]-checkpointCountTrueWithViolation[i])/checkpointCountWithViolation[i] + "!\n");
				float averageReliabiltyFalseWithViolation = (sumReliabilityWithViolation[i]-sumReliabilityTrueWithViolation[i]) / (checkpointCountWithViolation[i]-checkpointCountTrueWithViolation[i]);
				float averagePredictionDifferenceFalseWithViolation = (sumPredictionDifferenceWithViolation[i]-sumPredictionDifferenceTrueWithViolation[i]) / (checkpointCountWithViolation[i]-checkpointCountTrueWithViolation[i]);
				reliabiltyWithViolationFalseOut.append("Average False Reliability of checkpoint " + i + " with violation is " + averageReliabiltyFalseWithViolation + "\n");
				predictionDifferenceWithViolationFalseOut.append("Average False Difference in Prediction of checkpoint " + i + " with violation is "
						+ averagePredictionDifferenceFalseWithViolation + "\n");
			}
			
			if (checkpointCountNoViolation[i] == 0) {
				checkpointCountNoViolationOut.append("Checkpoint " + i + " had no violations!\n");
			} else {
				checkpointCountNoViolationOut.append("Checkpoint " + i + " reached " + checkpointCountNoViolation[i]/checkpointCount[i] + " without violation!\n");
				float averageReliabiltyNoViolation = sumReliabilityNoViolation[i] / checkpointCountNoViolation[i];
				float averagePredictionDifferenceNoViolation = sumPredictionDifferenceNoViolation[i]
						/ checkpointCountNoViolation[i];
				reliabiltyNoViolationOut.append("Average Reliability of Violations of checkpoint " + i + " is "
						+ averageReliabiltyNoViolation + "\n");
				predictionDifferenceNoViolationOut
						.append("Average Difference in Prediction of Violations of checkpoint " + i + " is "
								+ averagePredictionDifferenceNoViolation + "\n");
			}
			if (checkpointCountTrueNoViolation[i] == 0) {
				checkpointCountNoViolationTrueOut.append("Checkpoint " + i + " never true without violation!\n");
			} else {
				checkpointCountNoViolationTrueOut.append("Checkpoint " + i + " is true without violation " + checkpointCountTrueNoViolation[i]/checkpointCountNoViolation[i] + "!\n");
				float averageReliabiltyTrueNoViolation = sumReliabilityTrueNoViolation[i] / checkpointCountTrueNoViolation[i];
				float averagePredictionDifferenceTrueNoViolation = sumPredictionDifferenceTrueNoViolation[i] / checkpointCountTrueNoViolation[i];
				reliabiltyNoViolationTrueOut.append("Average True Reliability of checkpoint " + i + " without violation is " + averageReliabiltyTrueNoViolation + "\n");
				predictionDifferenceNoViolationTrueOut.append("Average True Difference in Prediction of checkpoint " + i + " without violation is "
						+ averagePredictionDifferenceTrueNoViolation + "\n");
			}
			if (checkpointCountNoViolation[i]-checkpointCountTrueNoViolation[i] == 0) {
				checkpointCountNoViolationFalseOut.append("Checkpoint " + i + " never false without violation!\n");
			} else {
				checkpointCountNoViolationFalseOut.append("Checkpoint " + i + " is false without violation " + (checkpointCountNoViolation[i]-checkpointCountTrueNoViolation[i])/checkpointCountNoViolation[i] + "!\n");
				float averageReliabiltyFalseNoViolation = (sumReliabilityNoViolation[i]-sumReliabilityTrueNoViolation[i]) / (checkpointCountNoViolation[i]-checkpointCountTrueNoViolation[i]);
				float averagePredictionDifferenceFalseNoViolation = (sumPredictionDifferenceNoViolation[i]-sumPredictionDifferenceTrueNoViolation[i]) / (checkpointCountNoViolation[i]-checkpointCountTrueNoViolation[i]);
				reliabiltyNoViolationFalseOut.append("Average False Reliability of checkpoint " + i + " without violation is " + averageReliabiltyFalseNoViolation + "\n");
				predictionDifferenceNoViolationFalseOut.append("Average False Difference in Prediction of checkpoint " + i + " without violation is "
						+ averagePredictionDifferenceFalseNoViolation + "\n");
			}
		}
			
		System.out.println(checkpointCountOut.toString());
		System.out.println(reliabiltyOut.toString());
		System.out.println(predictionDifferenceOut.toString());
		
		System.out.println(checkpointCountTrueOut.toString());
		System.out.println(reliabiltyTrueOut.toString());
		System.out.println(predictionDifferenceTrueOut.toString());
		System.out.println(checkpointCountFalseOut.toString());
		System.out.println(reliabiltyFalseOut.toString());
		System.out.println(predictionDifferenceFalseOut.toString());
		
		System.out.println(checkpointCountWithViolationOut.toString());
		System.out.println(reliabiltyWithViolationOut.toString());
		System.out.println(predictionDifferenceWithViolationOut.toString());
		
		System.out.println(checkpointCountWithViolationTrueOut.toString());
		System.out.println(reliabiltyWithViolationTrueOut.toString());
		System.out.println(predictionDifferenceWithViolationTrueOut.toString());
		System.out.println(checkpointCountWithViolationFalseOut.toString());
		System.out.println(reliabiltyWithViolationFalseOut.toString());
		System.out.println(predictionDifferenceWithViolationFalseOut.toString());
		
		System.out.println(checkpointCountNoViolationOut.toString());
		System.out.println(reliabiltyNoViolationOut.toString());
		System.out.println(predictionDifferenceNoViolationOut.toString());
		
		System.out.println(checkpointCountNoViolationTrueOut.toString());
		System.out.println(reliabiltyNoViolationTrueOut.toString());
		System.out.println(predictionDifferenceNoViolationTrueOut.toString());
		System.out.println(checkpointCountNoViolationFalseOut.toString());
		System.out.println(reliabiltyNoViolationFalseOut.toString());
		System.out.println(predictionDifferenceNoViolationFalseOut.toString());
		
	}
	
	public static void main(String[] args) {
		int numberCases = 30000;
		DataSet data = generateDummyTrainingDataSet(numberCases, 5, 100);
		testDataSet(data);
	}
}
