package com.yelloowstone.nf2t.cli;

import java.util.Collections;
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
		final Map<Integer, FlowFilePackageVersion> flowFilePackageVersions = new HashMap<>();
		flowFilePackageVersions.put(1, new FlowFilePackageVersion(StandardFlowFileMediaType.VERSION_1.getMediaType(), ".flowfilev1", () -> new FlowFileUnpackagerV1(), () -> new FlowFilePackagerV1()));
		flowFilePackageVersions.put(2, new FlowFilePackageVersion(StandardFlowFileMediaType.VERSION_2.getMediaType(), ".flowfilev2", () -> new FlowFileUnpackagerV2(), () -> new FlowFilePackagerV2()));
		flowFilePackageVersions.put(3, new FlowFilePackageVersion(StandardFlowFileMediaType.VERSION_3.getMediaType(), ".flowfilev3", () -> new FlowFileUnpackagerV3(), () -> new FlowFilePackagerV3()));
		this.flowFilePackageVersions = Collections.unmodifiableMap(flowFilePackageVersions);
	}
	
	public FlowFilePackageVersion get(int version) {
		return this.flowFilePackageVersions.get(version);
	}
}
