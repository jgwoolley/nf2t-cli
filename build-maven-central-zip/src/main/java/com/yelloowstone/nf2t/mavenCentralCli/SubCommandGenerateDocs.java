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
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.asciidoctor.Asciidoctor;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "docs", description = "Packages documentation, including ManPages, and JavaDocs.")
public class SubCommandGenerateDocs implements Callable<Integer> {

	@Option(names = { "-w", "--workdir" }, description = ".", defaultValue = ".")
	private Path workdir;
	@Parameters(description = "A path of a Maven Project.", defaultValue = ".")
	private Path[] inputPaths;

	private DocumentBuilderFactory dbFactory;
	private DocumentBuilder dBuilder;

	public SubCommandGenerateDocs() throws ParserConfigurationException, IOException {
		super();
		this.dbFactory = DocumentBuilderFactory.newInstance();
		this.dBuilder = dbFactory.newDocumentBuilder();
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

	private int buildIndex(final Path outPath, final Configuration configuration, final MavenProject[] mavenProjects) {
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
			final MavenProject mavenProject) {

		try {
			final Map<String, Object> data = mavenProject.getDataModel();

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

	private String readJarManifest(final Path artifactPath, final String name) {
		if (!Files.exists(artifactPath)) {
			System.err.println("Could not read Jar: " + artifactPath + ". It does not exist.");
			return null;
		}

		try (final ZipFile zipFile = new ZipFile(artifactPath.toFile())) {
			final ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.MF");
			if (manifestEntry == null) {
				System.err.println("No MANIFEST.MF file. Will not read");
				return null;
			}

			try (InputStream inputStream = zipFile.getInputStream(manifestEntry);
					InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
					BufferedReader reader = new BufferedReader(streamReader)) {

				Manifest manifest = new Manifest(inputStream);
				Attributes mainAttributes = manifest.getMainAttributes();
				return mainAttributes.getValue(name);
			}

		} catch (Exception e) {
			System.err.println("Could not read Jar: " + artifactPath);
			e.printStackTrace();
			return null;
		}
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

		final Path projectPath = mavenProject.getProjectPath();
		final MavenArtifact artifact = mavenProject.getMavenArtifact();

		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			new Exception("Path must be a directory: " + targetPath).printStackTrace();
			return 1;
		}

		final Path jarPath = targetPath.resolve(artifact.getFileName(".jar"));
		if (!Files.isRegularFile(jarPath)) {
			new Exception("Path must be a file: " + jarPath).printStackTrace();
			return 1;
		}

		final String mainClass = readJarManifest(jarPath, "Main-Class");
		if (mainClass == null) {
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
//		                .safe(org.asciidoctor.SafeMode.SAFE)
//		                .backend("manpage") //Optional, but recommended
					.build();

			final String extension = ".adoc";

			List<Entry<Path, String>> manPaths = Files.list(tmpDirPath).map(path -> {
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
				
				if(mainCommand.getValue().length() != entry.getValue().length()) {
					final String command = entry.getValue().substring(mainCommand.getValue().length() + 1, entry.getValue().length());
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
				}
			}

			final Path htmlPath = manPath.resolve(mainCommand.getValue() + ".html");
			final Path indexPath = manPath.resolve("index.html");
			Files.deleteIfExists(indexPath);
			Files.copy(htmlPath, indexPath);
		} catch (Exception e) {
			System.err.println("Could not write ZipEntry.");
			e.printStackTrace();
			return 1;
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

		final Path projectPath = mavenProject.getProjectPath();
		final MavenArtifact artifact = mavenProject.getMavenArtifact();

		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			new Exception("Path must be a file: " + targetPath).printStackTrace();
			return 1;
		}

		final Path javaDocJarPath = targetPath.resolve(artifact.getFileName("-javadoc.jar"));
		if (!Files.isRegularFile(javaDocJarPath)) {
			new Exception("Path must be a file: " + javaDocJarPath).printStackTrace();
			return 1;
		}

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
                        Files.copy(zipIn, entryPath, StandardCopyOption.REPLACE_EXISTING); //Corrected line
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				} catch (Exception e) {
					System.err.println("Could not parse JavaDoc Jar: " + javaDocJarPath + ". Entry: " + entry.getName());
					e.printStackTrace();
					return 1;
				}
				
			}
		} catch (Exception e) {
			System.err.println("Could not parse JavaDoc Jar: " + javaDocJarPath);
			e.printStackTrace();
			return 1;
		}

		return 0;
	}

	public Integer packageDocumentation(final Path outPath, final Configuration configuration,
			final MavenProject mavenProject) {
		final Path projectPath = outPath.resolve(mavenProject.getMavenArtifact().getArtifactId());

		try {
			Files.createDirectories(projectPath);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		try {
			int buildIndexResult = buildProjectIndex(projectPath, configuration, mavenProject);
			if (buildIndexResult != 0)
				return buildIndexResult;
			int buildManPageResult = buildManPage(projectPath, configuration, mavenProject);
			if (buildManPageResult != 0)
				return buildIndexResult;
			int buildJavaDocsResult = buildJavaDocs(projectPath, configuration, mavenProject);
			if (buildJavaDocsResult != 0)
				return buildIndexResult;
		} catch (Exception e) {
			System.err.println("Error in creating Documentation for Project: " + mavenProject);
			e.printStackTrace();
			return 1;
		}

		return 0;
	}

	@Override
	public Integer call() throws Exception {
		final Path distPath = workdir.resolve("dist");

		try {
			Files.createDirectories(distPath);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		final MavenProject[] mavenProjects = MavenUtils.parseMavenProjects(dBuilder, inputPaths);
		final Configuration configuration = generateConfiguration();

		buildIndex(distPath, configuration, mavenProjects);
		for (final MavenProject mavenProject : mavenProjects) {
			final int returnCode = packageDocumentation(distPath, configuration, mavenProject);
			if (returnCode != 0) {
				return returnCode;
			}
		}

		System.out.println(ConsoleColors.GREEN + "Documentation Zip generated at " + distPath.toAbsolutePath() + ConsoleColors.RESET);

		return 0;
	}

	public static void main(String[] args) throws ParserConfigurationException, IOException {
		int rc = new CommandLine(new SubCommandGenerateDocs()).execute(args);
		System.exit(rc);
	}

}
