package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class FlowFileStreamResult {
	public static final String PACKAGE_EXTENSION = ".pkg";
	public static final String FLOWFILE_DEFAULT_FILENAME = "flowFiles" + PACKAGE_EXTENSION;

	public static final String VERSION_DESCRIPTION = "This is the FlowFile version of the file tis packaged or unpackaged.";
	@JsonPropertyDescription(VERSION_DESCRIPTION)
	@JsonProperty("version")
	final private int version;

	public static final String INPUTPATH_DESCRIPTION = "The input path. For unpackage, a directory or file containing a FlowFileStream. For package, a directory or file containing FlowFile content.";
	@JsonPropertyDescription(INPUTPATH_DESCRIPTION)
	@JsonProperty("inputPath")
	final private Path inputPath;

	public static final String OUTPUTPATH_DESCRIPTION = "The output path. For unpackage, a directory containing the FlowFile content. For package, a directory where the \""
			+ FLOWFILE_DEFAULT_FILENAME + "\" will be created or the name of the file.";

	@JsonPropertyDescription(OUTPUTPATH_DESCRIPTION)
	@JsonProperty("outputPath")
	private Path outputPath;
	
	@JsonProperty("unixTime")
	final private long unixTime;
	
	@JsonPropertyDescription("The errors encountered while trying to unpackage or package the file(s).")
	@JsonProperty("errors")
	final private List<ErrorResult> errors = new ArrayList<>();
	
	@JsonPropertyDescription("The file(s) that were successfully unpackaged or packaged.")
	@JsonProperty("outputFiles")
	final private List<FlowFileResult> outputFiles = new ArrayList<>();

	@JsonCreator
	public FlowFileStreamResult(@JsonProperty("version") final int version,
			@JsonProperty("inputPath") final Path inputPath, @JsonProperty("outputPath") final Path outputPath,
			@JsonProperty("unixTime") final long unixTime) {
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
