package thrlrn;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkTest {

	public static void main(String[] args) throws IOException, InterruptedException {
		Main.verbose = true;
		MockExperiment mockEx = new MockExperiment();
		Socket socket = mockEx.waitOnRLConnection();
		for(int i=0; i<11; i++) {
			float action = mockEx.receiveAction(socket);
			mockEx.sendRewardParameters(socket, i%2==0, i);
			mockEx.sendDone(socket, i%3==0, i%5==0);
			mockEx.sendState(socket, (int)action, action, action, action, action, (int)action, (int)action);
		}
		mockEx.closeConnection(socket);
	}

}

class MockExperiment {
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
