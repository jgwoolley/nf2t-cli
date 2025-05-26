package com.yelloowstone.nf2t.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MockFlowFileStream {
	private String filename;
	private List<MockFlowFile> flowFiles;
	
	public MockFlowFileStream(final String filename) {
		super();
		this.filename = filename;
		this.flowFiles = new ArrayList<>();
	}
	
	public String getFilename() {
		return filename;
	}
	
	public List<MockFlowFile> getFlowFiles() {
		return flowFiles;
	}
	
	public int getFlowFileSize() {
		return flowFiles.size();
	}
	
	public MockFlowFile addFlowFile(final byte[] flowFileContent, final Map<String, String> flowFileAttributes) {
		MockFlowFile result = new MockFlowFile(flowFileAttributes, flowFileContent);
		flowFiles.add(result);
		return result;
	}
	
	@Override
	public String toString() {
		return "MockFlowFileStream [filename=" + filename + ", flowFiles=" + getFlowFileSize() + "]";
	}
	
	
}
