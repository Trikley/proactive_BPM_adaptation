package thrlrn;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

public class Experiment {

	int predictionType = 0;
	boolean costBased = false;

	public Experiment() {
		super();
	}

	public Experiment(int _predictionType, boolean _costBased) {
		this();
		predictionType = _predictionType;
		costBased = _costBased;
	}

	// ################### GENERIC
	public void ExperimentG(DataSetManager dsManager) throws Exception {
		while (true) {
			Socket socket = null;
			List<Float> costs = new LinkedList<Float>(); // Initialise as empty list to never have to check for null
															// reference
			try {
				socket = waitOnRLConnection();
				costs = iterateOverCases(dsManager.nextDataset(), socket);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				printCostsToConsole(costs);
				if (socket != null)
					socket.close();
			}
		}
	}

	private List<Float> iterateOverCases(DataSet ds, Socket socket) throws IOException {
		Vector<Case> cases = (Vector<Case>) ds.cases.clone();
		List<Float> result = new LinkedList<Float>();
		try {
			while (true) {
				if (Main.shuffle)
					Collections.shuffle(cases);
				Iterator<Case> casesIt = cases.iterator();
				// Iterator over the process instances (cases)
				Case myCase;
				int casesObserved = 0;
				int violationCases = 0;
				int casesInDataset = cases.size();

				while (casesIt.hasNext()) {
					Float cost;

					myCase = casesIt.next();
					if (Main.exclude && countViolationPredictions(myCase, 1) <= 0) {
						continue;
					}

					casesObserved++;
					if (myCase.actualDuration > myCase.plannedDuration) {
						violationCases++;
					}
					if (casesObserved % 1000 == 0) {
						System.out.println("Observed " + casesObserved + " cases, of which " + violationCases
								+ " included violations!");
					}
					if (!casesIt.hasNext()) {
						System.out.println("Observed " + casesObserved + " cases of the " + casesInDataset
								+ " cases in the dataset. Of those " + violationCases + " included violations!");
					}

					// Catch the first -1 action from resetting before case
					int action = receiveAction(socket);
					if (action != -1) {
						System.err.println("WTF!");
					}
					if (Main.exclude) {
						if (countViolationPredictions(myCase, 1) > 1)
							cost = (float) handleLongerCase(myCase, socket);
						else
							cost = (float) handleLength1Case(myCase, socket);
					} else {
						if (myCase.caseLength > 1)
							cost = (float) handleLongerCase(myCase, socket);
						else
							cost = (float) handleLength1Case(myCase, socket);
					}
					result.add(cost);
				}
			}
		} catch (EOFException e) {
			return result;
		}
	}

	private int handleLength1Case(Case myCase, Socket socket) throws IOException {
		int cost = 0;
		int pos = 1;

		// Search for the one predicted violation if excluding
		if (Main.exclude) {
			pos = getFirstViolationPrediction(myCase, pos);
		}

		sendRewardParameters(socket, false, 0);
		sendDone(socket, false, false);
		sendState(socket, myCase.caseId, myCase.actualDuration, myCase.predictedDuration[pos], myCase.plannedDuration,
				myCase.reliability[pos], pos, myCase.caseLength);

		pos++;
		int action = receiveAction(socket);
		if (action == -1) {
			System.err.println("WTF!");
		}
		boolean adapt = action == 1;

		cost += Main.costModel.calculateAdaptationCost(adapt, myCase.actualDuration > myCase.plannedDuration,
				myCase.caseLength, pos);
		sendRewardParameters(socket, adapt, cost);
		sendDone(socket, true, (adapt==myCase.actualDuration > myCase.plannedDuration));
		sendState(socket, myCase.caseId, myCase.actualDuration, 0, myCase.plannedDuration, 0, 0, myCase.caseLength);

		return cost;
	}

