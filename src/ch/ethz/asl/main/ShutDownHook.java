package ch.ethz.asl.main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

// Aggregates the results from Log file
public class ShutDownHook extends Thread{

    @Override
    public void run() {
        // WAIT FOR STATS
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Middleware shuts down...");
        System.out.println("Final aggregates");

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream input = classloader.getResourceAsStream("log4j2.properties");

        Properties prop = new Properties();
        try {
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Agregation
        final String fileName = prop.getProperty("appender.STAT_FILE.fileName");

        try {
            String content = new String(Files.readAllBytes(Paths.get(fileName)));
            String[] lines = content.split("\r\n|\r|\n");
            System.out.println(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
