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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "Build Maven Central Zip")
public class App implements Callable<Integer> {

	final DocumentBuilderFactory dbFactory;
	final DocumentBuilder dBuilder;

	@Parameters(description = "A path of a Maven Project.", defaultValue = ".")
	private Path[] inputPaths;

	@Option(description = "The local GPG user that will be fed into GPG command.", required = false, names = {
			"--gpgUser", "-u" })
	private String gpgUser;

	public App() throws ParserConfigurationException {
		super();
		this.dbFactory = DocumentBuilderFactory.newInstance();
		this.dBuilder = dbFactory.newDocumentBuilder();
	}

	private Integer signArtifact(MavenArtifact artifact, ZipOutputStream zos, Path artifactPath) throws IOException {
		final String zipEntryName = artifact.getZipEntryName();
		final String fileName = artifactPath.getFileName().toString();

		if (gpgUser == null) {
			return 0;
		}

		try {
			final int exitCode = ProcessUtils.exec("gpg", "--batch", "--no-tty", "--yes", "--local-user", gpgUser,
					"-ab", artifactPath.toAbsolutePath().toString());

			if (exitCode != 0) {
				return exitCode;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}

		final String gpgFileName = fileName + ".asc";
		final ZipEntry gpgZipEntry = new ZipEntry(zipEntryName + gpgFileName);
		zos.putNextEntry(gpgZipEntry);

		try (final InputStream fis = Files.newInputStream(artifactPath.getParent().resolve(gpgFileName))) {
			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zos.write(bytes, 0, length);
			}
		} finally {
			zos.closeEntry();
			System.out.println(ConsoleColors.RED + "Wrote GPG Signing: " + gpgZipEntry + ConsoleColors.RESET);
		}

		return 0;
	}

	private MavenArtifact createEffectivePom(Path projectPath) {
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

		final MavenArtifact artifact = XmlUtils.readMavenArtifact(element);

		if (artifact != null) {
			try {
				final Path newArtifactPath = targetPath.resolve(artifact.getFileName(".pom"));
				Files.delete(newArtifactPath);
				Files.move(artifactPath, newArtifactPath);
			} catch (Exception e) {
				System.err.println("Could not move pom file: " + artifactPath);
				e.printStackTrace();
				return null;
			}
		}

		return artifact;
	}

	private Integer packageArtifact(final Path artifactPath, final ZipOutputStream zos, final MavenArtifact artifact)
			throws SAXException, IOException {
		final String zipEntryName = artifact.getZipEntryName();
		final String fileName = artifactPath.getFileName().toString();

		final int signArtifactResult = signArtifact(artifact, zos, artifactPath);
		if (signArtifactResult != 0) {
			return signArtifactResult;
		}

		final ZipEntry zipEntry = new ZipEntry(zipEntryName + fileName);
		zos.putNextEntry(zipEntry);

		try (final InputStream fis = Files.newInputStream(artifactPath)) {
			final Map<String, MessageDigest> digests = new HashMap<>();

			for (String key : new String[] { "MD5", "SHA1", "SHA256", "SHA512" }) {
				digests.put(key, MessageDigest.getInstance(key));
			}

			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				for (final MessageDigest digest : digests.values()) {
					digest.update(bytes, 0, length);
				}
				zos.write(bytes, 0, length);
			}
			zos.closeEntry();
			System.out.println(ConsoleColors.YELLOW + "Wrote Artifact: " + zipEntry + ConsoleColors.RESET);

			for (final Entry<String, MessageDigest> entry : digests.entrySet()) {
				final String digestZipEntryName = zipEntryName + artifactPath.getFileName().toString() + "."
						+ entry.getKey().toLowerCase();
				final ZipEntry digestZipEntry = new ZipEntry(digestZipEntryName);
				zos.putNextEntry(digestZipEntry);

				final StringBuilder sb = new StringBuilder();
				for (final byte x : entry.getValue().digest()) {
					sb.append(String.format("%02x", x));
				}

				zos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
				zos.closeEntry();
				System.out.println("Wrote Digest: " + digestZipEntry);
			}

		} catch (NoSuchAlgorithmException e) {
			System.err.println("Could not parse artifact. " + artifactPath);
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

	private Integer buildManPage(final Configuration configuration, final Path projectPath, final MavenArtifact artifact, final ZipOutputStream zos) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Path must be a directory: " + targetPath);
			return 1;
		}

