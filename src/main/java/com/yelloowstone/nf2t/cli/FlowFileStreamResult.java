package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FlowFileStreamResult {
	@JsonProperty("version")
	final private int version;
	@JsonProperty("inputPath")
	final private Path inputPath;
	@JsonProperty("outputPath")
	private Path outputPath;
	@JsonProperty("unixTime")
	final private long unixTime;
	@JsonProperty("errors")
	final private List<ErrorResult> errors = new ArrayList<>();
	@JsonProperty("outputFiles")
	final private List<FlowFileResult> outputFiles = new ArrayList<>();

	@JsonCreator
	public FlowFileStreamResult(@JsonProperty("version") final int version, @JsonProperty("inputPath") final Path inputPath, @JsonProperty("outputPath") final Path outputPath, @JsonProperty("unixTime") final long unixTime) {
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

	public void setOutputPath(Path outputPath) {
		this.outputPath = outputPath;
	}
	
	public long getUnixTime() {
		return unixTime;
	}

	public List<ErrorResult> getErrors() {
		return errors;
	}

	public List<FlowFileResult> getOutputFiles() {
		return outputFiles;
	}
}
