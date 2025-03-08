package com.yelloowstone.nf2t.mavenCentralCli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MavenUtils {
	
	private static MavenArtifact parseMavenArtifact(final Element element) {
		if(element == null) {
			System.err.println("Unable to read Maven artifact. Given empty Element.");
			return null;
		}
		
		final String groupId = XmlUtils.getTextContentByTagNameWithDefault(element, "parent", "groupId");
        final String artifactId = XmlUtils.getTextContentByTagNameWithDefault(element, "parent", "artifactId");
        final String version = XmlUtils.getTextContentByTagNameWithDefault(element, "parent", "version");
        final String packaging = XmlUtils.getTextContentByTagNameWithDefault(element, "parent", "packaging", "jar");
        String name = XmlUtils.getTextContentByTagName(element, "name");
        String description = XmlUtils.getTextContentByTagName(element, "description");
        if(name == null) {
        	name = artifactId;
        }
        final List<String> modules = new ArrayList<>();
        
        final MavenArtifact artifact = new MavenArtifact(groupId, artifactId, version, packaging, name, description, modules);
        
        NodeList modulesNodeList = element.getElementsByTagName("modules");
		
        if (modulesNodeList.getLength() > 0) {
            Element modulesElement = (Element) modulesNodeList.item(0);  // Get the <modules> element
            NodeList moduleNodeList = modulesElement.getElementsByTagName("module"); // Get all <module> elements

            for (int i = 0; i < moduleNodeList.getLength(); i++) {
                Element moduleElement = (Element) moduleNodeList.item(i);
                String moduleName = moduleElement.getTextContent().trim(); // Extract and trim whitespace
                modules.add(moduleName);
            }
        }
        
        return artifact;
	}
	
	public static MavenArtifact parseMavenArtifact(final DocumentBuilder dBuilder, final Path projectPath) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println(
					"Could not create effective pom. Target directory does not exist. Use \"mvn\" command to build artifacts. "
							+ targetPath);
			return null;
		}

		final Path artifactPath = targetPath.resolve("effective.pom");

		try {
			final int exitCode = ProcessUtils.exec(projectPath.toFile(), "mvn", "help:effective-pom",
					"-Doutput=" + artifactPath.toAbsolutePath().toString(), "--quiet");
			if (exitCode != 0) {
				return null;
			}
		} catch (Exception e) {
			System.err.println("Could not execute mvn help:effective-pom command.");
			e.printStackTrace();
			return null;
		}

		if (!Files.isRegularFile(artifactPath)) {
			return null;
		}

		Document doc;
		try {
			doc = dBuilder.parse(artifactPath.toFile());
		} catch (Exception e) {
			System.err.println("Could not parse pom.xml: " + artifactPath);
			e.printStackTrace();
			return null;
		}
		final Element element = doc.getDocumentElement();
		doc.getDocumentElement().normalize();

		final MavenArtifact artifact = MavenUtils.parseMavenArtifact(element);

		if (artifact != null) {
			try {
				final Path newArtifactPath = targetPath.resolve(artifact.getFileName(".pom"));
				Files.deleteIfExists(newArtifactPath);
				Files.move(artifactPath, newArtifactPath);
			} catch (Exception e) {
				System.err.println("Could not move pom file: " + artifactPath);
				e.printStackTrace();
				return null;
			}
		}

		return artifact;
	}
	
	public static MavenProject parseMavenProject(final Instant buildTime, final DocumentBuilder dBuilder, final Path projectPath) {

		final MavenArtifact artifact = MavenUtils.parseMavenArtifact(dBuilder, projectPath);
		if (artifact == null) {
			return null;
		}
		
		final String gitHash = GitUtils.createGitHash(projectPath);
				
		return new MavenProject(buildTime, projectPath, artifact, gitHash);
	}
	
	public static MavenProject[] parseMavenProjects(final DocumentBuilder dBuilder, final Path[] projectPaths) {
		final MavenProject[] projects = new MavenProject[projectPaths.length];
		
		final Instant buildTime = Instant.now();
		
		for(int index = 0; index < projectPaths.length; index++) {
			final Path projectPath = projectPaths[index];
			final MavenProject project = parseMavenProject(buildTime, dBuilder, projectPath);
			projects[index] = project;
		}
		
		return projects;
	}
}
