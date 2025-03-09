package com.yelloowstone.nf2t.cli;

import java.util.Map;

public class MockFlowFile {
	private Map<String,String> flowFileAttributes;
	private byte[] flowFileContent;
	
	public MockFlowFile(Map<String, String> flowFileAttributes, byte[] flowFileContent) {
		super();
		this.flowFileAttributes = flowFileAttributes;
		this.flowFileContent = flowFileContent;
	}

	public Map<String, String> getFlowFileAttributes() {
		return flowFileAttributes;
	}

	public byte[] getFlowFileContent() {
		return flowFileContent;
	}
	
	public long getContentSize() {
		return flowFileContent.length;
	}

	@Override
	public String toString() {
		return "MockFlowFile [flowFileAttributes=" + flowFileAttributes + "]";
	}
}
