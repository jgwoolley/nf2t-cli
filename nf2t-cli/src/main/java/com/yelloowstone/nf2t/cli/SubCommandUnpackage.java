package com.yelloowstone.nf2t.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.util.FlowFileUnpackager;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "unpackage", description = "Unpackages FlowFileStream(s), information regarding this operation sent to standard out. See command arguments for furher details.")
public class SubCommandUnpackage extends AbstractSubCommand {
	@Option(names = { "-v", "--version" }, defaultValue = "3", description = {
			FlowFileStreamResult.VERSION_DESCRIPTION + " incoming FlowFileStream(s)." })
	private int version;
	@Option(names = { "-e",
			"--extension" }, defaultValue = ".pkg", description = FlowFileStreamResult.EXTENSION_UNPACKAGE_DESCRIPTION)
	private String extension;
	@Option(names = { "-i", "--in" }, description = "The input path."
			+ FlowFileStreamResult.INPUTPATH_UNPACKAGE_DESCRIPTION, required = true)
	private String inputOption;
	@Option(names = { "-o", "--out" }, description = "The output path."
			+ FlowFileStreamResult.OUTPUTPATH_UNPACKAGE_DESCRIPTION, required = false)
	private String outputOption;
	@Option(names = { "-u", "--uuid" }, description = FlowFileStreamResult.UUID_DESCRIPTION, defaultValue = "true")
	private boolean uuidFilenames;
	@Option(names = { "-r", "--results" }, description = FlowFileStreamResult.RESULTS_PATH_DESCRIPTION)
	private String resultsPath;

	private void unpackageFlowFileInputStream(final FlowFileStreamResult result, final InputStream is,
			final SourceFile source, final FlowFilePackageVersion packageVersion) {
		final List<FlowFileResult> outputFiles = result.getOutputFiles();
		final List<FlowFileErrorResult> errors = result.getErrors();
		final FlowFileUnpackager unpackager = packageVersion.getUnpackager();

		try {
			do {
				final Path contentPath = result.getOutputPath().resolve(UUID.randomUUID().toString() + ".dat");
				FlowFileResult flowFileResult = null;
				try (OutputStream out = contentPath == null ? OutputStream.nullOutputStream()
						: Files.newOutputStream(contentPath)) {
					final Map<String, String> attributes = unpackager.unpackageFlowFile(is, out);
					final long contentSize = Files.size(contentPath);
					flowFileResult = new FlowFileResult(source, null, attributes, contentSize);
					outputFiles.add(flowFileResult);
				} catch (Exception e) {
					throw new Exception("Could not unpackage " + source.getAbsolutePath(), e);
				}

				if (contentPath != null && result.isUuidFilenames() && flowFileResult != null) {
					String filename = flowFileResult.getRawAttributeExpressions().get(CoreAttributes.FILENAME.key());
					if (filename != null) {
						Path newContentPath = contentPath.getParent().resolve(filename);
						Files.move(contentPath, newContentPath);
						flowFileResult.setContentPath(new SourceFile(null, newContentPath.toAbsolutePath().toString(),
								newContentPath.getFileName().getFileName().toString(),
								flowFileResult.getContentSize()));
					}
				}

			} while (unpackager.hasMoreData());
		} catch (Exception e) {
			errors.add(new FlowFileErrorResult(e, source));
		}
	}

	private void unpackageInputStreamZip(final FlowFileStreamResult result, final InputStream is,
			final SourceFile source) {
		final List<FlowFileErrorResult> errors = result.getErrors();
		try (ZipInputStream zipIs = new ZipInputStream(is)) {
			ZipEntry entry;
			while ((entry = zipIs.getNextEntry()) != null) {
				final String newAbsolutePath = entry.getName();
				final String newFilename = new File(entry.getName()).getName();
				unpackageInputStream(result, zipIs,
						new SourceFile(source, newAbsolutePath, newFilename, entry.getSize()));
			}
		} catch (Exception e) {
			errors.add(new FlowFileErrorResult(e, source));
		}
	}

