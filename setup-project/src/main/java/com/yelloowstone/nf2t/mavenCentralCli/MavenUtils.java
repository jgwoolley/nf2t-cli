package com.yelloowstone.nf2t.mavenCentralCli;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenUtils {
	
	public static Model parseEffectivePom(final Path artifactPath) {
		try (FileReader reader = new FileReader(artifactPath.toFile())) {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            return mavenReader.read(reader);            
		} catch(IOException | XmlPullParserException e) {
			System.err.println("Could not parse pom.xml: " + artifactPath);
            e.printStackTrace();
			return null;
		}
	}
	
	public static Model generateEffectivePom(final Path projectPath) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println(
					"Target directory does not exist. Use \"mvn\" command to build artifacts if you haven't already. ["
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
		
		Model model = parseEffectivePom(artifactPath);
		
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
	
	public static Model findEffectivePom(final Path projectPath) {
		final List<Path> pomPaths;
		try {
			pomPaths = Files.list(projectPath).filter(x -> x.toString().endsWith(".pom")).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		if(pomPaths.size() == 0) {
			new Exception("Only one .pom file must exist in the directory: " + projectPath + ". The following .pom file(s) were found: " + pomPaths + ".").printStackTrace();
			return null;
		}
		
		final Path pomPath = pomPaths.get(0);
		
		return parseEffectivePom(pomPath);
	}
	
	public static MavenProject parseMavenProject(final boolean resolvePom, final Instant buildTime, final Path projectPath) {

		final Model artifact = resolvePom ? MavenUtils.generateEffectivePom(projectPath) : MavenUtils.findEffectivePom(projectPath);
		if (artifact == null) {
			return null;
		}
		
		final String gitHash = GitUtils.createGitHash(projectPath);
				
		return new MavenProject(resolvePom, buildTime, projectPath, artifact, gitHash);
	}
	
	public static List<MavenProject> parseMavenProjects(final boolean resolvePom, final Path[] paths) {
		final List<MavenProject> projects = new ArrayList<>();
		
		final Instant buildTime = Instant.now();
				
		for(final Path projectPath: paths) {
			final MavenProject project = parseMavenProject(resolvePom, buildTime, projectPath);
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
