package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;

public class ErrorResult {
	private final String exception;
	private final Path path;
	
	public ErrorResult(final Exception exception, final Path path) {
		this.exception = exception.getMessage();
		this.path = path;
	}
	
	public String getException() {
		return exception;
	}
	
	public Path getPath() {
		return path;
	}
	
	
}
