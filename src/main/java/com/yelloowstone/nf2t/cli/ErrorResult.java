package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResult {
	@JsonProperty("exception")
	private final String exception;
	@JsonProperty("path")
	private final Path path;
	
	public ErrorResult(final Exception exception, @JsonProperty("path") final Path path) {
		this(exception.getMessage(), path);

	}
	
	@JsonCreator
	public ErrorResult(@JsonProperty("exception") final String exception, @JsonProperty("path") final Path path) {
		this.exception = exception;
		this.path = path;
	}
	
	public String getException() {
		return exception;
	}
	
	public Path getPath() {
		return path;
	}
	
	
}
