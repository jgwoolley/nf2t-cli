package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class FlowFileStreamResult {

	protected static final String VERSION_DESCRIPTION = "This is the FlowFile version of the ";
	@JsonPropertyDescription(VERSION_DESCRIPTION + "file.")
	@JsonProperty("version")
	final private int version;

	protected static final String INPUTPATH_UNPACKAGE_DESCRIPTION = " For the unpackage command, a single FlowFileStream file, a directory of FlowFileStream files, a directory containing .ZIP or .TAR.GZ files containing FlowFileStream(s), or a single .ZIP or .TAR.GZ file containing FlowFileStream(s).";
	protected static final String INPUTPATH_PACKAGE_DESCRIPTION = " For the package command, a directory or file containing FlowFile content.";	
	@JsonPropertyDescription("The input path. " + INPUTPATH_UNPACKAGE_DESCRIPTION + INPUTPATH_PACKAGE_DESCRIPTION + "Represented by Java URI format.")
	@JsonProperty("inputPath")
	final private Path inputPath;

	protected static final String OUTPUTPATH_UNPACKAGE_DESCRIPTION = " For the unpackage command, a directory containing the FlowFile content.";
	protected static final String OUTPUTPATH_PACKAGE_DESCRIPTION = " For the package command, a directory where a FlowFileStream will be created containing all the incoming content with a default filename, or the full path to the FlowFileStream.";
	
	@JsonPropertyDescription("The output path. " + OUTPUTPATH_UNPACKAGE_DESCRIPTION + OUTPUTPATH_PACKAGE_DESCRIPTION + "Represented by Java URI format.")
	@JsonProperty("outputPath")
	private Path outputPath;
	
	@JsonProperty("unixTime")
	final private long unixTime;
	
	@JsonPropertyDescription("The errors encountered while trying to unpackage or package the file(s).")
	@JsonProperty("errors")
	final private List<FlowFileErrorResult> errors = new ArrayList<>();
	
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

	public List<FlowFileErrorResult> getErrors() {
		return errors;
	}

	public List<FlowFileResult> getOutputFiles() {
		return outputFiles;
	}
}
