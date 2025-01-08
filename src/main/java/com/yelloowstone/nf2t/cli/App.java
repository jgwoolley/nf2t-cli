package com.yelloowstone.nf2t.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.util.FlowFilePackager;
import org.apache.nifi.util.FlowFileUnpackager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.codegen.docgen.manpage.ManPageGenerator;

/**
 * @author 26191568+jgwoolley@users.noreply.github.com
 */
@Command(name = "nf2t", subcommands = ManPageGenerator.class, mixinStandardHelpOptions = true, description = "A Java CLI for parsing Apache NiFi FlowFileStreams. One or more FlowFiles can be serialized into a FlowFileStream, in one of three formats.")
public class App implements Callable<Integer> {
	public static final String FILE_SIZE_ATTRIBUTE = "size";

	@Spec
	private CommandSpec spec;
	private final FlowFilePackageVersions packageVersions;
	private final ObjectMapper mapper;

	public App() {
		super();
		this.packageVersions = new FlowFilePackageVersions();
		this.mapper = new ObjectMapper();
	}

	private void unpackageFlowFileInputStream(final FlowFileStreamResult result,
			final Path flowFilePath,
			final InputStream in) {
		// Unpack Frequently Used Variables
		final List<FlowFileResult> outputFiles = result.getOutputFiles();
		final List<FlowFileErrorResult> errors = result.getErrors();
		final FlowFilePackageVersion packageVersion = this.getPackageVersions().get(result.getVersion());
		final FlowFileUnpackager unpackager = packageVersion.getUnpackager();

		try {
			do {
				final Path contentPath = result.getOutputPath().resolve(UUID.randomUUID().toString() + ".dat");
				FlowFileResult flowFileResult = null;
				try (OutputStream out = Files.newOutputStream(contentPath)) {
					final Map<String, String> attributes = unpackager.unpackageFlowFile(in, out);
					final long contentSize = Files.size(contentPath);
					flowFileResult = new FlowFileResult(flowFilePath.toUri(), contentPath.toUri(), attributes,
							contentSize);
					outputFiles.add(flowFileResult);
				} catch (Exception e) {
					throw new Exception("Could not unpackage " + flowFilePath, e);
				}

				if (result.isUuidFilenames() && flowFileResult != null) {
					String filename = flowFileResult.getAttributes().get(CoreAttributes.FILENAME.key());
					if (filename != null) {
						Path newContentPath = contentPath.getParent().resolve(filename);
						Files.move(contentPath, newContentPath);
						flowFileResult.setContentPath(newContentPath.toUri());
					}
				}

			} while (unpackager.hasMoreData());
		} catch (Exception e) {
			errors.add(new FlowFileErrorResult(e, flowFilePath.toUri()));
		}
	}

	private URI updatePath(final Path originalPath, String scheme) throws URISyntaxException {
		final URI originalUri = originalPath.toUri();
		final String host = originalUri.getHost();
		final int port = originalUri.getPort();
		final String path = originalUri.getPath();
		final String query = originalUri.getQuery();
		final String fragment = originalUri.getFragment();

		final URI updatedURI = new URI(scheme, null, host, port, path, query, fragment);

		return updatedURI;
	}
	
	private void unpackageFlowFilePath(final FlowFileStreamResult result,
			final Path flowFilePath) {
		final List<FlowFileErrorResult> errors = result.getErrors();

		if (Files.isRegularFile(flowFilePath)) {
			final String fileName = flowFilePath.getFileName().toString();

			if (fileName.endsWith(".zip")) {
				try {
					final URI updatedPath = updatePath(flowFilePath, "jar:file");

					final Map<String, Object> env = new HashMap<>();
					env.put("create", "false");

					try (final FileSystem fs = FileSystems.newFileSystem(updatedPath, env)) {
						for (Path path : fs.getRootDirectories()) {
							unpackageFlowFilePath(result, path);
						}
					} catch (Exception e) {
						errors.add(new FlowFileErrorResult(e, updatedPath));
					}

				} catch (Exception e) {
					errors.add(new FlowFileErrorResult(e, flowFilePath.toUri()));
				}
			} else if (fileName.endsWith(".tar.gz")) {
				// TODO: ZIP File support
				// TODO: TAR / GZIP support TarArchiveEntry
				// this.fs = FileSystems.newFileSystem(zipfile, null);
				errors.add(new FlowFileErrorResult(new IllegalArgumentException("Doesn't support .tar.gz files yet"),
						flowFilePath.toUri()));

//				try(final InputStream fis = Files.newInputStream(flowFilePath);final GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(fis);final TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream);) {
//		             ArchiveEntry entry;
//		             while ((entry = tarInputStream.getNextEntry()) != null) {
//		                 System.out.println("Entry Name: " + entry.getName());
//		                 System.out.println("Entry Size: " + entry.getSize());
//		                 
//		                 // Process the entry data (if it's a file)
//		                 if (!entry.isDirectory()) {
//		                	 result.getErrors();
//		                	 
//		                	 if(entry.getName().endsWith(fileName)) {
//		     					unpackageFlowFileInputStream(result, packageVersion, uuidFilenames, flowFilePath, tarInputStream);
//			                    continue;
//		                	 }
//		                	 
//		                	 byte[] buffer = new byte[1024];
//		                     while (tarInputStream.read(buffer) > 0) {
//		                         
//		                     }
//		                 }
//		             }
//		             
//				}catch (Exception e) {
//					errors.add(new FlowFileErrorResult(e, flowFilePath.toUri()));
//				}					
			} else {
				// TODO: Currently assumes all files are flowFiles that are regular files and
				// not archives.

				try (final InputStream in = Files.newInputStream(flowFilePath)) {
					unpackageFlowFileInputStream(result, flowFilePath, in);
				} catch (Exception e) {
					errors.add(new FlowFileErrorResult(e, flowFilePath.toUri()));
				}
			}
		} else if (Files.isDirectory(flowFilePath)) {
			try (final Stream<Path> files = Files.list(flowFilePath)) {
				files.forEach(x -> {
					unpackageFlowFilePath(result, x);
				});
			} catch (IOException e) {
				errors.add(new FlowFileErrorResult(e, flowFilePath.toUri()));
			}
		} else {
			errors.add(new FlowFileErrorResult("Could not parse", flowFilePath.toUri()));
		}

	}