		final Path jarPath = targetPath.resolve(artifact.getFileName(".jar"));
		if (!Files.isRegularFile(jarPath)) {
			System.err.println("Path must be a file: " + targetPath);
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

			final String zipEntryPrefix = "/man/";
			final Asciidoctor asciidoctor = Asciidoctor.Factory.create();

			   final org.asciidoctor.Attributes attributes = org.asciidoctor.Attributes.builder()
		                .attribute("doctype", "manpage")
		                .build();

			   final org.asciidoctor.Options options = org.asciidoctor.Options.builder()
		                .attributes(attributes)
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
					final ZipEntry zipEntry = new ZipEntry(zipEntryPrefix + htmlFileName);
					zos.putNextEntry(zipEntry);
					zos.write(htmlContent.getBytes());
					zos.closeEntry();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			final Map<String,Object> data = generateDataModel(artifact);
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

	private Integer buildJavaDocs(final Path projectPath, final MavenArtifact artifact, final ZipOutputStream zos) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Path must be a directory: " + targetPath);
			return 1;
		}

		final Path javaDocPath = targetPath.resolve(artifact.getFileName("-javadoc.jar"));
		if (!Files.isRegularFile(javaDocPath)) {
			System.err.println("Path must be a file: " + targetPath);
			return 1;
		}

		final String destinationFolder = "/javadoc/";

		try (ZipInputStream sourceZip = new ZipInputStream(Files.newInputStream(javaDocPath))) {

			ZipEntry entry;
			while ((entry = sourceZip.getNextEntry()) != null) {
				String entryName = entry.getName();
				String newEntryName = destinationFolder + "/" + entryName; // Add destination folder

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

	private Integer buildDocs(final Path projectPath, final MavenArtifact artifact) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Path must be a directory: " + targetPath);
			return 1;
		}

		final Path outPath = targetPath.resolve(artifact.getFileName(".docs.zip"));
		if (Files.isRegularFile(outPath)) {
			try {
				Files.delete(outPath);
			} catch (IOException e) {
				System.err.println("Could not delete file: " + outPath);
				e.printStackTrace();
				return 1;
			}
		}
		
		try (final OutputStream fos = Files.newOutputStream(outPath);
				final ZipOutputStream zos = new ZipOutputStream(fos);) {
			final Configuration configuration = this.generateConfiguration();
			
			int buildIndexResult = buildIndex(configuration, projectPath, artifact, zos);
			if (buildIndexResult != 0)
				return buildIndexResult;
			int buildManPageResult = buildManPage(configuration, projectPath, artifact, zos);
			if (buildManPageResult != 0)
				return buildIndexResult;
			int buildJavaDocsResult = buildJavaDocs(projectPath, artifact, zos);
			if (buildJavaDocsResult != 0)
				return buildIndexResult;
		} catch (Exception e) {
			System.err.println("Error in creating Documentation ZIP: " + outPath);
			e.printStackTrace();
			return 1;
		} finally {
			System.out.println(ConsoleColors.GREEN + "Documentation Zip generated at " + outPath + ConsoleColors.RESET);
		}

		return 0;
	}

