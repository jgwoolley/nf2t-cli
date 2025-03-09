package com.yelloowstone.nf2t.mavenCentralCli;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

public class MavenProject {
	private final Instant buildTime;
	private final Path projectPath;
	private final Model mavenArtifact;
	private final String gitHash;

	public MavenProject(Instant buildTime, Path projectPath, Model mavenArtifact,
			String gitHash) {
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

	public Model getMavenArtifact() {
		return mavenArtifact;
	}

	public String getGitHash() {
		return gitHash;
	}

	public String getCommitURL() {
		final Object result = getMavenArtifact().getProperties().get("yelloowstone.commitURL");
		if(result == null) {
			return null;
		}
		
		return result.toString();
	}

	public String getFileName(String postfix) {
		return getMavenArtifact().getArtifactId() + "-" + getMavenArtifact().getVersion() + postfix;
	}

	public String getCoordinate() {
		return getMavenArtifact().getGroupId() + ":" + getMavenArtifact().getArtifactId() + ":" + getMavenArtifact().getPackaging() + ":" + getMavenArtifact().getVersion();
	};

	public String getMavenCentralZipEntryPrefix() {
		final StringBuilder sb = new StringBuilder("/");
		for (final String name : this.getMavenArtifact().getGroupId().split("\\.")) {
			sb.append(name);
			sb.append("/");
		}
		sb.append(this.getMavenArtifact().getArtifactId());
		sb.append("/");

		sb.append(this.getMavenArtifact().getVersion());
		sb.append("/");

		return sb.toString();
	}
	
	public String getJarFileName() {
		return getFileName(".jar");
	};

	public String getJavaDocFileName() {
		return getFileName("-javadoc.jar");
	};

	public String getSourcesFileName() {
		return getFileName("-sources.jar");
	};

	public String getEffectivePomFileName() {
		return getFileName(".pom");
	};
	
	public Map<String, String> getMavenCentralArtifactNames() {
		return Map.of("Jar", getJarFileName(), "JavaDoc", getJavaDocFileName(), "Source", getSourcesFileName(), "Pom", getEffectivePomFileName());
	}
		
	
	public String getMavenCentralArtifactName() {
		return getFileName(".maven.zip");
	}
	
	public boolean hasDependency(final String groupId, final String artifactId, final String version) {
		for(final Dependency dependency: getMavenArtifact().getDependencies()) {
			if(groupId != null && !dependency.getGroupId().equals(groupId)) {
				continue;
			}
			
			if(artifactId != null && !dependency.getArtifactId().equals(artifactId)) {
				continue;
			}
			
			if(version != null && !dependency.getVersion().equals(version)) {
				continue;
			}
			
			return true;
		}
		
		return false;
	}
	
	public boolean isPicocli() {
		return hasDependency("info.picocli", "picocli", null);
	}
	
	public Map<String, Object> getDataModel() {
		final Map<String, Object> dataModel = new HashMap<>();
		dataModel.put("mavenProject", this);

		final Model artifact = this.getMavenArtifact();
		
		final List<MavenProjectProperty> properties = new ArrayList<>();
		properties.add(new MavenProjectProperty("Build Time", this.getBuildTime().toString()));

		if(this.getGitHash() != null) {
			if(this.getCommitURL() == null) {
				properties.add(new MavenProjectProperty("Git Hash", this.getGitHash()));
			} else {
				properties.add(new MavenProjectProperty("Git Hash", this.getGitHash(), this.getCommitURL() + this.getGitHash()));
			}
		}
		
		properties.add(new MavenProjectProperty("Maven Coordinate", getCoordinate()));
		properties.add(new MavenProjectProperty("Maven Artifact Name", artifact.getName()));
		properties.add(new MavenProjectProperty("Maven Artifact Id", artifact.getArtifactId()));
		properties.add(new MavenProjectProperty("Maven Artifact Group Id", artifact.getGroupId()));
		properties.add(new MavenProjectProperty("Maven Artifact Version", artifact.getVersion()));
		properties.add(new MavenProjectProperty("Maven Artifact Packaging", artifact.getPackaging()));
		properties.add(new MavenProjectProperty("JavaDocs URL", "./javadoc/index.html", "./javadoc/index.html"));
		
		final boolean isPicocli = artifact.getDependencies().stream().filter(x -> {
			return x.getArtifactId().equals("picocli") && x.getGroupId().equals("info.picocli");
		}).count() > 0;		
		
		if(isPicocli) {
			properties.add(new MavenProjectProperty("Man Page URL", "./man/index.html", "./man/index.html"));
		}
		
		getMavenCentralArtifactNames().entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).forEach(x -> {
			properties.add(new MavenProjectProperty(x.getKey(), "./artifacts/" + x.getValue(), "./artifacts/" + x.getValue()));
		});

		properties.add(new MavenProjectProperty("Maven Central Zip", "./artifacts/" + getMavenCentralArtifactName(), "./artifacts/" + getMavenCentralArtifactName()));
		
		dataModel.put("properties", properties);

		return dataModel;
	}

	public Path getTargetPath() {
		return projectPath.resolve("target");
	}

	@Override
	public String toString() {
		return "MavenProject [projectPath=" + projectPath + ", mavenArtifact=" + mavenArtifact.getArtifactId() + "]";
	}
}