	@Command(name = "unpackage", description = "Unpackages FlowFileStream(s), information regarding this operation sent to standard out. See command arguments for furher details.")
	public Integer unpackageFlowFileStream(
			@Option(names = { "-v", "--version" }, defaultValue = "3", description = {
					FlowFileStreamResult.VERSION_DESCRIPTION + " incoming FlowFileStream(s)." }) final int version,
			@Option(names = { "-e",
					"--extension" }, defaultValue = ".pkg", description = FlowFileStreamResult.EXTENSION_UNPACKAGE_DESCRIPTION) final String extension,
			@Option(names = { "-i", "--in" }, description = "The input path."
					+ FlowFileStreamResult.INPUTPATH_UNPACKAGE_DESCRIPTION, required = true) final String inputOption,
			@Option(names = { "-o", "--out" }, description = "The output path."
					+ FlowFileStreamResult.OUTPUTPATH_UNPACKAGE_DESCRIPTION, required = true) final String outputOption,
			@Option(names = { "-u", "--uuid" }, description = FlowFileStreamResult.UUID_DESCRIPTION, defaultValue = "true") final boolean uuidFilenames) {
		final FlowFileStreamResult result = createResult(version, extension, uuidFilenames, inputOption, outputOption);

		// Unpack Frequently Used Variables
		final Path inputPath = result.getInputPath();
		final Path outputPath = result.getOutputPath();
		final List<FlowFileErrorResult> errors = result.getErrors();

		// Get Packager For Current Version
		final FlowFilePackageVersion packageVersion = packageVersions.get(version);
		if (packageVersion == null) {
			errors.add(new FlowFileErrorResult(new Exception("Bad FlowFile Package Version Given: " + version),
					inputPath.toUri()));
			printResult(result);
			return 1;
		}

		if (!Files.isDirectory(outputPath)) {
			final Exception exception = new FileNotFoundException(
					"Output path not found: " + outputPath.toAbsolutePath().toString());
			errors.add(new FlowFileErrorResult(exception, inputPath.toUri()));
		} else {
			if (Files.isDirectory(inputPath)) {
				try (final Stream<Path> files = Files.list(inputPath)) {
					files.forEach(flowFilePath -> {
						unpackageFlowFilePath(result, flowFilePath);
					});
				} catch (IOException e) {
					errors.add(new FlowFileErrorResult(e, inputPath.toUri()));
				}
			} else if (Files.isRegularFile(inputPath)) {
				unpackageFlowFilePath(result, inputPath);
			} else {
				final Exception exception = new FileNotFoundException(
						"Input path not found: " + inputPath.toAbsolutePath().toString());
				errors.add(new FlowFileErrorResult(exception, inputPath.toUri()));
			}
		}

		if (printResult(result)) {
			return 1;
		}

		return 0;
	}

