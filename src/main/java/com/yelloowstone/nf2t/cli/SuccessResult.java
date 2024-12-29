package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;

public class SuccessResult {
	private final Path flowFilePath;
	private final Path contentPath;
	private final Path attributesPath;
	private final long contentSize;
	
	public SuccessResult(final Path flowFilePath, final Path contentPath, final Path attributesPath, final long size) {
		this.flowFilePath = flowFilePath;
		this.contentPath = contentPath;
		this.attributesPath = attributesPath;
		this.contentSize = size;
	}

	public Path getFlowFilePath() {
		return flowFilePath;
	}

	public Path getContentPath() {
		return contentPath;
	}

	public Path getAttributesPath() {
		return attributesPath;
	}

	public long getContentSize() {
		return contentSize;
	}
}
