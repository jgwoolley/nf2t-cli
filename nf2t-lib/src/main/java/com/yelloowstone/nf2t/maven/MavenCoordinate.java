package com.yelloowstone.nf2t.maven;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Model;

public class MavenCoordinate {
	public static final String[] DIGEST_NAMES = new String[] {"MD5", "SHA1", "SHA256", "SHA512"};
	
	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String packaging;
	private final String classifier;
	
	public MavenCoordinate(final String groupId, final String artifactId, final String version, final String packaging, final String classifier) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packaging = packaging;
		this.classifier = classifier;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getPackaging() {
		return packaging;
	}

	public String getClassifier() {
		return classifier;
	}
	
	public String getCoordinate() {
		final String groupId = getGroupId();
		final String artifactId = getArtifactId();
		final String version = getVersion();
		final String packaging = getPackaging();
		final String classifier = getClassifier();
		
		final StringBuilder builder = new StringBuilder();
		
		if(groupId == null) {
			throw new IllegalArgumentException("Cannot generate Maven Coordinate when groupId is null");
		}
		builder.append(groupId);
		builder.append(':');
		
		if(artifactId == null) {
			throw new IllegalArgumentException("Cannot generate Maven Coordinate when groupId is null");
		}
		builder.append(artifactId);
		builder.append(':');
		
		if(packaging == null) {
			if(classifier == null) {
				throw new IllegalArgumentException("Cannot generate Maven Coordinate when packaging is null, and classifier is non-null");
			}
		} else {
			builder.append(packaging);
			builder.append(':');
		}
		
		if(classifier != null) {
			builder.append(classifier);
			builder.append(':');
		}
		
		if(version == null) {
			throw new IllegalArgumentException("Cannot generate Maven Coordinate when version is null");
		}
		builder.append(version);
		
		return builder.toString();
	};

	public String getFileName() {
		final StringBuilder sb = new StringBuilder();
		final String artifactId = getArtifactId();
		final String version = getVersion();
		final String classifier = getClassifier();
		final String packaging = getPackaging();
		
		if(artifactId == null) {
			throw new IllegalArgumentException("Cannot generate Maven Coordinate when artifactId is null");
		}
		sb.append(artifactId);
		sb.append('-');
		
		if(version == null) {
			throw new IllegalArgumentException("Cannot generate Maven Coordinate when version is null");
		}
		sb.append(version);
		
		if(classifier != null) {
			sb.append('-');
			sb.append(classifier);
		}
		
		sb.append('.');
		
		if(packaging == null) {
			throw new IllegalArgumentException("Cannot generate Maven Coordinate when packaging is null");
		}
		sb.append(packaging);

		return sb.toString();
	}

	public Path resolveFilePath(Path artifactsPath) {
		return artifactsPath.resolve(getFileName());
	}
	
	public String getMavenCentralZipEntryPrefix() {
		final StringBuilder sb = new StringBuilder("/");
		for (final String name : this.getGroupId().split("\\.")) {
			sb.append(name);
			sb.append("/");
		}
		sb.append(this.getArtifactId());
		sb.append("/");

		sb.append(this.getVersion());
		sb.append("/");

		return sb.toString();
	}

	public MavenCoordinate getBaseArtifact() {
		final String groupId = getGroupId();
		final String artifactId = getArtifactId();
		final String version = getVersion();
		final String packaging = getPackaging();
		
		return new MavenCoordinate(groupId, artifactId, version, packaging, null);
	}
	
	public MavenCoordinate getDocsJar() {
		final String groupId = getGroupId();
		final String artifactId = getArtifactId();
		final String version = getVersion();
		
		return new MavenCoordinate(groupId, artifactId, version, "jar", "javadoc");
	}

	public MavenCoordinate getSourcesJar() {
		final String groupId = getGroupId();
		final String artifactId = getArtifactId();
		final String version = getVersion();
		
		return new MavenCoordinate(groupId, artifactId, version, "jar", "sources");
	}

	public MavenCoordinate getEffectivePom() {
		final String groupId = getGroupId();
		final String artifactId = getArtifactId();
		final String version = getVersion();
		
		return new MavenCoordinate(groupId, artifactId, version, "pom", null);
	}
	
	public MavenCoordinate getMavenCentralZip() {
		final String groupId = getGroupId();
		final String artifactId = getArtifactId();
		final String version = getVersion();
		
		return new MavenCoordinate(groupId, artifactId, version, "zip", null);
	}
	
	public Map<String, MavenCoordinate> getMavenCentralArtifacts() {
		final Map<String, MavenCoordinate> artifacts = new HashMap<>();
		
		artifacts.put("Base Artifact", getBaseArtifact());
		artifacts.put("JavaDoc", getDocsJar());
		artifacts.put("Source", getSourcesJar());
		artifacts.put("Pom", getEffectivePom());
		
		return artifacts;
	}
	
	public static MavenCoordinate readModel(final Model mavenModel) {
		final String groupId = mavenModel.getGroupId() == null ? mavenModel.getParent().getGroupId() : mavenModel.getGroupId();
		final String artifactId = mavenModel.getArtifactId();
		final String version = mavenModel.getGroupId() == null ? mavenModel.getParent().getVersion() : mavenModel.getVersion();
		final String packaging = mavenModel.getPackaging();
		
		return new MavenCoordinate(groupId, artifactId, version, packaging, null);
	}

	public static MavenCoordinate parseCoordinate(final String mavenCoordinate) {
		final String[] split = mavenCoordinate.split("\\.");
		
		final String groupId = split[0];
		final String artifactId = split[1];
		
		if(split.length == 2) {
			return new MavenCoordinate(groupId, artifactId, null, null, null);
		}
		
		else if(split.length == 3) {
			final String version = split[2];
			
			return new MavenCoordinate(groupId, artifactId, version, null, null);
		} else if(split.length == 4) {
			final String packaging = split[2];
			final String version = split[3];
			
			return new MavenCoordinate(groupId, artifactId, version, packaging, null);
		} else if(split.length == 5) {
			final String packaging = split[2];
			final String classifier = split[3];
			final String version = split[4];
			
			return new MavenCoordinate(groupId, artifactId, version, packaging, classifier);
		}
		
		throw new IllegalArgumentException("Must have between 2 and 5 elements split by period.");
	}
}
