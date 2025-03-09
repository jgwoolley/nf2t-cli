package com.yelloowstone.nf2t.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MockFlowFileStreamContainer {
	private final Path containerPath;
	private final List<MockFlowFileStream> flowFileStreams;
	
	public MockFlowFileStreamContainer(Path containerPath) {
		super();
		this.containerPath = containerPath;
		this.flowFileStreams = new ArrayList<>();
	}

	public Path getContainerPath() {
		return containerPath;
	}

	public List<MockFlowFileStream> getFlowFileStreams() {
		return flowFileStreams;
	}

	public int getFlowFileSize() {
		return flowFileStreams.stream().mapToInt(x -> x.getFlowFileSize()).sum();
	}
	
	public MockFlowFileStream addFlowFileStream(final String filename) {
		MockFlowFileStream result = new MockFlowFileStream(filename);
		flowFileStreams.add(result);
		return result;
	}
	
	@Override
	public String toString() {
		return "MockFlowFileStreamContainer [containerPath=" + containerPath + ", flowFileStreams=" + flowFileStreams.size() + ", flowFiles=" + getFlowFileSize() + "]";
	}
}
