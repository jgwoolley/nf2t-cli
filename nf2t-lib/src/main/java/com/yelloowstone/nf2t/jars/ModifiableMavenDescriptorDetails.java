package com.yelloowstone.nf2t.jars;

import java.util.HashMap;
import java.util.Map;

public class ModifiableMavenDescriptorDetails implements MavenDescriptorDetails {
	private final String groupId;
	private final String artifactId;
	private final Map<String,String> properties;
	
	public ModifiableMavenDescriptorDetails(final String groupId, final String artifactId) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.properties = new HashMap<>();
	}
	
	@Override
	public String getGroupId() {
		return groupId;
	}
	
	@Override
	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public Map<String, String> getProperties() {
		return this.properties;
	}
	
	public void addProperties(String key, String value) {
		this.properties.put(key, value);
	}
}
