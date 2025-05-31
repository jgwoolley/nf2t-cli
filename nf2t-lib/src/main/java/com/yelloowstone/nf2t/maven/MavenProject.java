package com.yelloowstone.nf2t.maven;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import com.yelloowstone.nf2t.jars.JarDetails;

//TODO: Combine with JarDetails
public class MavenProject {
	public static final String MAVEN_CENTRAL_ARTIFACT_KEY = "Maven Central Zip";
	
	private final boolean isPicocli;
	private final Instant buildTime;
	private final Path projectPath;
	private final Path artifactPath;
	private final Model mavenModel;
	private final MavenCoordinate baseCoordinate;
	private final MavenJarArtifact baseJarArtifact;
	private final String gitHash;


	public MavenProject(final boolean isPicocli, final Instant buildTime, final Path projectPath, final Path artifactPath,
			final Model mavenModel, final MavenCoordinate baseCoordinate, final MavenJarArtifact baseJarArtifact, final String gitHash) {
		super();
		this.isPicocli = isPicocli;
		this.artifactPath = artifactPath;
		this.buildTime = buildTime;
		this.projectPath = projectPath;
		this.mavenModel = mavenModel;
		this.baseCoordinate = baseCoordinate;
		this.baseJarArtifact = baseJarArtifact;
		this.gitHash = gitHash;
	}

	public Instant getBuildTime() {
		return buildTime;
	}

	public Path getProjectPath() {
		return projectPath;
	}

	public Model getMavenModel() {
		return mavenModel;
	}
	
	public MavenCoordinate getBaseCoordinate() {
		return baseCoordinate;
	}
	
	public MavenJarArtifact getBaseJarArtifact() {
		return baseJarArtifact;
	}

	public String getGitHash() {
		return gitHash;
	}

	public Path getArtifactPath() {
		return artifactPath;
	}
	
	public boolean isPicocli() {
		if(this.isPicocli) {
			MavenJarArtifact baseJarArtifact = getBaseJarArtifact();
			JarDetails jarDetails = baseJarArtifact.getJarDetails();
			String mainClass = jarDetails.getMainClass();
			return mainClass != null;
		}
		
		return false;
	}
	
	public String getMavenCentralZipEntryPrefix() {
		final MavenCoordinate baseCoordinate = this.getBaseCoordinate();
		
		final StringBuilder sb = new StringBuilder("/");
		for (final String name : baseCoordinate.getGroupId().split("\\.")) {
			sb.append(name);
			sb.append("/");
		}
		sb.append(baseCoordinate.getArtifactId());
		sb.append("/");

		sb.append(baseCoordinate.getVersion());
		sb.append("/");

		return sb.toString();
	}
	
	public String getCommitURL() {
		final Object result = getMavenModel().getProperties().get("yelloowstone.commitURL");
		if (result == null) {
			return null;
		}

		return result.toString();
	}
	
	public String getProjectName() {
		return mavenModel.getName() == null ? mavenModel.getArtifactId() : mavenModel.getName();
	}

	public boolean hasDependency(final String groupId, final String artifactId, final String version) {
		for (final Dependency dependency : getMavenModel().getDependencies()) {
			if (groupId != null && !dependency.getGroupId().equals(groupId)) {
				continue;
			}

			if (artifactId != null && !dependency.getArtifactId().equals(artifactId)) {
				continue;
			}

			if (version != null && !dependency.getVersion().equals(version)) {
				continue;
			}

			return true;
		}

		return false;
	}

	public Map<String, Object> getDataModel(final Map<String, String> buildArtifactsResult) {
		final Map<String, Object> dataModel = new HashMap<>();
		dataModel.put("mavenProject", this);

		final Model artifact = this.getMavenModel();

		final List<MavenProjectProperty> properties = new ArrayList<>();
		properties.add(new MavenProjectProperty("Build Time", this.getBuildTime().toString()));

		if (this.getGitHash() != null) {
			if (this.getCommitURL() == null) {
				properties.add(new MavenProjectProperty("Git Hash", this.getGitHash()));
			} else {
				properties.add(new MavenProjectProperty("Git Hash", this.getGitHash(),
						this.getCommitURL() + this.getGitHash()));
			}
		}

		properties.add(new MavenProjectProperty("Maven Coordinate", getBaseCoordinate().getCoordinate()));
		properties.add(new MavenProjectProperty("Maven Artifact Name", artifact.getName()));
		properties.add(new MavenProjectProperty("Maven Artifact Id", artifact.getArtifactId()));
		properties.add(new MavenProjectProperty("Maven Artifact Group Id", artifact.getGroupId()));
		properties.add(new MavenProjectProperty("Maven Artifact Version", artifact.getVersion()));
		properties.add(new MavenProjectProperty("Maven Artifact Packaging", artifact.getPackaging()));
		properties.add(new MavenProjectProperty("JavaDocs URL", "./javadoc/index.html", "./javadoc/index.html"));

		if (isPicocli()) {
			properties.add(new MavenProjectProperty("Man Page URL", "./man/index.html", "./man/index.html"));
		}

		if (buildArtifactsResult != null) {
			buildArtifactsResult.entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).forEach(x -> {
				properties.add(new MavenProjectProperty(x.getKey(), "./artifacts/" + x.getValue(),
						"./artifacts/" + x.getValue()));
			});
		}

		dataModel.put("properties", properties);

		return dataModel;
	}

	@Override
	public String toString() {
		return "MavenProject [projectPath=" + projectPath + ", MavenCoordinate=" + mavenModel.getArtifactId() + "]";
	}
}
