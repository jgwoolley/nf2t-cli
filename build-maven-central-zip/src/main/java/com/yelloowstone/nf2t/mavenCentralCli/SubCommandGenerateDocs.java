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
import java.util.zip.ZipOutputStream;

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
		for (String templateName : new String[] { "root.ftl", "project.ftl", "man.ftl" }) {
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

	private int buildIndex(final ZipOutputStream zos, final Configuration configuration,
			final MavenProject[] mavenProjects) {
		try {
			final Map<String, Object> data = new HashMap<String, Object>();
			data.put("mavenProjects", mavenProjects);

			try (final StringWriter stringWriter = new StringWriter();
					java.io.Writer fileWriter = new java.io.BufferedWriter(stringWriter);) {
				// Load template
				Template template = configuration.getTemplate("root.ftl");

				// Process template and write to output file
				template.process(data, fileWriter);

				final String zipEntryName = "/index.html";
				zos.putNextEntry(new ZipEntry(zipEntryName));
				zos.write(stringWriter.toString().getBytes());
			}

		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
		return 0;

	}

	private int buildProjectIndex(final ZipOutputStream zos, final Configuration configuration,
			final MavenProject mavenProject) {

		try {
			final Map<String, Object> data = mavenProject.getDataModel();

			try (final StringWriter stringWriter = new StringWriter();
					java.io.Writer fileWriter = new java.io.BufferedWriter(stringWriter);) {
				// Load template
				Template template = configuration.getTemplate("project.ftl");

				// Process template and write to output file
				template.process(data, fileWriter);

				final String zipEntryName = mavenProject.getDocumentationZipEntryPrefix("index.html");
				zos.putNextEntry(new ZipEntry(zipEntryName));
				zos.write(stringWriter.toString().getBytes());
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

	private Integer buildManPage(final ZipOutputStream zos, final Configuration configuration,
			final MavenProject mavenProject) {
		final Path projectPath = mavenProject.getProjectPath();
		final MavenArtifact artifact = mavenProject.getMavenArtifact();
		final Map<String, Object> data = mavenProject.getDataModel();

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
			final String zipEntryPrefix = mavenProject.getDocumentationManPageZipEntryPrefix();

			final Asciidoctor asciidoctor = Asciidoctor.Factory.create();

			final org.asciidoctor.Attributes attributes = org.asciidoctor.Attributes.builder()
					.attribute("doctype", "manpage").build();

			final org.asciidoctor.Options options = org.asciidoctor.Options.builder().attributes(attributes)
//		                .safe(org.asciidoctor.SafeMode.SAFE)
//		                .backend("manpage") //Optional, but recommended
					.build();

			zos.putNextEntry(new ZipEntry(zipEntryPrefix));
			zos.closeEntry();
			final String extension = ".adoc";

			List<Entry<Path, String>> manPaths = Files.list(tmpDirPath).map(manPath -> {
				String htmlFileName = manPath.getFileName().toString();
				htmlFileName = htmlFileName.substring(0, htmlFileName.length() - extension.length()) + ".html";

				return Map.entry(manPath, htmlFileName);
			}).collect(Collectors.toList());

			for (Entry<Path, String> entry : manPaths) {
				final Path manPath = entry.getKey();
				final String htmlFileName = entry.getValue();

				try {
					final String asciidocContent = Files.readString(manPath);
					final String htmlContent = asciidoctor.convert(asciidocContent, options);
					final String zipEntryName = mavenProject.getDocumentationManPageZipEntryPrefix(htmlFileName);
					final ZipEntry zipEntry = new ZipEntry(zipEntryName);
					zos.putNextEntry(zipEntry);
					zos.write(htmlContent.getBytes());
					zos.closeEntry();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			data.put("manPaths", manPaths.stream().map(x -> x.getValue()).collect(Collectors.toList()));

			try (final StringWriter stringWriter = new StringWriter();
					java.io.Writer fileWriter = new java.io.BufferedWriter(stringWriter);) {
				// Load template
				Template template = configuration.getTemplate("man.ftl");

				// Process template and write to output file
				template.process(data, fileWriter);
				zos.putNextEntry(new ZipEntry(zipEntryPrefix + "index.html"));
				zos.write(stringWriter.toString().getBytes());
			}

		} catch (Exception e) {
			System.err.println("Could not write ZipEntry.");
			e.printStackTrace();
			return 1;
		}

		return 0;
	}

	private Integer buildJavaDocs(final ZipOutputStream zos, final Configuration configuration,
			final MavenProject mavenProject) {
		final Path projectPath = mavenProject.getProjectPath();
		final MavenArtifact artifact = mavenProject.getMavenArtifact();

		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			new Exception("Path must be a file: " + targetPath).printStackTrace();
			return 1;
		}

		final Path javaDocPath = targetPath.resolve(artifact.getFileName("-javadoc.jar"));
		if (!Files.isRegularFile(javaDocPath)) {
			new Exception("Path must be a file: " + javaDocPath).printStackTrace();
			return 1;
		}

		try (ZipInputStream sourceZip = new ZipInputStream(Files.newInputStream(javaDocPath))) {

			ZipEntry entry;
			while ((entry = sourceZip.getNextEntry()) != null) {
				String entryName = entry.getName();
				String newEntryName = mavenProject.getDocumentationJavaDocZipEntryPrefix(entryName); // Add destination folder

				ZipEntry newEntry = new ZipEntry(newEntryName);
				zos.putNextEntry(newEntry);

				// Copy the data
				byte[] buffer = new byte[1024];
				int len;
				while ((len = sourceZip.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}

				zos.closeEntry();
				sourceZip.closeEntry();
			}

		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
		return 0;
	}

	public Integer packageDocumentation(final ZipOutputStream zos, final Configuration configuration, final MavenProject mavenProject) {

		try {
			int buildIndexResult = buildProjectIndex(zos, configuration, mavenProject);
			if (buildIndexResult != 0)
				return buildIndexResult;
			int buildManPageResult = buildManPage(zos, configuration, mavenProject);
			if (buildManPageResult != 0)
				return buildIndexResult;
			int buildJavaDocsResult = buildJavaDocs(zos, configuration, mavenProject);
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
		final Path targetPath = workdir.resolve("target");
		final Path outPath = targetPath.resolve("docs.zip");

		try {
			Files.createDirectories(targetPath);
			Files.deleteIfExists(outPath);
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		final MavenProject[] mavenProjects = MavenUtils.parseMavenProjects(dBuilder, inputPaths);
		final Configuration configuration = generateConfiguration();

		try (final OutputStream fos = Files.newOutputStream(outPath);
				final ZipOutputStream zos = new ZipOutputStream(fos);) {
			
			buildIndex(zos, configuration, mavenProjects);
			for (final MavenProject mavenProject : mavenProjects) {
				final int returnCode = packageDocumentation(zos, configuration, mavenProject);
				if (returnCode != 0) {
					return returnCode;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		}

		System.out.println(ConsoleColors.GREEN + "Documentation Zip generated at " + outPath + ConsoleColors.RESET);

		return 0;
	}

	public static void main(String[] args) throws ParserConfigurationException, IOException {
		int rc = new CommandLine(new SubCommandGenerateDocs()).execute(args);
		System.exit(rc);
	}

}
