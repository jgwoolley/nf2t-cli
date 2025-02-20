package com.yelloowstone.nf2t.mavenCentralCli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "Build Maven Central Zip")
public class App implements Callable<Integer> {

	final DocumentBuilderFactory dbFactory;
	final DocumentBuilder dBuilder;

	@Parameters(description = "The path of the Maven Project.", defaultValue="..")
	private Path path;

	@Option(description = "The local GPG user that will be fed into GPG command.", required = false, names = {
			"--gpgUser" }, defaultValue="0xCED254CF741FE1663B9BEC32D12C9545C6D5AA73")
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
			final int exitCode = ProcessUtils.exec("gpg", "--batch", "--no-tty", "--yes", "--local-user",
					gpgUser, "-ab", artifactPath.toAbsolutePath().toString());
			
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
			System.out.println("Wrote GPG Signing: " + gpgZipEntry);
		}

		return 0;
	}

	private Integer createEffectivePom(MavenArtifact artifact, ZipOutputStream zos, Path path) throws IOException {
		final Path targetPath = path.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Could not create effective pom. Target directory does not exist. Use \"mvn\" command to build artifacts. " + targetPath);
			return 0;
		}
		
		final String zipEntryName = artifact.getZipEntryName();
		final String fileName = artifact.getFileName(".pom");
		final Path artifactPath = targetPath.resolve(fileName);
		
		final ZipEntry gpgZipEntry = new ZipEntry(zipEntryName + fileName);
		zos.putNextEntry(gpgZipEntry);

		try (final InputStream fis = Files.newInputStream(artifactPath)) {
			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zos.write(bytes, 0, length);
			}
		} finally {
			zos.closeEntry();
			System.out.println("Wrote Effective POM: " + gpgZipEntry);
		}
		
		try {
			final int exitCode = ProcessUtils.exec(path.toFile(), "mvn", "help:effective-pom", "-Doutput="+artifactPath.toAbsolutePath().toString(), "--quiet");
			if (exitCode != 0) {
				return exitCode;
			}
		} catch (Exception e) {
			System.err.println("Could not execute mvn help:effective-pom command.");
			e.printStackTrace();
			return 1;
		}

		signArtifact(artifact, zos, artifactPath);

		return 0;
	}
	
	private Integer parseArtifact(MavenArtifact artifact, ZipOutputStream zos, Path artifactPath)
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
			System.out.println("Wrote Artifact: " + zipEntry);
			
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

	private Integer parsePom(ZipOutputStream zos, Path path) {
		final Path pomPath = path.resolve("pom.xml");
		if (!Files.exists(pomPath)) {
			System.err.println("There is no pom.xml file at: " + pomPath);
			return 1;
		}

		Document doc;
		try {
			doc = dBuilder.parse(pomPath.toFile());
		} catch (Exception e) {
			System.err.println("Could not parse pom.xml: " + pomPath);
			e.printStackTrace();
			return 1;
		}
		final Element element = doc.getDocumentElement();
		doc.getDocumentElement().normalize();

		final MavenArtifact artifact = XmlUtils.readMavenArtifact(element);

		for (final String moduleName : artifact.getModules()) {
			final Path modulePath = path.resolve(moduleName);
			final int result = parsePom(zos, modulePath);

			if (0 != result) {
				return result;
			}
		}

		final Path targetPath = path.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Target directory does not exist. Use \"mvn\" command to build artifacts. " + targetPath);
			return 0;
		}
		
		final List<Path> artifactPaths = new ArrayList<>();

		for (final String postfix : new String[] { ".jar", "-javadoc.jar", "-sources.jar" }) {
			final Path artifactPath = targetPath.resolve(artifact.getFileName(postfix));
			artifactPaths.add(artifactPath);
			if (!Files.isRegularFile(artifactPath)) {
				System.err.println("Could not find required artifact. Consult with pom.xml file to determine why this file was not created. " + artifactPath);
				return 0;
			}
		}

		try {
			createEffectivePom(artifact, zos, path);
		} catch (IOException e) {
			System.err.println("Could not create effective pom");
			e.printStackTrace();
		}
		
		try {
			final String zipEntryName = artifact.getZipEntryName();
			zos.putNextEntry(new ZipEntry(zipEntryName));
			zos.closeEntry();

			for (final Path artifactPath : artifactPaths) {
				final int parseArtifactResult = parseArtifact(artifact, zos, artifactPath);
//				System.out.println(artifactPath);
				if(parseArtifactResult != 0) {
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

	private Integer parsePom(Path path) {
		if(this.gpgUser == null) {
			System.err.println("GPG User not specified. This will result in an invalid Maven Central ZIP.");
		}
		
		final Path targetPath = path.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Path must be a directory: " + targetPath);
			return 1;
		}

		final Path outPath = targetPath.resolve("maven.zip");
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
			return parsePom(zos, path);
		} catch (Exception e) {
			System.err.println("Error in creating ZIP: " + outPath);
			e.printStackTrace();
			return 1;
		} finally {
			System.out.println("Zip generated at " + outPath);
		}
	}
	
	@Override
	public Integer call() throws Exception {
		return parsePom(path);
	}
	
	public static void main(String[] args) throws ParserConfigurationException {
		int rc = new CommandLine(new App()).execute(args);
		System.exit(rc);
	}
}
