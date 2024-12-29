package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Result {
	final int version;
	final Path inputPath;
	final Path outputPath;
	final long unixTime;
	final List<ErrorResult> errors = new ArrayList<>();
	final List<SuccessResult> outputFiles = new ArrayList<>();
	
	public Result(final int version, final Path inputPath, final Path outputPath, final long unixTime) {
		this.version = version;
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.unixTime = unixTime;
	}

	public int getVersion() {
		return version;
	}

	public Path getInputPath() {
		return inputPath;
	}

	public Path getOutputPath() {
		return outputPath;
	}

	public long getUnixTime() {
		return unixTime;
	}

	public List<ErrorResult> getErrors() {
		return errors;
	}

	public List<SuccessResult> getOutputFiles() {
		return outputFiles;
	}
}
