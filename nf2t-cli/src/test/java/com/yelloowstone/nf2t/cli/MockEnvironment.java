package com.yelloowstone.nf2t.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.util.FlowFilePackager;
import org.apache.tika.Tika;
import org.junit.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelloowstone.nf2t.cli.flowfiles.FlowFilePackageVersion;
import com.yelloowstone.nf2t.cli.flowfiles.FlowFilePackageVersions;
import com.yelloowstone.nf2t.cli.flowfiles.FlowFileUtils;

import picocli.CommandLine;

public class MockEnvironment implements AutoCloseable {
	private final FlowFilePackageVersions packageVersions;
	private final int version;

	private final ObjectMapper mapper;

	private final Path versionPath;
	private final Path contentPath;
	private final Path packagedPath;
	private final Path unpackagedPath;
	private final Tika tika;

	private final List<Path> packageInputs;
	private final List<MockFlowFileStreamContainer> unpackageInputs;

	public MockEnvironment(final ObjectMapper mapper, final int version) throws IOException {
		this.mapper = mapper;
		this.tika = new Tika();
		this.packageVersions = new FlowFilePackageVersions();
		this.version = version;
		this.packageInputs = new ArrayList<>();
		this.unpackageInputs = new ArrayList<>();

		this.versionPath = Files.createTempDirectory("testPackageFile");
		this.contentPath = versionPath.resolve("content");
		this.packagedPath = versionPath.resolve("packaged");
		this.unpackagedPath = versionPath.resolve("unpackaged");

		Files.createDirectories(versionPath);
		Files.createDirectories(contentPath);
		Files.createDirectories(packagedPath);
		Files.createDirectories(unpackagedPath);
	}

	public FlowFilePackageVersions getPackageVersions() {
		return packageVersions;
	}

	public int getVersion() {
		return version;
	}

	public Path getVersionPath() {
		return versionPath;
	}

	public Path getContentPath() {
		return contentPath;
	}

	public Path getPackagedPath() {
		return packagedPath;
	}

	public Path getUnpackagedPath() {
		return unpackagedPath;
	}

	public List<Path> getPackageInputs() {
		return packageInputs;
	}

	public List<MockFlowFileStreamContainer> getUnpackageInputs() {
		return unpackageInputs;
	}

	public int getFlowFileSize() {
		return unpackageInputs.stream().mapToInt(x -> x.getFlowFileSize()).sum();
	}

	public MockFlowFileStreamContainer addFlowFileStreamContainer(final Path path) {
		MockFlowFileStreamContainer result = new MockFlowFileStreamContainer(path);
		getUnpackageInputs().add(result);
		return result;
	}

	public Tika getTika() {
		return tika;
	}

	public void createExampleUnpackageFromContent() throws Exception {
		final FlowFilePackageVersion packageVersion = getPackageVersions().get(version);
		final FlowFilePackager packager = packageVersion.getPackager();
		final Path flowFileStreamPath = packageVersion.getDefaultName(packagedPath);

		if (Files.exists(flowFileStreamPath)) {
			Assert.assertTrue("File already exists: " + flowFileStreamPath, false);
		}
		final MockFlowFileStreamContainer container = this.addFlowFileStreamContainer(flowFileStreamPath);
		final MockFlowFileStream flowFileStream = container
				.addFlowFileStream(flowFileStreamPath.getFileName().toString());

		try (OutputStream out = Files.newOutputStream(flowFileStreamPath)) {
			Files.list(contentPath).map(x -> Map.entry(null, null));

			final List<Path> paths = Files.list(contentPath).collect(Collectors.toList());
			System.out.println(
					"\t" + "Paths to package from Content Files: " + mapper.writer().writeValueAsString(paths));

			if (version == 1 && paths.size() != 1) {
				throw new Exception("Version 1 only supports 1 result: Got " + paths.size());
			}

			for (Path path : paths) {
				final long size = Files.size(path);
				final Map<String, String> attributes = FlowFileUtils.generateDefaultAttributes(getTika(), path, size);
				flowFileStream.addFlowFile(Files.readAllBytes(path), attributes);

				System.out.println("\t" + "Content paths to package: " + path);
				System.out.println(
						"\t" + "FlowFile Attributes to package: " + mapper.writer().writeValueAsString(attributes));
				try (InputStream in = Files.newInputStream(path)) {
					packager.packageFlowFile(in, out, attributes, size);
				} catch (Exception e) {
					throw new Exception("Failed to read " + path + " or write to " + flowFileStreamPath, e);
				}
			}
		}
		System.out.println(
				"\t" + ConsoleColors.YELLOW + "Wrote FlowFileStream: " + flowFileStreamPath + ConsoleColors.RESET);
	}

