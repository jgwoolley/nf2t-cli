package com.yelloowstone.nf2t.mavenCentralCli;

import java.util.List;

public class MavenArtifact {	
	private final String groupId;
	private final String artifactId;
	private final String version;
	private final String packaging;
	private final String name;
	private final List<String> modules;
	
	public MavenArtifact(String groupId, String artifactId, String version, String packaging, String name, List<String> modules) {
		super();
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packaging = packaging;
		this.name = name;
		this.modules = modules;
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
	
	public String getName() {
		return name;
	}
	
	public List<String> getModules() {
		return modules;
	}			

	public String toString() {
		return getCoordinate();
	}

	public String getFileName(String postfix) {
		return getArtifactId() + "-" + getVersion() + postfix;
	}
	
	public String getCoordinate() {
		return getGroupId() + ":" + getArtifactId() + ":" + getPackaging() + ":" + getVersion();
	};
	
	public String getZipEntryName() {
		final StringBuilder sb = new StringBuilder("/");
		for(final String name: this.getGroupId().split("\\.")) {
			sb.append(name);
			sb.append("/");
		}
		sb.append(this.getArtifactId());
		sb.append("/");

		sb.append(this.getVersion());
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
	
}
