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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.util.FlowFilePackager;
import org.apache.tika.Tika;

import picocli.CommandLine;

public class MockEnvironment implements AutoCloseable {
	private int expectedResults = 0;
	private final FlowFilePackageVersions packageVersions;
	private final int version;

	private final Path versionPath;
	private final Path contentPath;
	private final Path packagedPath;
	private final Path unpackagedPath;
	private final Tika tika;

	public MockEnvironment(final int version) throws IOException {
		this.tika = new Tika();
		this.packageVersions = new FlowFilePackageVersions();
		this.version = version;

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

	public int getExpectedResults() {
		return expectedResults;
	}
	
	public Tika getTika() {
		return tika;
	}

	public void createExampleContent(final String filename, final String value) throws IOException {
		final Path path = contentPath.resolve(filename);
		this.expectedResults += 1;
		Files.writeString(path, value);
	}

	public void createExampleContent(final String filename, final byte[] value) throws IOException {
		final Path path = contentPath.resolve(filename);
		this.expectedResults += 1;
		Files.write(path, value);
	}

	public void createExampleZip() throws IOException {
		final Path zipFile = Files.createFile(packagedPath.resolve("test.zip"));
		try (final OutputStream fos = Files.newOutputStream(zipFile)) {
			final ZipOutputStream zipOut = new ZipOutputStream(fos);
			final FlowFilePackageVersion packageVersion = this.getPackageVersions().get(version);
			final FlowFilePackager packager = packageVersion.getPackager();

			for (String example : new String[] { "1.txt", "2.txt" }) {
				this.expectedResults += 1;
				final ZipEntry zipEntry = new ZipEntry(example + packageVersion.getFileExtension());
				zipOut.putNextEntry(zipEntry);

				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				final InputStream in = new ByteArrayInputStream(new byte[] {});
				final Map<String, String> attributes = Map.of("Test", "Test", CoreAttributes.FILENAME.name(), example);

				packager.packageFlowFile(in, out, attributes, 0);
				zipOut.write(out.toByteArray());
				zipOut.closeEntry();
			}
			zipOut.close();
		}
	}

	public void createExampleTarGz() throws IOException {
		final Path zipFile = Files.createFile(packagedPath.resolve("test.tar.gz"));

		try (final OutputStream fos = Files.newOutputStream(zipFile);
				GzipCompressorOutputStream gzipOutputStream = new GzipCompressorOutputStream(fos);
				TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gzipOutputStream)) {

			tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU); // Handle long file names

			for(int version = 1; version <=3; version++) {
				this.expectedResults += 1;

				final FlowFilePackageVersion packageVersion = this.getPackageVersions().get(version);
				
				final FlowFilePackager packager = packageVersion.getPackager();
				
				final ByteArrayOutputStream flowfileStream = new ByteArrayOutputStream();
				final byte[] content = "Hello World!".getBytes(StandardCharsets.UTF_8);
				try(InputStream is = new ByteArrayInputStream(content)) {
					final Map<String, String> attributes = Map.of("Test", "Test");
					packager.packageFlowFile(is, flowfileStream, attributes, content.length);
				}
				
				final byte[] out = flowfileStream.toByteArray();
				final long size = out.length;
				
				final TarArchiveEntry tarEntry = new TarArchiveEntry("test" + packageVersion.getFileExtension());
				tarEntry.setSize(size);

				tarOutputStream.putArchiveEntry(tarEntry);			
				tarOutputStream.write(out);
				tarOutputStream.closeArchiveEntry();
			}
		}
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
