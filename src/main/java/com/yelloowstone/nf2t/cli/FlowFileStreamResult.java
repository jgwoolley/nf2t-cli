package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class FlowFileStreamResult {

	private static final String UNPACKAGE_DESCRIPTION = " For the unpackage command, ";
	private static final String PACKAGE_DESCRIPTION = " For the package command, ";

	protected static final String EXTENSION_DESCRIPTION = "This is the extension  ";
	protected static final String EXTENSION_UNPACKAGE_DESCRIPTION = UNPACKAGE_DESCRIPTION
			+ "it is used to determine whether or not an incoming file is a FlowFileStream of the specified version.";
	protected static final String EXTENSION_PACKAGE_DESCRIPTION = PACKAGE_DESCRIPTION
			+ "it will be used to generate the outgoing extension of the specified version.";
	@JsonPropertyDescription(EXTENSION_DESCRIPTION + EXTENSION_UNPACKAGE_DESCRIPTION + EXTENSION_PACKAGE_DESCRIPTION)
	@JsonProperty("extension")
	final private String extension;

	protected static final String VERSION_DESCRIPTION = "This is the FlowFileStream version of the ";
	@JsonPropertyDescription(VERSION_DESCRIPTION + "file.")
	@JsonProperty("version")
	final private int version;

	protected static final String UUID_DESCRIPTION = UNPACKAGE_DESCRIPTION
			+ "Will make all unpackaged content filename(s) UUIDs, to prevent clobering.";

	@JsonPropertyDescription(UUID_DESCRIPTION)
	@JsonProperty("uuidFilenames")
	private boolean uuidFilenames;

	protected static final String INPUTPATH_UNPACKAGE_DESCRIPTION = UNPACKAGE_DESCRIPTION
			+ "a single FlowFileStream file, a directory of FlowFileStream files, a directory containing .ZIP or .TAR.GZ files containing FlowFileStream(s), or a single .ZIP or .TAR.GZ file containing FlowFileStream(s).";
	protected static final String INPUTPATH_PACKAGE_DESCRIPTION = PACKAGE_DESCRIPTION
			+ "a directory or file containing FlowFile content.";
	@JsonPropertyDescription("The input path. " + INPUTPATH_UNPACKAGE_DESCRIPTION + INPUTPATH_PACKAGE_DESCRIPTION
			+ "Represented by Java URI format.")
	@JsonProperty("inputPath")
	final private Path inputPath;

	protected static final String OUTPUTPATH_UNPACKAGE_DESCRIPTION = UNPACKAGE_DESCRIPTION
			+ "a directory containing the FlowFile content.";
	protected static final String OUTPUTPATH_PACKAGE_DESCRIPTION = PACKAGE_DESCRIPTION
			+ "a directory where a FlowFileStream will be created containing all the incoming content with a default filename, or the full path to the FlowFileStream.";

	@JsonPropertyDescription("The output path. " + OUTPUTPATH_UNPACKAGE_DESCRIPTION + OUTPUTPATH_PACKAGE_DESCRIPTION
			+ "Represented by Java URI format.")
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

	protected static final String RESULTS_PATH_DESCRIPTION = "An optional field to specify an output for the results JSON.";
	
	@JsonPropertyDescription(RESULTS_PATH_DESCRIPTION)
	@JsonProperty("resultsPath")
	final private Path resultsPath;
	
	protected static final String DEFAULT_ATTRIBUTES_DESCRIPTION = "Attributes to set on outgoing FlowFile(s).";
	
	@JsonPropertyDescription(DEFAULT_ATTRIBUTES_DESCRIPTION)
	@JsonProperty("defaultAttributes")
	final Map<String, String> defaultAttributes;
	
	protected static final String KEEP_ATTRIBUTES_DESCRIPTION = "Keep default attributes generated by nf2t-cli.";
	
	@JsonPropertyDescription(KEEP_ATTRIBUTES_DESCRIPTION)
	@JsonProperty("keepAttributes")
	final boolean keepAttributes;
	
	@JsonCreator
	public FlowFileStreamResult(@JsonProperty("version") final int version, @JsonProperty("extension") String extension,
			@JsonProperty("uuidFilenames") final boolean uuidFilenames, @JsonProperty("inputPath") final Path inputPath,
			@JsonProperty("outputPath") final Path outputPath, @JsonProperty("resultsPath") Path resultsPath,
			@JsonProperty("unixTime") final long unixTime, @JsonProperty("defaultAttributes") Map<String, String> defaultAttributes, @JsonProperty("keepAttributes") boolean keepAttributes) {
		this.version = version;
		this.extension = extension;
		this.uuidFilenames = uuidFilenames;
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.resultsPath = resultsPath;
		this.unixTime = unixTime;
		this.defaultAttributes = defaultAttributes;
		this.keepAttributes = keepAttributes;
	}

	public int getVersion() {
		return version;
	}

	public String getExtension() {
		return extension;
	}

	public boolean isUuidFilenames() {
		return uuidFilenames;
	}

	public void setUuidFilenames(boolean uuidFilenames) {
		this.uuidFilenames = uuidFilenames;
	}

	public Path getInputPath() {
		return inputPath;
	}

	public Path getOutputPath() {
		return outputPath;
	}

	public Path getResultsPath() {
		return resultsPath;
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
