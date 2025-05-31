package com.yelloowstone.nf2t.jars;

import java.util.List;
import java.util.jar.Manifest;

public interface JarDetails {
	public NarDetails getNarDetails();
	public Manifest getManifest();
	public List<MavenDescriptorDetails> getMavenDescriptorPoms();
	
	
	public default String getMainManifestAttributeValue(String name) {
		final String mainClass = this.getManifest().getMainAttributes().getValue(name);
		
		return mainClass;
	}
	
	public default String getMainClass() {
		return this.getMainManifestAttributeValue("Main-Class");
	}
}
