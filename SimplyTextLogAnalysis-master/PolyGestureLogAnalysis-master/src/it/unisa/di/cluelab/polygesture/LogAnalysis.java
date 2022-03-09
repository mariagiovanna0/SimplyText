package it.unisa.di.cluelab.polygesture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LogAnalysis {
	private static final String[] TASK_NAMES = new String[] { "Cancella carattere", "Cancella parola", "Cancella frase",
			"Sposta parola (taglia - incolla)", "Sposta frase", "Correggi testo", "Duplica parola (Copia - incolla)","Duplica frase" };
	private static final String[] MODES = new String[] { "ET_2.5", "ET_3.0", "TE_2.5", "TE_3.0" };

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("missing arguments");
			System.exit(1);
		}

		File logDir = new File(args[0]);
		File outDir = new File(args[1]);
		File[] userDirs = logDir.listFiles((dir, name) -> new File(dir, name).isDirectory());
		Arrays.sort(userDirs, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				String n1 = o1.getName();
				if (n1.length() <= 1 || !Character.isDigit(n1.charAt(1))) {
					n1 = "0" + n1;
				}
				String n2 = o2.getName();
				if (n2.length() <= 1 || !Character.isDigit(n2.charAt(1))) {
					n2 = "0" + n2;
				}
				return n1.compareTo(n2);
			}
		});
		String header = "user,task1,task2,task3,task4,task5,task6,task7,task8,sum";
		String[] titles = new String[] { "EFFECTIVE TIME:", "TOTAL TIME:", "FAILURES:" };
		NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
		nf.setMaximumFractionDigits(4);
		StringBuilder[][] results = new StringBuilder[titles.length][];
		for (int i = 0; i < titles.length; i++) {
			results[i] = new StringBuilder[userDirs.length + 2];
			for (int j = 0; j < userDirs.length; j++) {
				results[i][j] = new StringBuilder(userDirs[j].getName());
			}
			results[i][userDirs.length] = new StringBuilder("AVG");
			results[i][userDirs.length + 1] = new StringBuilder("STDEV");
		}
		for (String mode : MODES) {
			ArrayList<List<Long>[]> stats = new ArrayList<List<Long>[]>();
			for (File userDir : userDirs) {
				stats.add(getStats(userDir, userDir.getName() + "_" + mode + ".log"));
			}
			try (PrintWriter out = new PrintWriter(new File(outDir, mode + ".csv"))) {
				for (int ti = 0; ti < titles.length; ti++) {
					final double div = ti == 2 ? 1 : 1000.;
					out.println(titles[ti]);
					out.println(header);
					long[] sums = new long[TASK_NAMES.length + 1];
					long[] numSums = new long[sums.length];
					for (int i = 0; i < userDirs.length; i++) {
						out.print(userDirs[i].getName());
						List<Long> statTime = stats.get(i)[ti];
						String last = null;
						for (int j = 0; j < statTime.size(); j++) {
							if (statTime.get(j) != null) {
								sums[j] += statTime.get(j);
								last = "," + nf.format(statTime.get(j) / div);
								numSums[j]++;
							} else {
								last = ",";
							}
							out.print(last);
						}
						out.println();
						results[ti][i].append(last);
					}

					out.print("AVG");
					double[] avg = new double[sums.length];
					String last = null;
					for (int i = 0; i < sums.length; i++) {
						avg[i] = (sums[i] / div) / numSums[i];
						last = "," + nf.format(avg[i]);
						out.print(last);
					}
					results[ti][userDirs.length].append(last);

					double[] stdevs = new double[sums.length];
					for (int i = 0; i < userDirs.length; i++) {
						List<Long> statTime = stats.get(i)[ti];
						for (int j = 0; j < statTime.size(); j++) {
							if (statTime.get(j) != null) {
								double d = (statTime.get(j) / div) - avg[j];
								stdevs[j] += d * d;
							}
						}
					}
					out.print("\nSTDEV");
					for (int i = 0; i < stdevs.length; i++) {
						last = "," + nf.format(Math.sqrt((stdevs[i] / (numSums[i] - 1))));
						out.print(last);
					}
					out.println("\n");
					results[ti][userDirs.length + 1].append(last);
				}
			}
		}
		try (PrintWriter out = new PrintWriter(new File(outDir, "results.csv"))) {
			for (int ti = 0; ti < titles.length; ti++) {
				out.println(titles[ti]);
				out.print("user");
				for (String s : MODES) {
					out.print("," + s);
				}
				out.println();
				for (StringBuilder s : results[ti]) {
					out.println(s);
				}
				out.println();
			}
		}
	}

	private static List<Long>[] getStats(File dir, String filename) throws IOException {
		HashMap<String, Long> taskStarted = new HashMap<>();
		HashMap<String, Long> taskCompleted = new HashMap<>();
		HashMap<String, Long> taskCompletedEffective = new HashMap<>();
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(dir, filename))))) {
			long firstTouchTime = -1L;
			for (String line; (line = br.readLine()) != null;) {
				String[] sp = line.split("\t", 4);
				if (sp.length != 4) {
					continue;
				}
				String event = sp[2];
				String desc = sp[3];
				switch (event) {
				case "TaskStarted":
					firstTouchTime = -1L;
					Long n = taskStarted.get(desc);
					taskStarted.put(desc, n == null ? 1 : n + 1);
					break;
				case "InputEvent":
					if (firstTouchTime == -1L && (desc.startsWith("TouchEvent") || desc.startsWith("ACTION_DOWN"))) {
						firstTouchTime = Long.parseLong(sp[0]);
					}
					break;
				case "TaskCompleted":
					String[] task = desc.split(" in ", 2);
					if (firstTouchTime == -1L) {
						throw new IllegalStateException(filename + " - no touch event for task: " + task[0]);
					}
					String[] seconds = task[1].replace("\"", "").split(" seconds", 2);
					long time = Long.parseLong(seconds[0]);
					taskCompleted.put(task[0], time);
					long effTimeCalc = Long.parseLong(sp[0]) - firstTouchTime;
					if (time < effTimeCalc) {
						System.err.println(
								filename + " - time < effTimeCalc: " + time + " < " + effTimeCalc + " (replaced)");
						effTimeCalc = time;
					}
					if (seconds[1].isEmpty()) {
						taskCompletedEffective.put(task[0], effTimeCalc);
					} else {
						long effTime = Long
								.parseLong(seconds[1].substring(seconds[1].indexOf(':') + 2).replace(")", ""));
						if (Math.abs(effTime - effTimeCalc) >= 25L) {
							System.err.println(filename + " " + task[0] + " - difference: " + (effTime - effTimeCalc));
						}
						taskCompletedEffective.put(task[0], effTime);
					}
					firstTouchTime = -1L;
					taskStarted.put(task[0], taskStarted.get(task[0]) - 1);
				}
			}
		}
		@SuppressWarnings("unchecked")
		List<Long>[] res = new List[] { new ArrayList<>(), new ArrayList<>(), new ArrayList<>() };
		for (String taskName : TASK_NAMES) {
			res[0].add(taskCompletedEffective.get(taskName));
			res[1].add(taskCompleted.get(taskName));
			res[2].add(taskStarted.get(taskName));
		}
		for (List<Long> s : res) {
			long sum = 0L;
			for (Long t : s) {
				if (t != null) {
					sum += t;
				}
			}
			s.add(sum);
		}
		return res;
	}

}
