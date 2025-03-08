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

import org.xml.sax.SAXException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "Build Maven Central Zip", subcommands = { SubCommandGenerateDocs.class })
public class App implements Callable<Integer> {

	@Spec
	private CommandSpec spec;

	private final DocumentBuilderFactory dbFactory;
	private final DocumentBuilder dBuilder;

	public App() throws ParserConfigurationException, IOException {
		super();
		this.dbFactory = DocumentBuilderFactory.newInstance();
		this.dBuilder = dbFactory.newDocumentBuilder();
	}

	private Integer signArtifact(final String gpgUser, final MavenArtifact artifact, final ZipOutputStream zos, final Path artifactPath)
			throws IOException {
		final String zipEntryName = artifact.getMavenCentralZipEntryPrefix();
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

	private Integer packageArtifact(final String gpgUser, final Path artifactPath, final ZipOutputStream zos,
			final MavenArtifact artifact) throws SAXException, IOException {
		final String zipEntryName = artifact.getMavenCentralZipEntryPrefix();
		final String fileName = artifactPath.getFileName().toString();

		final int signArtifactResult = signArtifact(gpgUser, artifact, zos, artifactPath);
		if (signArtifactResult != 0) {
			return signArtifactResult;
		}

		final ZipEntry zipEntry = new ZipEntry(zipEntryName + fileName);
		zos.putNextEntry(zipEntry);

		try (final InputStream fis = Files.newInputStream(artifactPath)) {
			final Map<String, MessageDigest> digests = new HashMap<>();

			for (String key : MavenArtifact.DIGEST_NAMES) {
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
	
	private Integer packageProject(final String gpgUser, final Path projectPath, final ZipOutputStream zos,
			final MavenArtifact artifact) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Path must be a directory: " + targetPath);
			return 1;
		}

		final List<Path> artifactPaths = new ArrayList<>();

		for (final Entry<String, String> entry : artifact.getMavenCentralArtifactNames().entrySet()) {
			final String artifactName = entry.getValue();
			final Path artifactPath = targetPath.resolve(artifactName);
			artifactPaths.add(artifactPath);
			if (!Files.isRegularFile(artifactPath)) {
				System.err.println(
						"Could not find required artifact ("+entry.getKey()+"). Consult with pom.xml file to determine why this file was not created. "
								+ artifactPath);
				return 0;
			}
		}

		try {
			final String zipEntryName = artifact.getMavenCentralZipEntryPrefix();
			zos.putNextEntry(new ZipEntry(zipEntryName));
			zos.closeEntry();

			for (final Path artifactPath : artifactPaths) {
				final int parseArtifactResult = packageArtifact(gpgUser, artifactPath, zos, artifact);
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

	private Integer packageMavenCentral(final String gpgUser, final Path projectPath) {
		if (gpgUser == null) {
			System.err.println("GPG User not specified. This will result in an invalid Maven Central ZIP.");
		}

		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println("Path must be a directory: " + targetPath);
			return 1;
		}

		final MavenArtifact artifact = MavenUtils.parseMavenArtifact(dBuilder, projectPath);
		if (artifact == null) {
			return 1;
		}

		final Path outPath = targetPath.resolve(artifact.getMavenCentralArtifactName());
		if (Files.isRegularFile(outPath)) {
			try {
				Files.deleteIfExists(outPath);
			} catch (IOException e) {
				System.err.println("Could not delete file: " + outPath);
				e.printStackTrace();
				return 1;
			}
		}

		try (final OutputStream fos = Files.newOutputStream(outPath);
				final ZipOutputStream zos = new ZipOutputStream(fos);) {
			return packageProject(gpgUser, projectPath, zos, artifact);
		} catch (Exception e) {
			System.err.println("Error in creating ZIP: " + outPath);
			e.printStackTrace();
			return 1;
		} finally {
			System.out.println(ConsoleColors.GREEN + "Zip generated at " + outPath.toAbsolutePath() + "\nCoordinate: "
					+ artifact.getCoordinate() + ConsoleColors.RESET);
		}
	}

	@Command(name = "mavenCentral", description = "Packages Maven packages meant for the Maven Central Repository.")
	public Integer packageMavenCentral(
			@Parameters(description = "A path of a Maven Project.", defaultValue = ".") final Path[] inputPaths,
			@Option(names = { "--gpgUser", "-u" }, required = false, description = {
					"The local GPG user that will be fed into GPG command." }) final String gpgUser) {
		for (final Path projectPath : inputPaths) {
			final int returnCode = packageMavenCentral(gpgUser, projectPath);
			if (returnCode != 0) {
				return returnCode;
			}
		}
		return 0;
	}

	@Override
	public Integer call() throws Exception {
		return packageMavenCentral(new Path[] { Path.of(".") }, null );
	}

	public static void main(String[] args) throws ParserConfigurationException, IOException {
		int rc = new CommandLine(new App()).execute(args);
		System.exit(rc);
	}
}
