package com.yelloowstone.nf2t.jars;

import java.util.Map;

public interface MavenDescriptorDetails {
	
	public String getGroupId();
	
	public String getArtifactId();
	
	public Map<String, String> getProperties();
}
