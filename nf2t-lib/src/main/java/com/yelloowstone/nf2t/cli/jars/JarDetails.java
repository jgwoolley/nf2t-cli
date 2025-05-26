package com.yelloowstone.nf2t.cli.jars;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

public class JarDetails implements iJarDetails {
	private final NarDetails narDetails;
	private final Manifest manifestDetails;
	private final List<MavenPomDetails> mavenDescriptorPoms;
	
	public JarDetails(final NarDetails narDetails, final Manifest manifest, final List<MavenPomDetails> mavenPoms) {
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
	public List<MavenPomDetails> getMavenDescriptorPoms() {
		return this.mavenDescriptorPoms;
	}
	
	public static class Builder implements iJarDetails {
		private NarDetails narDetails;
		private Manifest manifest;
		private Map<String,MavenPomDetails> mavenPoms = new HashMap<>();
		
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

		public MavenPomDetails addMavenPomDetails(final String groupId, final String artifactId) {		
			return this.mavenPoms.computeIfAbsent(groupId + ":" + artifactId, x -> new MavenPomDetails(groupId, artifactId));
		}
		
		public iJarDetails build() {
			return new JarDetails(getNarDetails(), getManifest(), getMavenDescriptorPoms());
		}

		@Override
		public List<MavenPomDetails> getMavenDescriptorPoms() {
			return List.copyOf(this.mavenPoms.values());
		}

	}
}