	private int handleLongerCase(Case myCase, Socket socket) throws IOException {
		int cost = 0;
		int pos = 1;

		// Search for the first predicted violation if excluding
		if (Main.exclude) {
			pos = getFirstViolationPrediction(myCase, pos);
		}

		sendRewardParameters(socket, false, cost);
		sendDone(socket, false, false);
		sendState(socket, myCase.caseId, myCase.actualDuration, myCase.predictedDuration[pos], myCase.plannedDuration,
				myCase.reliability[pos], pos, myCase.caseLength);

		pos++;
		for (int i = pos; i <= myCase.caseLength; i++) {

			// Predicted violation (positive case)
			boolean pred = (myCase.predictedDuration[i] > myCase.plannedDuration);

			if (!Main.exclude || pred) {
				int action = receiveAction(socket);
				if (action == -1) {
					System.err.println("WTF!");
				}
				boolean adapt = action == 1;
				// if end of case
				if (adapt || i == myCase.caseLength) {
					pos = i;
					cost += Main.costModel.calculateAdaptationCost(adapt,
							myCase.actualDuration > myCase.plannedDuration, myCase.caseLength, pos);
					sendRewardParameters(socket, adapt, cost);
					sendDone(socket, true, (adapt==myCase.actualDuration > myCase.plannedDuration));
					sendState(socket, myCase.caseId, myCase.actualDuration, 0, myCase.plannedDuration, 0, 0,
							myCase.caseLength);
					break;
				}
				sendRewardParameters(socket, adapt, cost);
				sendDone(socket, false, false);
				sendState(socket, myCase.caseId, myCase.actualDuration, myCase.predictedDuration[i],
						myCase.plannedDuration, myCase.reliability[i], i, myCase.caseLength);
			}
		}
		return cost;
	}

	private int countViolationPredictions(Case myCase, int checkFromStep) {
		int result = 0;
		for (int i = checkFromStep; i <= myCase.caseLength; i++) {
			if (myCase.predictedDuration[i] > myCase.plannedDuration) {
				result++;
			}
		}
		return result;
	}

	private int getFirstViolationPrediction(Case myCase, int checkFromStep) {
		for (int i = checkFromStep; i <= myCase.caseLength; i++) {
			if (myCase.predictedDuration[i] > myCase.plannedDuration) {
				return i;
			}
		}
		throw new RuntimeException("Case needs to include at least one violation prediction!");
	}

	private void printCostsToConsole(List<Float> costs) {
		float totalCosts = 0f;
		for (float c : costs) {
			totalCosts += c;
		}
		System.out.println("Number of cases: " + costs.size());
		System.out.println("Total Costs: " + totalCosts);
	}

	public void sendRewardParameters(Socket socket, boolean adapted, double cost) throws IOException {
		if (Main.verbose)
			System.out.println("Sending reward parameters...");
		PrintWriter out = new PrintWriter(socket.getOutputStream());
		out.print("" + adapted);
		out.println();
		out.print("" + (float) cost);
		out.println();
		out.flush();
		if (Main.verbose)
			System.out.println("Sent: " + adapted + ", " + cost);
	}

	public void sendState(Socket socket, int processID, double actualDuration, double predictedDuration,
			double planedDuration, double reliabilty, int checkPointPosition, int processLength) throws IOException {
		if (Main.verbose)
			System.out.println("Sending state...");
		PrintWriter out = new PrintWriter(socket.getOutputStream());
		out.print("" + (float) processID);
		out.println();
		out.print("" + (float) actualDuration);
		out.println();
		out.print("" + (float) predictedDuration);
		out.println();
		out.print("" + (float) planedDuration);
		out.println();
		out.print("" + (float) reliabilty);
		out.println();
		out.print("" + (float) checkPointPosition);
		out.println();
		out.print("" + (float) processLength);
		out.println();
		out.flush();
		if (Main.verbose)
			System.out.println("Sent: " + processID + ", " + predictedDuration + ", " + planedDuration + ", "
					+ reliabilty + ", " + checkPointPosition + ", " + processLength);
	}

	public void sendDone(Socket socket, boolean done, boolean trueNegativeStatus) throws IOException {
		if (Main.verbose)
			System.out.println("Sending done...");
		PrintWriter out = new PrintWriter(socket.getOutputStream());
		out.print("" + done);
		out.println();
		if(done) {
			out.print("" + trueNegativeStatus);
			out.println();
		}
		out.flush();
		if (Main.verbose)
			System.out.println("Sent: " + done);
	}

	public int receiveAction(Socket socket) throws IOException {
		if (Main.verbose)
			System.out.println("Receiving action...");
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String line;
		try {
			while ((line = in.readLine()).equals("")) {
				// Skip empty lines...
			}
		} catch (NullPointerException e) {
			throw new EOFException("EOF reached!");
		}
		line = line.trim().replaceAll("[\\[\\]]", "");
		if (line.endsWith(".")) {
			line = line.substring(0, line.length() - 2);
		}
		int adapt = Integer.parseInt(line);

		if (Main.verbose)
			System.out.println("Received: " + adapt);
		return adapt;
	}

	public Socket waitOnRLConnection() throws IOException {
		System.out.println("Waiting on incoming RL connection...");
		ServerSocket sSocket = new ServerSocket(1337);
		Socket socket = sSocket.accept();
		sSocket.close();
		System.out.println("Connected!");
		return socket;
	}

	public void closeConnection(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
