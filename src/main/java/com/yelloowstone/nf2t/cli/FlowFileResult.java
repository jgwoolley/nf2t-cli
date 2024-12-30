package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class FlowFileResult {
	
	@JsonPropertyDescription("Represents the location of the Packaged FlowFile.")
	@JsonProperty("flowFilePath")
	private final Path flowFilePath;
	
	@JsonPropertyDescription("Represents the location of the FlowFile Content.")
	@JsonProperty("contentPath")
	private Path contentPath;
	
	@JsonPropertyDescription("Represents the FlowFile Attributes.")
	@JsonProperty("attributes")
	private final Map<String,String> attributes;
	
	@JsonPropertyDescription("Represents the size of the FlowFile Content.")
	@JsonProperty("contentSize")
	private final long contentSize;
	
	@JsonCreator
	public FlowFileResult(@JsonProperty("flowFilePath") final Path flowFilePath, @JsonProperty("contentPath") final Path contentPath, @JsonProperty("attributes") final Map<String,String> attributes, @JsonProperty("contentSize") final long size) {
		this.flowFilePath = flowFilePath;
		this.contentPath = contentPath;
		this.attributes = attributes;
		this.contentSize = size;
	}

	public Path getFlowFilePath() {
		return flowFilePath;
	}

	public Path getContentPath() {
		return contentPath;
	}

	public Map<String,String> getAttributes() {
		return attributes;
	}

	public long getContentSize() {
		return contentSize;
	}

	public void setContentPath(Path contentPath) {
		this.contentPath = contentPath;
	}
}
