package com.yelloowstone.nf2t.cli;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class FlowFileResult {
	
	@JsonPropertyDescription("Represents the location of the Packaged FlowFile, represented by Java URI format.")
	@JsonProperty("flowFilePath")
	private final URI flowFilePath;
	
	@JsonPropertyDescription("Represents the location of the FlowFile Content, represented by Java URI format.")
	@JsonProperty("contentPath")
	private URI contentPath;
	
	@JsonPropertyDescription("Represents the FlowFile Attributes.")
	@JsonProperty("attributes")
	private final Map<String,String> attributes;
	
	@JsonPropertyDescription("Represents the size of the FlowFile Content.")
	@JsonProperty("contentSize")
	private final long contentSize;
	
	@JsonCreator
	public FlowFileResult(@JsonProperty("flowFilePath") final URI flowFilePath, @JsonProperty("contentPath") final URI contentPath, @JsonProperty("attributes") final Map<String,String> attributes, @JsonProperty("contentSize") final long size) {
		this.flowFilePath = flowFilePath;
		this.contentPath = contentPath;
		this.attributes = attributes;
		this.contentSize = size;
	}

	public URI getFlowFilePath() {
		return flowFilePath;
	}

	public URI getContentPath() {
		return contentPath;
	}

	public Map<String,String> getAttributes() {
		return attributes;
	}

	public long getContentSize() {
		return contentSize;
	}

	public void setContentPath(URI contentPath) {
		this.contentPath = contentPath;
	}
}
