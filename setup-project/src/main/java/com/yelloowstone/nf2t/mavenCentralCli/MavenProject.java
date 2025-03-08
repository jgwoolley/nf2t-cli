package com.yelloowstone.nf2t.mavenCentralCli;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenProject {
	private final Instant buildTime;
	private final MavenProjectType projectType;
	private final Path projectPath;
	private final MavenArtifact mavenArtifact;
	private final String gitHash;
	private final String commitURL;

	public MavenProject(Instant buildTime, MavenProjectType projectType, Path projectPath, MavenArtifact mavenArtifact,
			String gitHash, String commitURL) {
		super();
		this.buildTime = buildTime;
		this.projectType = projectType;
		this.projectPath = projectPath;
		this.mavenArtifact = mavenArtifact;
		this.gitHash = gitHash;
		this.commitURL = commitURL;
	}

	public Instant getBuildTime() {
		return buildTime;
	}

	public MavenProjectType getProjectType() {
		return projectType;
	}

	public Path getProjectPath() {
		return projectPath;
	}

	public String getProjectName() {
		return getMavenArtifact().getName();
	}

	public MavenArtifact getMavenArtifact() {
		return mavenArtifact;
	}

	public String getGitHash() {
		return gitHash;
	}

	public String getCommitURL() {
		return commitURL;
	}

	public Map<String, Object> getDataModel() {
		final Map<String, Object> dataModel = new HashMap<>();
		dataModel.put("mavenProject", this);

		final MavenArtifact artifact = this.getMavenArtifact();
		
		final List<MavenProjectProperty> properties = new ArrayList<>();
		properties.add(new MavenProjectProperty("Build Time", this.getBuildTime().toString()));

		if(this.getGitHash() != null) {
			if(this.commitURL == null) {
				properties.add(new MavenProjectProperty("Git Hash", this.getGitHash()));
			} else {
				properties.add(new MavenProjectProperty("Git Hash", this.getGitHash(), this.commitURL + this.getGitHash()));
			}
		}
		
		properties.add(new MavenProjectProperty("Maven Coordinate", artifact.getCoordinate()));
		properties.add(new MavenProjectProperty("Maven Artifact Name", artifact.getName()));
		properties.add(new MavenProjectProperty("Maven Artifact Id", artifact.getArtifactId()));
		properties.add(new MavenProjectProperty("Maven Artifact Group Id", artifact.getGroupId()));
		properties.add(new MavenProjectProperty("Maven Artifact Version", artifact.getVersion()));
		properties.add(new MavenProjectProperty("Maven Artifact Packaging", artifact.getPackaging()));
		properties.add(new MavenProjectProperty("JavaDocs URL", "./javadoc/index.html", "./javadoc/index.html"));
		
		if(this.projectType == MavenProjectType.PICOCLI) {
			properties.add(new MavenProjectProperty("Man Page URL", "./man/index.html", "./man/index.html"));
		}
		
		this.getMavenArtifact().getMavenCentralArtifactNames().entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).forEach(x -> {
			properties.add(new MavenProjectProperty(x.getKey(), "./artifacts/" + x.getValue(), "./artifacts/" + x.getValue()));
		});

		properties.add(new MavenProjectProperty("Maven Central Zip", "./artifacts/" + artifact.getMavenCentralArtifactName(), "./artifacts/" + artifact.getMavenCentralArtifactName()));
		
		dataModel.put("properties", properties);

		return dataModel;
	}

	public Path getTargetPath() {
		return projectPath.resolve("target");
	}

	@Override
	public String toString() {
		return "MavenProject [projectPath=" + projectPath + ", mavenArtifact=" + mavenArtifact + "]";
	}
}
