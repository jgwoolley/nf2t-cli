package com.yelloowstone.nf2t.jars;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

public class ModifiableJarDetails implements JarDetails {
	private NarDetails narDetails;
	private Manifest manifest;
	private Map<String,ModifiableMavenDescriptorDetails> mavenPoms = new HashMap<>();
	
	@Override
	public NarDetails getNarDetails() {
		return this.narDetails;
	}
		
	public void setNarDetails(NarDetails narDetails) {
		this.narDetails = narDetails;
	}

	@Override
	public Manifest getManifest() {
		return this.manifest;
	}
	
	public void setManifest(final Manifest manifest) {
		this.manifest = manifest;
	}

	public ModifiableMavenDescriptorDetails addMavenPomDetails(final String groupId, final String artifactId) {		
		return this.mavenPoms.computeIfAbsent(groupId + ":" + artifactId, x -> new ModifiableMavenDescriptorDetails(groupId, artifactId));
	}
	
	public UnmodifiableJarDetails build() {
		return new UnmodifiableJarDetails(getNarDetails(), getManifest(), getMavenDescriptorPoms());
	}

	@Override
	public List<MavenDescriptorDetails> getMavenDescriptorPoms() {
		return List.copyOf(this.mavenPoms.values());
	}

}
