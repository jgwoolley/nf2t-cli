package com.yelloowstone.nf2t.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ProcessUtils {
	public static int exec(String... args) throws IOException, InterruptedException {
		return exec(null, args);
	}
	
	public static int exec(File directory, String... args) throws IOException, InterruptedException {
		try {
			final ProcessBuilder builder = new ProcessBuilder(args);
			builder.directory(directory);
			final Process process = builder.start();

			// Get the process's input stream (where you send data to gpg)
			final OutputStream gpgInput = process.getOutputStream();

			// Provide the input gpg expects (e.g., a blank line, or your passphrase if
			// needed)
			final String inputString = "\n"; // Or "your_passphrase\n" if required
			gpgInput.write(inputString.getBytes());
			gpgInput.flush();
			gpgInput.close(); // Important: Close the input stream

			// Thread for reading stdout
			new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						System.out.println("Stdout: " + line); // Or process the line as needed
					}
				} catch (IOException e) {
					e.printStackTrace(); // Handle or log the error
				}
			}).start();

			// Thread for reading stderr (important to prevent blocking)
			new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						System.err.println("Stderr: " + line); // Or process the line as needed
					}
				} catch (IOException e) {
					e.printStackTrace(); // Handle or log the error
				}
			}).start();

			return process.waitFor();
		} catch(Exception e) { 
			System.err.println("Failure executing commaind [" + String.join(" ", args) + "] within working directory [" + directory + "].");
			e.printStackTrace();
		}
		
		return 1;
	}
}
