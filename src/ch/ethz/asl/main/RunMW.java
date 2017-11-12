package ch.ethz.asl.main;

import java.util.*;

public class RunMW {

	static String myIp = null;
	static int myPort = 8080;
	static List<String> mcAddresses = null;
	static int numThreadsPTP = -1;
	static boolean readSharded = false;

	
	public static void main(String[] args) throws Exception {

		// -----------------------------------------------------------------------------
		// Parse and prepare arguments
		// -----------------------------------------------------------------------------
		parseArguments(args);

		// -----------------------------------------------------------------------------
		// Start the Middleware
		// -----------------------------------------------------------------------------

		// check for specs deviations and cut it
		specifiactionsCheck();
		System.out.println("Middleware started...");

		new MiddlewareMain(myIp, myPort, mcAddresses, numThreadsPTP, readSharded).run();

//		// hook statistics
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			@Override
//			public void run() {
//				System.out.println("Triggered shutdown hook!");
//			}
//		});
//
//		while (true) {
//			Thread.sleep(1000);
//		}

	}

	private static void specifiactionsCheck() {
		if (mcAddresses.size() > 3) {
			printUsageWithError("Max # of servers is 3 (according to specification)");
			System.exit(1);
		}

		if (numThreadsPTP > 128) {
			printUsageWithError("Max # of threads is 128 (according to specification)");
			System.exit(1);
		}
	}

	// outputs all params to Console
	private static void printParams(Map<String, List<String>> params) {
		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
			String paramName = entry.getKey();

			List<String> paramList = entry.getValue();

			String paramValues = "";
			for (String val: paramList) {
				paramValues += val + " ";
			}

			System.out.println(paramName + " | " + paramValues);
		}
	}
	
	private static void parseArguments(String[] args) {
		Map<String, List<String>> params = new HashMap<>();

		List<String> options = null;
		for (int i = 0; i < args.length; i++) {
			final String a = args[i];

			if (a.charAt(0) == '-') {
				if (a.length() < 2) {
					System.err.println("Error at argument " + a);
					System.exit(1);
				}

				options = new ArrayList<String>();
				params.put(a.substring(1), options);
			} else if (options != null) {
				options.add(a);
			} else {
				System.err.println("Illegal parameter usage");
				System.exit(1);
			}
		}

		if (params.size() == 0) {
			printUsageWithError(null);
			System.exit(1);
		}

		if (params.get("l") != null)
			myIp = params.get("l").get(0);
		else {
			printUsageWithError("Provide this machine's external IP! (see ifconfig or your VM setup)");
			System.exit(1);			
		}

		if (params.get("p") != null)
			myPort = Integer.parseInt(params.get("p").get(0));
		else {
			printUsageWithError("Provide the port, that the middleware listens to (e.g. 11212)!");
			System.exit(1);			
		}

		if (params.get("m") != null) {
			mcAddresses = params.get("m");
		} else {
			printUsageWithError(
					"Give at least one memcached backend server IP address and port (e.g. 123.11.11.10:11211)!");
			System.exit(1);
		}

		if (params.get("t") != null)
			numThreadsPTP = Integer.parseInt(params.get("t").get(0));
		else {
			printUsageWithError("Provide the number of threads for the threadpool!");
			System.exit(1);
		}

		if (params.get("s") != null)
			readSharded = Boolean.parseBoolean(params.get("s").get(0));
		else {
			printUsageWithError("Provide true/false to enable sharded reads!");
			System.exit(1);
		}

		//printParams(params);

	}

	private static void printUsageWithError(String errorMessage) {
		System.err.println();
		System.err.println(
				"Usage: -l <MyIP> -p <MyListenPort> -t <NumberOfThreadsInPool> -s <readSharded> -m <MemcachedIP:Port> <MemcachedIP2:Port2> ...");
		if (errorMessage != null) {
			System.err.println();
			System.err.println("Error message: " + errorMessage);
		}

	}
}