	public static String loadResourceAsString(String resourcePath) throws IOException {
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

	private Configuration generateConfiguration() throws IOException {
		final Path templateDirPath = Files.createTempDirectory("templateDir");
		for (String templateName : new String[] { "root.ftl", "man.ftl" }) {
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
	
	private Map<String,Object> generateDataModel(final MavenArtifact artifact) {
		// Create data model
		final Map<String, Object> data = new HashMap<>();
		data.put("artifactId", artifact.getArtifactId());
		data.put("groupId", artifact.getGroupId());
		data.put("version", artifact.getVersion());
		data.put("buildTime", Instant.now().toString());
		
		return data;
	}
	
	private int buildIndex(final Configuration configuration, final Path projectPath, final MavenArtifact artifact, final ZipOutputStream zos) {

		try {
			
			final Map<String,Object> data = generateDataModel(artifact);
			
			try (final StringWriter stringWriter = new StringWriter();
					java.io.Writer fileWriter = new java.io.BufferedWriter(stringWriter);) {
				// Load template
				Template template = configuration.getTemplate("root.ftl");
				
				// Process template and write to output file
				template.process(data, fileWriter);
				zos.putNextEntry(new ZipEntry("index.html"));
				zos.write(stringWriter.toString().getBytes());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
		return 0;
	}

	private Integer packageProject(final Path projectPath, final ZipOutputStream zos, final MavenArtifact artifact) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Path must be a directory: " + targetPath);
			return 1;
		}

		final List<Path> artifactPaths = new ArrayList<>();

		for (final String postfix : new String[] { ".jar", "-javadoc.jar", "-sources.jar", ".pom" }) {
			final Path artifactPath = targetPath.resolve(artifact.getFileName(postfix));
			artifactPaths.add(artifactPath);
			if (!Files.isRegularFile(artifactPath)) {
				System.err.println(
						"Could not find required artifact. Consult with pom.xml file to determine why this file was not created. "
								+ artifactPath);
				return 0;
			}
		}

		try {
			final String zipEntryName = artifact.getZipEntryName();
			zos.putNextEntry(new ZipEntry(zipEntryName));
			zos.closeEntry();

			for (final Path artifactPath : artifactPaths) {
				final int parseArtifactResult = packageArtifact(artifactPath, zos, artifact);
				if (parseArtifactResult != 0) {
					return parseArtifactResult;
				}
			}
		} catch (Exception e) {
			System.err.println("Could not write ZipEntry.");
			e.printStackTrace();
			return 1;
		}

		return 0;
	}

	private Integer packageProject(final Path projectPath) {
		if (this.gpgUser == null) {
			System.err.println("GPG User not specified. This will result in an invalid Maven Central ZIP.");
		}

		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Path must be a directory: " + targetPath);
			return 1;
		}

		final MavenArtifact artifact = createEffectivePom(projectPath);
		if (artifact == null) {
			return 1;
		}

		final Path outPath = targetPath.resolve(artifact.getFileName(".maven.zip"));
		if (Files.isRegularFile(outPath)) {
			try {
				Files.delete(outPath);
			} catch (IOException e) {
				System.err.println("Could not delete file: " + outPath);
				e.printStackTrace();
				return 1;
			}
		}

		// TODO: Separate into its own subcommand.
		final int buildManPageResult = buildDocs(projectPath, artifact);
		if (buildManPageResult != 0) {
			return buildManPageResult;
		}

		try (final OutputStream fos = Files.newOutputStream(outPath);
				final ZipOutputStream zos = new ZipOutputStream(fos);) {
			return packageProject(projectPath, zos, artifact);
		} catch (Exception e) {
			System.err.println("Error in creating ZIP: " + outPath);
			e.printStackTrace();
			return 1;
		} finally {
			System.out.println(ConsoleColors.GREEN + "Zip generated at " + outPath + "\nCoordinate: "
					+ artifact.getCoordinate() + ConsoleColors.RESET);
		}
	}

	@Override
	public Integer call() throws Exception {
		for (final Path projectPath : inputPaths) {
			final int returnCode = packageProject(projectPath);
			if (returnCode != 0) {
				return returnCode;
			}
		}

		return 0;
	}

	public static void main(String[] args) throws ParserConfigurationException {
		int rc = new CommandLine(new App()).execute(args);
		System.exit(rc);
	}
}