	private void unpackageInputStreamGzip(final FlowFileStreamResult result, final InputStream is,
			final SourceFile source) {
		final List<FlowFileErrorResult> errors = result.getErrors();

		try (final GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(is);
				final TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream);) {
			ArchiveEntry entry;
			while ((entry = tarInputStream.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					byte[] buffer = new byte[1024];
					while (tarInputStream.read(buffer) > 0) {

					}
				} else {
					final String newAbsolutePath = entry.getName();
					final String newFilename = new File(entry.getName()).getName();
					unpackageInputStream(result, tarInputStream,
							new SourceFile(source, newAbsolutePath, newFilename, entry.getSize()));
				}

			}

		} catch (Exception e) {
			errors.add(new FlowFileErrorResult(e, source));
		}
	}

	private void unpackageInputStream(final FlowFileStreamResult result, final InputStream is,
			final SourceFile source) {
		final List<FlowFileErrorResult> errors = result.getErrors();
		final String absolutePath = source.getAbsolutePath();

		if (absolutePath == null) {
			errors.add(new FlowFileErrorResult(new NullPointerException("Given Absolute Path was null."), source));
			return;
		}

		if (absolutePath.endsWith(".zip")) {
			unpackageInputStreamZip(result, is, source);
			return;
		}

		if (absolutePath.endsWith(".tar.gz")) {
			unpackageInputStreamGzip(result, is, source);
			return;
		}

		if (absolutePath.endsWith(result.getExtension())) {
			final FlowFilePackageVersion packageVersion = this.getPackageVersions().get(result.getVersion());
			unpackageFlowFileInputStream(result, is, source, packageVersion);
			return;
		}

		for (int version = 1; version <= 3; version++) {
			final FlowFilePackageVersion packageVersion = this.getPackageVersions().get(version);
			if (absolutePath.endsWith(packageVersion.getFileExtension())) {
				unpackageFlowFileInputStream(result, is, source, packageVersion);
				return;
			}
		}
	}

	@Override
	public Integer call() throws Exception {
		final FlowFileStreamResult result = createResult(version, extension, uuidFilenames, inputOption, outputOption,
				resultsPath, new HashMap<>(), true);

		// Unpack Frequently Used Variables
		final Path inputPath = result.getInputPath();
		final Path outputPath = result.getOutputPath();
		final List<FlowFileErrorResult> errors = result.getErrors();

		final SourceFile source = SourceFile.fromPath(null, inputPath);

		// Get Packager For Current Version
		final FlowFilePackageVersion packageVersion = getPackageVersion(version);
		if (packageVersion == null) {
			errors.add(
					new FlowFileErrorResult(new Exception("Bad FlowFile Package Version Given: " + version), source));
			printResult(result);
			return 1;
		}

		if (!Files.isDirectory(outputPath)) {
			final Exception exception = new FileNotFoundException(
					"Output path not found: " + outputPath.toAbsolutePath().toString());
			errors.add(new FlowFileErrorResult(exception, source));
		} else {
			if (Files.isDirectory(inputPath)) {
				try (final Stream<Path> files = Files.list(inputPath)) {
					files.forEach(x -> {
						final SourceFile newSource = SourceFile.fromPath(null, x);
						try (final InputStream is = Files.newInputStream(x)) {
							unpackageInputStream(result, is, newSource);
						} catch (IOException e) {
							errors.add(new FlowFileErrorResult(e, newSource));
						}
					});
				} catch (IOException e) {
					errors.add(new FlowFileErrorResult(e, source));
				}
			} else if (Files.isRegularFile(inputPath)) {
				try (final InputStream is = Files.newInputStream(inputPath)) {
					unpackageInputStream(result, is, source);
				} catch (IOException e) {
					errors.add(new FlowFileErrorResult(e, source));
				}
			} else {
				final Exception exception = new FileNotFoundException(
						"Input path not found: " + inputPath.toAbsolutePath().toString());
				errors.add(new FlowFileErrorResult(exception, source));
			}
		}

		if (printResult(result)) {
			return 1;
		}

		return 0;
	}

}
