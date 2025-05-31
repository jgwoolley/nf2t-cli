package com.yelloowstone.nf2t.maven;

import java.nio.file.Path;

import com.yelloowstone.nf2t.jars.JarDetails;
import com.yelloowstone.nf2t.jars.UnmodifiableJarDetails;

public class MavenJarArtifact {
	private final Path path;
	private final MavenCoordinate coordinate;
	private final UnmodifiableJarDetails jarDetails;
	
	public MavenJarArtifact(final Path path, final MavenCoordinate coordinate, final UnmodifiableJarDetails jarDetails) {
		super();
		this.path = path;
		this.coordinate = coordinate;
		this.jarDetails = jarDetails;
	}
	
	public Path getPath() {
		return path;
	}
	
	public MavenCoordinate getCoordinate() {
		return coordinate;
	}
	
	public JarDetails getJarDetails() {
		return jarDetails;
	}
}
