package com.yelloowstone.nf2t.cli;

import java.util.HashMap;
import java.util.Map;

import org.apache.nifi.flowfile.attributes.StandardFlowFileMediaType;
import org.apache.nifi.util.FlowFilePackagerV1;
import org.apache.nifi.util.FlowFilePackagerV2;
import org.apache.nifi.util.FlowFilePackagerV3;
import org.apache.nifi.util.FlowFileUnpackagerV1;
import org.apache.nifi.util.FlowFileUnpackagerV2;
import org.apache.nifi.util.FlowFileUnpackagerV3;

public class FlowFilePackageVersions {
	private final Map<Integer, FlowFilePackageVersion> flowFilePackageVersions;
	
	public FlowFilePackageVersions() {
		this.flowFilePackageVersions = new HashMap<>();
		this.flowFilePackageVersions.put(1, new FlowFilePackageVersion(StandardFlowFileMediaType.VERSION_1.getMediaType(), ".pkg", () -> new FlowFileUnpackagerV1(), () -> new FlowFilePackagerV1()));
		this.flowFilePackageVersions.put(2, new FlowFilePackageVersion(StandardFlowFileMediaType.VERSION_2.getMediaType(), ".pkg", () -> new FlowFileUnpackagerV2(), () -> new FlowFilePackagerV2()));
		this.flowFilePackageVersions.put(3, new FlowFilePackageVersion(StandardFlowFileMediaType.VERSION_3.getMediaType(), ".pkg", () -> new FlowFileUnpackagerV3(), () -> new FlowFilePackagerV3()));
	}
	
	public FlowFilePackageVersion get(int version) {
		return this.flowFilePackageVersions.get(version);
	}
}
