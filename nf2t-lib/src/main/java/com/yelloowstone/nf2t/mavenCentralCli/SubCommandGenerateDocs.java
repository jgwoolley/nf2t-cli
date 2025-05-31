package com.yelloowstone.nf2t.mavenCentralCli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.asciidoctor.Asciidoctor;

import com.yelloowstone.nf2t.cli.App;
import com.yelloowstone.nf2t.jars.JarDetails;
import com.yelloowstone.nf2t.maven.MavenCoordinate;
import com.yelloowstone.nf2t.maven.MavenJarArtifact;
import com.yelloowstone.nf2t.maven.MavenProject;
import com.yelloowstone.nf2t.utils.ProcessUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "docs", description = "Packages documentation, including ManPages, and JavaDocs.")
public class SubCommandGenerateDocs extends AbstractSubCommand {

	public SubCommandGenerateDocs() throws ParserConfigurationException, IOException {
		super();
	}

	private static String loadResourceAsString(String resourcePath) throws IOException {
		ClassLoader classLoader = App.class.getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(resourcePath);

		if (inputStream == null) {
			throw new IllegalArgumentException("Resource not found: " + resourcePath);
		}

		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append(System.lineSeparator());
			}
		}
		return content.toString();
	}

	private static Configuration generateConfiguration() throws IOException {
		final Path templateDirPath = Files.createTempDirectory("templateDir");
		for (String templateName : new String[] { "root.ftl", "project.ftl" }) {
			String content = loadResourceAsString("templates/" + templateName);
			Files.writeString(templateDirPath.resolve(templateName), content);
		}

		// Configure FreeMarker
		final Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);

		cfg.setDirectoryForTemplateLoading(templateDirPath.toFile());
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);

		return cfg;
	}

	private int buildIndex(final Path outPath, final Configuration configuration,
			final List<MavenProject> mavenProjects) {
		try {
			final Map<String, Object> data = new HashMap<String, Object>();
			data.put("mavenProjects", mavenProjects);

			try (final StringWriter stringWriter = new StringWriter();
					java.io.Writer fileWriter = new java.io.BufferedWriter(stringWriter);) {
				// Load template
				Template template = configuration.getTemplate("root.ftl");

				// Process template and write to output file
				template.process(data, fileWriter);

				final Path indexPath = outPath.resolve("index.html");
				Files.writeString(indexPath, stringWriter.toString());
			}

		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
		return 0;

	}

	private int buildProjectIndex(final Path outPath, final Configuration configuration,
			final MavenProject mavenProject, final Map<String, String> buildArtifactsResult) {

		try {
			final Map<String, Object> data = mavenProject.getDataModel(buildArtifactsResult);

			try (final StringWriter stringWriter = new StringWriter();
					java.io.Writer fileWriter = new java.io.BufferedWriter(stringWriter);) {
				// Load template
				Template template = configuration.getTemplate("project.ftl");

				// Process template and write to output file
				template.process(data, fileWriter);

				final Path indexPath = outPath.resolve("index.html");
				Files.writeString(indexPath, stringWriter.toString());
			}

		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
		return 0;
	}

	private Integer buildManPage(final Path outPath, final Configuration configuration,
			final MavenProject mavenProject) {
		
		final Path manPath = outPath.resolve("man");
		try {
			Files.createDirectories(manPath);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}
		
		final Path indexPath = manPath.resolve("index.html");
		
		if(mavenProject.isPicocli()) {
			final MavenJarArtifact baseJarArtifact = mavenProject.getBaseJarArtifact();
			final Path jarPath = baseJarArtifact.getPath();
			final JarDetails jarDetails = baseJarArtifact.getJarDetails();
			final String mainClass = jarDetails.getMainClass();

			if(mainClass == null) {
				new IllegalArgumentException("Jar had no main class: " + jarPath).printStackTrace();
				return 1;
			}
			
			try {
				final Path tmpDirPath = Files.createTempDirectory("manPage");
				final int exitCode = ProcessUtils.exec("java", "-cp", jarPath.toString(),
						"picocli.codegen.docgen.manpage.ManPageGenerator", mainClass, "--outdir", tmpDirPath.toString());

				if (exitCode != 0) {
					return exitCode;
				}

				final Asciidoctor asciidoctor = Asciidoctor.Factory.create();

				final org.asciidoctor.Attributes attributes = org.asciidoctor.Attributes.builder()
						.attribute("doctype", "manpage").build();

				final org.asciidoctor.Options options = org.asciidoctor.Options.builder().attributes(attributes)
//			                .safe(org.asciidoctor.SafeMode.SAFE)
//			                .backend("manpage") //Optional, but recommended
						.build();

				final String extension = ".adoc";

				final List<Entry<Path, String>> manPaths = Files.list(tmpDirPath).map(path -> {
					String htmlFileName = path.getFileName().toString();
					htmlFileName = htmlFileName.substring(0, htmlFileName.length() - extension.length());

					return Map.entry(path, htmlFileName);
				}).collect(Collectors.toList());

				manPaths.sort((a, b) -> {
					return a.getValue().length() - b.getValue().length();
				});

				final Entry<Path, String> mainCommand = manPaths.get(0);

				for (Entry<Path, String> entry : manPaths) {
					final Path path = entry.getKey();

					if (mainCommand.getValue().length() != entry.getValue().length()) {
						final String command = entry.getValue().substring(mainCommand.getValue().length() + 1,
								entry.getValue().length());
						System.out.println("Parsed Command: " + command);
					}

					final String htmlFileName = entry.getValue() + ".html";

					try {
						final String asciidocContent = Files.readString(path);
						final String htmlContent = asciidoctor.convert(asciidocContent, options);
						final Path htmlPath = manPath.resolve(htmlFileName);

						Files.writeString(htmlPath, htmlContent);
					} catch (IOException e) {
						e.printStackTrace();
						// TODO: Check if this is a good option
						return 1;
					}
				}

				final Path htmlPath = manPath.resolve(mainCommand.getValue() + ".html");
				Files.deleteIfExists(indexPath);
				Files.copy(htmlPath, indexPath);
			} catch (Exception e) {
				System.err.println("Could not write ZipEntry.");
				e.printStackTrace();
				return 1;
			}
		} else {
			try {
				final String htmlContent = "<html><head><title>Invalid Man Page</title></head><body><h1>Invalid Man Page</h1><p><a href=\"../index.html\">Go to parent</a></p></body></html>";				        
				Files.writeString(indexPath, htmlContent);
			} catch (IOException e) {
				e.printStackTrace();
				// TODO: Check if this is a good option
				return 1;
			}
		}
		
		

		return 0;
	}

	private Integer buildJavaDocs(final Path outPath, final Configuration configuration,
			final MavenProject mavenProject) {
		final Path javaDocPath = outPath.resolve("javadoc");
		try {
			Files.createDirectories(javaDocPath);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		final Path targetPath = mavenProject.getArtifactPath();
		if (!Files.isDirectory(targetPath)) {
			new Exception("Given Target path was not a directory: " + targetPath).printStackTrace();
			return 1;
		}

		final Path javaDocJarPath = targetPath.resolve(mavenProject.getBaseCoordinate().getDocsJar().getFileName());
		
		final boolean isJavaDoc = Files.isRegularFile(javaDocJarPath);
		
		if (isJavaDoc) {
			try (ZipInputStream sourceZip = new ZipInputStream(Files.newInputStream(javaDocJarPath))) {

				ZipEntry entry;
				while ((entry = sourceZip.getNextEntry()) != null) {
					final String entryName = entry.getName();
					final Path entryPath = javaDocPath.resolve(entryName);

					if (entry.isDirectory()) {
						Files.createDirectories(entryPath);
						continue;
					}
					Files.createDirectories(entryPath.getParent());

					try (final OutputStream os = Files.newOutputStream(entryPath)) {
						// Copy the data
						byte[] buffer = new byte[1024];
						int len;
						while ((len = sourceZip.read(buffer)) > 0) {
							os.write(buffer, 0, len);
						}

						sourceZip.closeEntry();
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
				return 1;
			}

			try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(javaDocJarPath))) {
				ZipEntry entry = zipIn.getNextEntry();
				while (entry != null) {
					try {
						final Path entryPath = javaDocPath.resolve(entry.getName());
						if (entry.isDirectory()) {
							Files.createDirectories(entryPath);
						} else {
							Files.createDirectories(entryPath.getParent()); // Ensure parent dirs exist
							Files.copy(zipIn, entryPath, StandardCopyOption.REPLACE_EXISTING); // Corrected line
						}
						zipIn.closeEntry();
						entry = zipIn.getNextEntry();
					} catch (Exception e) {
						System.err
								.println("Could not parse JavaDoc Jar: " + javaDocJarPath + ". Entry: " + entry.getName());
						e.printStackTrace();
						return 1;
					}

				}
			} catch (Exception e) {
				System.err.println("Could not parse JavaDoc Jar: " + javaDocJarPath);
				e.printStackTrace();
				return 1;
			}
		} else {
			final String htmlContent = "<html><head><title>No JavaDoc Available</title></head><body><h1>No JavaDoc Available</h1><p><a href=\"../index.html\">Go to parent</a></p></body></html>";	  
			try {
				Files.writeString(javaDocPath.resolve("index.html"), htmlContent);
			} catch (IOException e) {
				e.printStackTrace();
				return 1;
			}
		}

		return 0;
	}

	private Path buildArtifact(final Path artifactsPath, final MavenProject mavenProject,
			final String artifactFilename) {
		final Path artifactPath = mavenProject.getArtifactPath().resolve(artifactFilename);

		if (!Files.isRegularFile(artifactPath)) {
			System.err.println("Could not find artifact: " + artifactPath);
			return null;
		}

		final Path newArtifactPath = artifactsPath.resolve(artifactFilename);

		// TODO: Only .jar .pom -javadoc.jar should have to exist...

		try {
			Files.deleteIfExists(newArtifactPath);
			Files.copy(artifactPath, newArtifactPath);
		} catch (Exception e) {
			System.err.println("Could not move artifact: " + artifactPath + " to " + newArtifactPath);
			e.printStackTrace();
			return null;
		}

		return newArtifactPath;
	}

	private Map<String, String> buildArtifacts(final Path projectPath, final Configuration configuration,
			final MavenProject mavenProject) {
		final Map<String, String> result = new HashMap<>();
		final Path artifactsPath = projectPath.resolve("artifacts");

		try {
			Files.createDirectories(artifactsPath);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		final MavenCoordinate baseCoordinate = mavenProject.getBaseCoordinate();
		
		
		
		for (final Entry<String, MavenCoordinate> entry : baseCoordinate.getMavenCentralArtifacts().entrySet()) {
			final String artifactKey = entry.getKey();
			final String artifactFilename = entry.getValue().getFileName();
			final Path artifactPath = buildArtifact(artifactsPath, mavenProject, artifactFilename);
			if (artifactPath != null) {
				result.put(artifactKey, artifactFilename);
			}
		}
		final String mavenCentralArtifactName = baseCoordinate.getMavenCentralZip().getFileName();
		final Path mavenCentralArtifactPath = buildArtifact(artifactsPath, mavenProject,
				mavenCentralArtifactName);
		if (mavenCentralArtifactPath != null) {
			result.put(MavenProject.MAVEN_CENTRAL_ARTIFACT_KEY, mavenCentralArtifactName);
		}
		System.out.println("Filtered Artifact Set: " + result.keySet());
		
		return result;
	}

	public Integer packageDocumentation(final Path outPath, final Configuration configuration,
			final MavenProject mavenProject) {
		final Path projectPath = outPath.resolve(mavenProject.getMavenModel().getArtifactId());

		try {
			Files.createDirectories(projectPath);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		System.out.println(
				ConsoleColors.PURPLE + "Starting Project: " + mavenProject.getProjectName() + ConsoleColors.RESET);

		try {
			final Map<String, String> buildArtifactsResult = buildArtifacts(projectPath, configuration, mavenProject);

			int buildIndexResult = buildProjectIndex(projectPath, configuration, mavenProject, buildArtifactsResult);
			if (buildIndexResult != 0) {
				return buildIndexResult;
			}
			
			final boolean isPicocli = mavenProject.isPicocli();
			
			//TODO: Fix Picocli docs not appearing on GitHub CI/CD
			System.out.println(ConsoleColors.YELLOW + "DEBUG: isPicocli: " + isPicocli + ConsoleColors.RESET);
			
			int buildManPageResult = buildManPage(projectPath, configuration, mavenProject);
			if (buildManPageResult != 0) {
				return buildIndexResult;
			}		

			int buildJavaDocsResult = buildJavaDocs(projectPath, configuration, mavenProject);
			if (buildJavaDocsResult != 0) {
				return buildIndexResult;
			}
		} catch (Exception e) {
			System.err.println("Error in creating Documentation for Project: " + mavenProject);
			e.printStackTrace();
			return 1;
		}

		System.out.println(
				ConsoleColors.YELLOW + "Created Project: " + mavenProject.getProjectName() + ConsoleColors.RESET);

		return 0;
	}

	@Override
	public Integer processProjects(List<MavenProject> mavenProjects) {
		final Path distPath = getWorkdir().resolve("dist");

		try {
			Files.createDirectories(distPath);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		Configuration configuration;
		try {
			configuration = generateConfiguration();
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		buildIndex(distPath, configuration, mavenProjects);
		for (final MavenProject mavenProject : mavenProjects) {
			final int returnCode = packageDocumentation(distPath, configuration, mavenProject);
			if (returnCode != 0) {
				return returnCode;
			}
		}

		System.out.println(ConsoleColors.GREEN + "Documentation Zip generated at " + distPath.toAbsolutePath()
				+ ConsoleColors.RESET);

		return 0;
	}

	public static void main(String[] args) throws ParserConfigurationException, IOException {
		int rc = new CommandLine(new SubCommandGenerateDocs()).execute(new String[] { "--gpgUser",
				"0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73", "--workdir", "..", "--isPicocli", "nf2t-cli" });
		System.exit(rc);
	}
}
