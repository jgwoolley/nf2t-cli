package com.yelloowstone.nf2t.cli;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class FlowFileErrorResult {
	@JsonPropertyDescription("A text Representation of the Exception thrown.")
	@JsonProperty("exception")
	private final String exception;
	
	@JsonProperty("exceptionStack") final List<String> exceptionStack;
	
	@JsonPropertyDescription("The path of the file that threw an error, represented by Java URI format.")
	@JsonProperty("path")
	private final SourceFile path;
	
	public FlowFileErrorResult(final Exception exception, @JsonProperty("path") final SourceFile path) {
		this(Arrays.asList(exception.getStackTrace()).stream().map(x -> x.toString()).collect(Collectors.toList()), exception.getMessage(), path);

	}
	
	@JsonCreator
	public FlowFileErrorResult(@JsonProperty("exceptionStack") final List<String> exceptionStack, @JsonProperty("exception") final String exception, @JsonProperty("path") final SourceFile path) {
		this.exceptionStack = exceptionStack;
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
