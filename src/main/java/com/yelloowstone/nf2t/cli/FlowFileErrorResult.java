package com.yelloowstone.nf2t.cli;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class FlowFileErrorResult {
	@JsonPropertyDescription("A text Representation of the Exception thrown.")
	@JsonProperty("exception")
	private final String exception;
	
	@JsonPropertyDescription("The path of the file that threw an error, represented by Java URI format.")
	@JsonProperty("path")
	private final SourceFile path;
	
	public FlowFileErrorResult(final Exception exception, @JsonProperty("path") final SourceFile path) {
		this(exception.getMessage(), path);

	}
	
	@JsonCreator
	public FlowFileErrorResult(@JsonProperty("exception") final String exception, @JsonProperty("path") final SourceFile path) {
		this.exception = exception;
		this.path = path;
	}
	
	public String getException() {
		return exception;
	}
	
	public SourceFile getPath() {
		return path;
	}
	
	
}