	public void createExampleContent(final String filename, final String value) throws IOException {
		final Path path = contentPath.resolve(filename);
		packageInputs.add(path);
		Files.writeString(path, value);
	}

	public void createExampleContent(final String filename, final byte[] value) throws IOException {
		final Path path = contentPath.resolve(filename);
		packageInputs.add(path);
		Files.write(path, value);
	}

	public MockFlowFileStreamContainer createExampleUnpackageZip() throws IOException {
		final Path zipFile = Files.createFile(packagedPath.resolve("test.zip"));
		final MockFlowFileStreamContainer container = this.addFlowFileStreamContainer(zipFile);

		try (final OutputStream fos = Files.newOutputStream(zipFile)) {
			final ZipOutputStream zipOut = new ZipOutputStream(fos);
			final FlowFilePackageVersion packageVersion = this.getPackageVersions().get(version);
			final FlowFilePackager packager = packageVersion.getPackager();

			for (String example : new String[] { "1.txt", "2.txt" }) {
				final String flowFileStreamName = example + packageVersion.getFileExtension();

				final ZipEntry zipEntry = new ZipEntry(flowFileStreamName);
				zipOut.putNextEntry(zipEntry);

				final MockFlowFileStream flowFileStream = container.addFlowFileStream(flowFileStreamName);
				final Map<String, String> attributes = Map.of("Test", "Test", CoreAttributes.FILENAME.name(), example);
				final byte[] flowFileStreamContent = new byte[] {};
				flowFileStream.addFlowFile(flowFileStreamContent, attributes);

				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				final InputStream in = new ByteArrayInputStream(flowFileStreamContent);

				packager.packageFlowFile(in, out, attributes, 0);
				zipOut.write(out.toByteArray());
				zipOut.closeEntry();
			}
			zipOut.close();
		}
		System.out.println("\t" + ConsoleColors.YELLOW + "Wrote ZIP: " + zipFile + ConsoleColors.RESET);

		return container;
	}

	public MockFlowFileStreamContainer createExampleUnpackageTarGz() throws IOException {
		final Path zipFile = Files.createFile(packagedPath.resolve("test.tar.gz"));
		final MockFlowFileStreamContainer container = this.addFlowFileStreamContainer(zipFile);

		try (final OutputStream fos = Files.newOutputStream(zipFile);
				GzipCompressorOutputStream gzipOutputStream = new GzipCompressorOutputStream(fos);
				TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gzipOutputStream)) {

			tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU); // Handle long file names

			for (int version = 1; version <= 3; version++) {
				final FlowFilePackageVersion packageVersion = this.getPackageVersions().get(version);
				final String flowFileStreamName = "test" + packageVersion.getFileExtension();
				final MockFlowFileStream flowFileStream = container.addFlowFileStream(flowFileStreamName);
				final FlowFilePackager packager = packageVersion.getPackager();

				final ByteArrayOutputStream flowfileStream = new ByteArrayOutputStream();
				final byte[] content = "Hello World!".getBytes(StandardCharsets.UTF_8);
				try (InputStream is = new ByteArrayInputStream(content)) {
					final Map<String, String> attributes = Map.of("Test", "Test");
					flowFileStream.addFlowFile(content, attributes);
					packager.packageFlowFile(is, flowfileStream, attributes, content.length);
				}

				final byte[] out = flowfileStream.toByteArray();
				final long size = out.length;

				final TarArchiveEntry tarEntry = new TarArchiveEntry(flowFileStreamName);

				tarEntry.setSize(size);

				tarOutputStream.putArchiveEntry(tarEntry);
				tarOutputStream.write(out);
				tarOutputStream.closeArchiveEntry();
			}
		}
		System.out.println("\t" + ConsoleColors.YELLOW + "Wrote TARGZ: " + zipFile + ConsoleColors.RESET);

		return container;
	}

	public String execute(Function<MockEnvironment, String[]> func) {
		final App app = new App();
		StringWriter sw = new StringWriter();
		CommandLine cmd = new CommandLine(app);
		cmd.setOut(new PrintWriter(sw));

		int result = cmd.execute(func.apply(this));
		if (result != 0) {
			throw new RuntimeException("Exit code from tested function was not zero: " + result);
		}

		return sw.toString();
	}

	@Override
	public void close() throws Exception {
		deleteWalk(this.versionPath);
	}

	private static void deleteWalk(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					throw exc;
				}
			}
		});
	}

	public List<Path> getFilePaths() throws IOException {
		final List<Path> result = new LinkedList<>();

		Files.walkFileTree(versionPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				result.add(file);
				return FileVisitResult.CONTINUE;
			}
		});

		return result;
	}
}