	@Command(name = "package", description = "Packages FlowFileStream(s), information regarding this operation sent to standard out. See command arguments for furher details.")
	public Integer packageFlowFileStream(
			@Option(names = { "-v", "--version" }, defaultValue = "3", description = {
					FlowFileStreamResult.VERSION_DESCRIPTION + " resulting FlowFile." }) final int version,
			@Option(names = { "-e",
					"--extension" }, defaultValue = "", description = FlowFileStreamResult.EXTENSION_PACKAGE_DESCRIPTION) final String extension,
			@Option(names = { "-i", "--in" }, description = "The input path."
					+ FlowFileStreamResult.INPUTPATH_PACKAGE_DESCRIPTION, required = true) final String inputOption,
			@Option(names = { "-o", "--out" }, description = "The output path."
					+ FlowFileStreamResult.OUTPUTPATH_PACKAGE_DESCRIPTION, required = true) final String outputOption) {
		final FlowFileStreamResult result = createResult(version, extension, true, inputOption, outputOption);

		// Unpack Frequently Used Variables
		final Path inputPath = result.getInputPath();
		Path outputPath = result.getOutputPath();
		final List<FlowFileErrorResult> errors = result.getErrors();

		// Get Packager For Current Version
		final FlowFilePackageVersion packageVersion = packageVersions.get(version);
		if (packageVersion == null) {
			errors.add(new FlowFileErrorResult(new Exception("Bad FlowFile Package Version Given: " + version),
					inputPath.toUri()));
			printResult(result);
			return 1;
		}

		if (Files.isDirectory(outputPath)) {
			outputPath = packageVersion.getDefaultName(outputPath);
			result.setOutputPath(outputPath);
		}

		final List<Path> contentPaths = new LinkedList<>();
		if (Files.isRegularFile(inputPath)) {
			contentPaths.add(inputPath);
		} else if (Files.isDirectory(inputPath)) {
			try {
				Files.list(inputPath).filter(Files::isRegularFile).forEach(x -> contentPaths.add(x));
			} catch (IOException e) {
				result.getErrors().add(new FlowFileErrorResult(e, outputPath.toUri()));
				printResult(result);
				return 1;
			}
		} else {
			final Exception exception = new FileNotFoundException(
					"Flowfile content not found at path: " + inputPath.toAbsolutePath().toString());
			result.getErrors().add(new FlowFileErrorResult(exception, inputPath.toUri()));
			printResult(result);
			return 1;
		}

		final FlowFilePackager packager = packageVersion.getPackager();

		try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
			for (Path contentPath : contentPaths) {
				try {
					final Map<String, String> attributes = new HashMap<>();
					final long contentSize = updateDefaultAttributes(attributes, contentPath);

					try (InputStream inputStream = Files.newInputStream(contentPath)) {
						packager.packageFlowFile(inputStream, outputStream, attributes, contentSize);

						final FlowFileResult packageResult = new FlowFileResult(outputPath.toUri(), inputPath.toUri(),
								attributes, contentSize);
						result.getOutputFiles().add(packageResult);
					}

				} catch (IOException e) {
					result.getErrors().add(new FlowFileErrorResult(e, contentPath.toUri()));
					printResult(result);
					return 1;
				}
			}
		} catch (IOException e) {
			result.getErrors().add(new FlowFileErrorResult(e, outputPath.toUri()));
			printResult(result);
			return 1;
		}

		if (printResult(result)) {
			return 1;
		}

		return 0;
	}

	@Command(name = "generateSchema", description = "Generates a JSONSchema for the result of the Unpackage/Package commands.")
	public Integer generateSchema() throws JsonProcessingException {
		SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
		mapper.acceptJsonFormatVisitor(FlowFileStreamResult.class, visitor);
		JsonSchema personSchema = visitor.finalSchema();
		spec.commandLine().getOut().println(this.mapper.writer().writeValueAsString(personSchema));
		return 0;
	}

	@Override
	public Integer call() throws Exception {
		throw new picocli.CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand.");
	}

	public FlowFilePackageVersions getPackageVersions() {
		return packageVersions;
	}

	public boolean printResult(final FlowFileStreamResult result) {
		try {
			String x = this.mapper.writer().writeValueAsString(result);
			spec.commandLine().getOut().println(x);
		} catch (JsonProcessingException e) {
			e.printStackTrace(spec.commandLine().getErr());
			return true;
		}

		return false;
	}

	public FlowFileStreamResult createResult(final int version, String extension, final boolean uuidFilenames, final String inputOption,
			String outputOption) {
		final Path inputPath = Paths.get(inputOption == null ? "." : inputOption);
		final Path outputPath = outputOption == null ? inputPath : Paths.get(outputOption);

		if (extension.length() <= 0) {
			extension = packageVersions.get(version).getFileExtension();
		}

		long unixTime = System.currentTimeMillis() / 1000L;
		return new FlowFileStreamResult(version, extension, uuidFilenames, inputPath, outputPath, unixTime);
	}

	public static long updateDefaultAttributes(Map<String, String> attributes, Path path) throws IOException {
		final long contentSize = Files.size(path);

		attributes.put(CoreAttributes.FILENAME.key(), path.getFileName().toString());
		attributes.put(CoreAttributes.PATH.key(), path.getParent().toString());
		attributes.put(CoreAttributes.ABSOLUTE_PATH.key(), path.toString());
		attributes.put(FILE_SIZE_ATTRIBUTE, Long.toString(contentSize));

		return contentSize;
	}

	public static void main(String[] args) {
		int rc = new CommandLine(new App()).execute(args);
		System.exit(rc);
	}
}
