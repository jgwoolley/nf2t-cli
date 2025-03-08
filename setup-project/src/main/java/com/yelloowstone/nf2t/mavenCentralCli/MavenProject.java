package com.yelloowstone.nf2t.mavenCentralCli;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


public class MavenProject {
	private final Instant buildTime;
	private final Path projectPath;
	private final MavenArtifact mavenArtifact;
	private final String gitHash;

	public MavenProject(Instant buildTime, Path projectPath, MavenArtifact mavenArtifact, String gitHash) {
		super();
		this.buildTime = buildTime;
		this.projectPath = projectPath;
		this.mavenArtifact = mavenArtifact;
		this.gitHash = gitHash;
	}

	public Instant getBuildTime() {
		return buildTime;
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

	public Map<String, Object> getDataModel() {
		final Map<String, Object> dataModel = new HashMap<>();
		dataModel.put("mavenProject", this);

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
