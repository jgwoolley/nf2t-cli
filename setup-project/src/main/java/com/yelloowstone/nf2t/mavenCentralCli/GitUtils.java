package com.yelloowstone.nf2t.mavenCentralCli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class GitUtils {
	
	public static String createGitHash(Path path) {
		try {
			final ProcessBuilder builder = new ProcessBuilder("git", "log", "-1", "--pretty=format:'%H'");
			builder.directory(path.toFile());
			final Process process = builder.start();

			// Capture stdout
			final BufferedReader standardOutReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			String s;
			final StringBuilder standardOut = new StringBuilder();
			while ((s = standardOutReader.readLine()) != null) {
				standardOut.append(s);
			}

			// Capture stderr
			final BufferedReader standardErrorReader = new BufferedReader(
					new InputStreamReader(process.getErrorStream()));
			final StringBuilder standardError = new StringBuilder();
			while ((s = standardErrorReader.readLine()) != null) {
				standardError.append(s);
			}

			final int exitCode = process.waitFor();
			if (exitCode == 0) {
				return standardOut.toString().replace("\'", "");
			} else {
				System.err.println("Could not execute git command.");
			}

		} catch (Exception e) {
			System.err.println("Could not execute git command.");
			e.printStackTrace();
		}

		return null;
	}
}
