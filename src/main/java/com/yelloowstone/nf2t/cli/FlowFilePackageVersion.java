package com.yelloowstone.nf2t.cli;

import java.util.function.Supplier;

import org.apache.nifi.util.FlowFilePackager;
import org.apache.nifi.util.FlowFileUnpackager;

public class FlowFilePackageVersion {
	private final String mimetype;
	private final String fileExtension;
	private final Supplier<FlowFileUnpackager> unpackager;
	private final Supplier<FlowFilePackager> packager;
	
	public FlowFilePackageVersion(String mimetype, String fileExtension,
			Supplier<FlowFileUnpackager> unpackager, Supplier<FlowFilePackager> packager) {
		this.mimetype = mimetype;
		this.fileExtension = fileExtension;
		this.unpackager = unpackager;
		this.packager = packager;
	}

	public String getMimetype() {
		return mimetype;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public FlowFileUnpackager getUnpackager() {
		return unpackager.get();
	}

	public FlowFilePackager getPackager() {
		return packager.get();
	}
}
