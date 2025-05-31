package com.yelloowstone.nf2t.jars;

import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

// TODO: Combine with MavenProject
public class UnmodifiableJarDetails implements JarDetails {
	private final NarDetails narDetails;
	private final Manifest manifestDetails;
	private final List<MavenDescriptorDetails> mavenDescriptorPoms;
	
	public UnmodifiableJarDetails(final NarDetails narDetails, final Manifest manifest, final List<MavenDescriptorDetails> mavenPoms) {
		this.narDetails = narDetails;
		this.manifestDetails = manifest;
		this.mavenDescriptorPoms = Collections.unmodifiableList(mavenPoms);
	}
	
	@Override
	public NarDetails getNarDetails() {
		return this.narDetails;
	}

	@Override
	public Manifest getManifest() {
		return this.manifestDetails;
	}
	
	@Override
	public List<MavenDescriptorDetails> getMavenDescriptorPoms() {
		return this.mavenDescriptorPoms;
	}
}
