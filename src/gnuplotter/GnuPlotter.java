package gnuplotter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GnuPlotter {

	static int CORES = 8;

	public static void main(String[] args) {
		BlockingQueue<Runnable> jobs = new LinkedBlockingQueue<Runnable>(1);
		Executor exec = new ThreadPoolExecutor(0, CORES, 2, TimeUnit.SECONDS, jobs);
		File currentDir = new File("").getAbsoluteFile();
		System.out.println("Working in " + currentDir.getAbsolutePath());
		List<File> csvs = new LinkedList<File>();
		recFindCsvFiles(currentDir, csvs);
		for(File csv:csvs) {
			handleCsv(csv, exec);
		}
		while (!jobs.isEmpty()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace(); 
			}
		}
	}

	static void recFindCsvFiles(File parentDir, List<File> output) {
		File[] children = parentDir.listFiles();
		for (File child : children) {
			if (child.isDirectory()) {
				recFindCsvFiles(child.getAbsoluteFile(), output);
			} else {
				if (child.getAbsolutePath().endsWith(".csv") && child.getAbsolutePath().contains("python_src")) {
					output.add(child);
				}
			}
		}
	}

	static void handleCsv(File csv, Executor exec) {
		boolean csvHandled = false;
		while(!csvHandled) {
			try {
				exec.execute(() -> {
					try {
						create20000Pic(csv);
						createCompletePic(csv);
						createFirstDatasetRunPic(csv);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
				csvHandled = true;
				Thread.sleep(10);
				break;
			} catch (RejectedExecutionException | InterruptedException e2) {
				csvHandled = false;
				continue;
			}
		}
	}
	
	static void create20000Pic(File csv) throws InterruptedException, IOException {
		createPicWithFirstRows(csv, 20000, "20000");
	}
	
	static void createCompletePic(File csv) throws InterruptedException, IOException {
		File png = new File(csv.getAbsolutePath().replace(".csv", ".png"));
		if(!png.exists()) {
			createProcess("filename='" + csv.getAbsolutePath()
			+ "';poutput='" + csv.getAbsolutePath().replace(".csv", ".png") + "'");
		}
	}
	
	static void createCompletePicByFirstColumn(File csv) throws InterruptedException, IOException {
		File png = new File(csv.getAbsolutePath().replace(".csv", ".png"));
		if(!png.exists()) {
			createProcess("filename='" + csv.getAbsolutePath()
			+ "';poutput='" + csv.getAbsolutePath().replace(".csv", ".png") + "';axiscolumn=1");
		}
	}
	
	static void createFirstDatasetRunPic(File csv) throws InterruptedException, IOException {
		String absolutePathCsv = csv.getAbsolutePath();
		int numberOfCases;
		if(absolutePathCsv.contains("csv_bpic12")) {
			numberOfCases = 4361;
		}else if(absolutePathCsv.contains("csv_bpic17")) {
			numberOfCases = 10500;
		}else if(absolutePathCsv.contains("csv_cargo")) {
			numberOfCases = 1313;
		}else if(absolutePathCsv.contains("csv_traffic")) {
			numberOfCases = 50117;
		}else {
			return;
		} 
		
		if(absolutePathCsv.toLowerCase().contains("maintain")) {
			File newCsv = copyFirstRunRowsToNewCsv(csv);
			createCompletePicByFirstColumn(newCsv);
			newCsv.delete();
		}else {
			createPicWithFirstRows(csv, numberOfCases, "FirstDatasetRun");
		}
		
	}
	
	static File copyFirstRunRowsToNewCsv(File originalCsv) throws IOException {
		File result = new File(originalCsv.getParentFile(), originalCsv.getName().replace(".csv", "OnlyFirstRun.csv"));
		if(!result.exists()) {
			result.createNewFile();
		}
		PrintWriter writer = new PrintWriter(new FileOutputStream(result, false));
		BufferedReader reader = new BufferedReader(new FileReader(originalCsv));
		boolean finished;
		String line;
		int lastCaseId = -1;
		do {
			line = reader.readLine();
			if(line!=null) {
				String[] collumns = line.split(",");
				int caseId = (int)Float.parseFloat(collumns[0]);
				if(caseId<lastCaseId) {
					finished = true;
					break;
				}else {
					finished = false;
					lastCaseId = caseId;
					writer.println(line);
				}
			}else {
				finished = true;
				break;
			}
		} while (!finished);
		
		writer.flush();
		writer.close();
		reader.close();
		
		return result;
	}
	
	static void createPicWithFirstRows(File csv, int rows, String appandage) throws InterruptedException, IOException {
		File png = new File(csv.getAbsolutePath().replace(".csv", appandage+".png"));
		if(!png.exists()) {
			createProcess("filename='" + csv.getAbsolutePath() + "';xbound='"+rows+"';poutput='"
					+ csv.getAbsolutePath().replace(".csv", appandage+".png") + "'");
		}
	}
	
	static Lock counterLock = new ReentrantLock();
	static int idCounter = 0;
	static int currentlyWorkedCounter = 0;
	static void createProcess(String command) throws InterruptedException, IOException {
		counterLock.lock();
		int counterCopy = idCounter;
		idCounter++;
		currentlyWorkedCounter++;
		counterLock.unlock();
		System.out.println("Working " + counterCopy + ":");
		System.out.println(command);
		ProcessBuilder processBuilder;	
		processBuilder = new ProcessBuilder("gnuplot", "-e" ,
				command,
				"plot_script.gp");
		processBuilder.inheritIO();
		processBuilder.start().waitFor();
		counterLock.lock();
		currentlyWorkedCounter--;
		System.out.println("Finished " + counterCopy + ", still working on " + currentlyWorkedCounter + "!");
		counterLock.unlock();
	}

}
