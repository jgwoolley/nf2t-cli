package com.yelloowstone.nf2t.mavenCentralCli;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenUtils {
	
	public static Model parseMavenArtifact(final Path projectPath) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println(
					"Could not create effective pom. Target directory does not exist. Use \"mvn\" command to build artifacts if you haven't already. ["
							+ targetPath.toAbsolutePath() + "]");
			return null;
		}

		final Path artifactPath = targetPath.resolve("effective.pom");

		try {
			final int exitCode = ProcessUtils.exec(projectPath.toFile(), "mvn", "help:effective-pom",
					"-Doutput=" + artifactPath.toAbsolutePath().toString(), "--quiet");
			if (exitCode != 0) {
				System.err.println(
						"Could not create effective pom. Project path is ["
								+ projectPath.toAbsolutePath() + "].");
				return null;
			}
		} catch (Exception e) {
			System.err.println("Could not execute mvn help:effective-pom command. Project path is [" + projectPath.toAbsolutePath() + "].");
			e.printStackTrace();
			return null;
		}

		if (!Files.isRegularFile(artifactPath)) {
			return null;
		}
		
		Model model;
		try (FileReader reader = new FileReader(artifactPath.toFile())) {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            model = mavenReader.read(reader);            
		} catch(IOException | XmlPullParserException e) {
			System.err.println("Could not parse pom.xml: " + artifactPath);
            e.printStackTrace();
			return null;
		}
		
        final Path newArtifactPath = targetPath.resolve(MavenUtils.getFileName(model, ".pom"));
		
		if (model != null) {
			try {
				Files.deleteIfExists(newArtifactPath);
				Files.move(artifactPath, newArtifactPath);
			} catch (Exception e) {
				System.err.println("Could not move pom file: " + newArtifactPath);
				e.printStackTrace();
				return null;
			}
		}
		
        return model;
	}
	
	public static MavenProject parseMavenProject(final Instant buildTime, final Path projectPath) {

		final Model artifact = MavenUtils.parseMavenArtifact(projectPath);
		if (artifact == null) {
			return null;
		}
		
		final String gitHash = GitUtils.createGitHash(projectPath);
				
		return new MavenProject(buildTime, projectPath, artifact, gitHash);
	}
	
	public static List<MavenProject> parseMavenProjects(final Path[] paths) {
		final List<MavenProject> projects = new ArrayList<>();
		
		final Instant buildTime = Instant.now();
				
		for(final Path projectPath: paths) {
			final MavenProject project = parseMavenProject(buildTime, projectPath);
			if(project == null) {
				System.err.println("Could not process project: " + projectPath);
				return null;
			}
			projects.add(project);
		}
		
		return projects;
	}
	
	public static String getFileName(Model model, String postfix) {
		return model.getArtifactId() + "-" + model.getVersion() + postfix;
	}
}
