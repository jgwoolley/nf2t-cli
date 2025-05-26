package com.yelloowstone.nf2t.cli.jars;

import java.util.HashMap;
import java.util.Map;

public class MavenPomDetails {
	private final String groupId;
	private final String artifactId;
	private final Map<String,String> properties;
	
	public MavenPomDetails(String groupId, String artifactId) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.properties = new HashMap<>();
	}
	
	public String getGroupId() {
		return groupId;
	}
	public String getArtifactId() {
		return artifactId;
	}

	public void addProperties(String key, String value) {
		this.properties.put(key, value);
	}

	public Map<String, String> getProperties() {
		return this.properties;
	}
	
}
