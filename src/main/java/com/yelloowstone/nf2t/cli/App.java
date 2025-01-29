package com.yelloowstone.nf2t.cli;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.nifi.attribute.expression.language.PreparedQuery;
import org.apache.nifi.attribute.expression.language.Query;
import org.apache.nifi.attribute.expression.language.StandardEvaluationContext;
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

	private void unpackageFlowFileInputStream(final FlowFileStreamResult result, final InputStream is,
			final SourceFile source, final FlowFilePackageVersion packageVersion) {
		final List<FlowFileResult> outputFiles = result.getOutputFiles();
		final List<FlowFileErrorResult> errors = result.getErrors();
		final FlowFileUnpackager unpackager = packageVersion.getUnpackager();

		try {
			do {
				final Path contentPath = result.getOutputPath().resolve(UUID.randomUUID().toString() + ".dat");
				FlowFileResult flowFileResult = null;
				try (OutputStream out = contentPath == null ? OutputStream.nullOutputStream(): Files.newOutputStream(contentPath)) {
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
				unpackageInputStream(result, zipIs, new SourceFile(source, newAbsolutePath, newFilename, entry.getSize()));
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
					unpackageInputStream(result, tarInputStream, new SourceFile(source, newAbsolutePath, newFilename, entry.getSize()));
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

		if(absolutePath == null) {
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

	@Command(name = "unpackage", description = "Unpackages FlowFileStream(s), information regarding this operation sent to standard out. See command arguments for furher details.")
	public Integer unpackageFlowFileStream(
			@Option(names = { "-v", "--version" }, defaultValue = "3", description = {
					FlowFileStreamResult.VERSION_DESCRIPTION + " incoming FlowFileStream(s)." }) final int version,
			@Option(names = { "-e",
					"--extension" }, defaultValue = ".pkg", description = FlowFileStreamResult.EXTENSION_UNPACKAGE_DESCRIPTION) final String extension,
			@Option(names = { "-i", "--in" }, description = "The input path."
					+ FlowFileStreamResult.INPUTPATH_UNPACKAGE_DESCRIPTION, required = true) final String inputOption,
			@Option(names = { "-o", "--out" }, description = "The output path."
					+ FlowFileStreamResult.OUTPUTPATH_UNPACKAGE_DESCRIPTION, required = false) final String outputOption,
			@Option(names = { "-u",
					"--uuid" }, description = FlowFileStreamResult.UUID_DESCRIPTION, defaultValue = "true") final boolean uuidFilenames,
			@Option(names = {"-r", "--results"}, description=FlowFileStreamResult.RESULTS_PATH_DESCRIPTION) final String resultsPath) {
		final FlowFileStreamResult result = createResult(version, extension, uuidFilenames, inputOption, outputOption, resultsPath, new HashMap<>(), true);

		// Unpack Frequently Used Variables
		final Path inputPath = result.getInputPath();
		final Path outputPath = result.getOutputPath();
		final List<FlowFileErrorResult> errors = result.getErrors();

		final SourceFile source = SourceFile.fromPath(null, inputPath);
		
		// Get Packager For Current Version
		final FlowFilePackageVersion packageVersion = packageVersions.get(version);
		if (packageVersion == null) {
			errors.add(new FlowFileErrorResult(new Exception("Bad FlowFile Package Version Given: " + version),
					source));
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
						try(final InputStream is = Files.newInputStream(x)) {
							unpackageInputStream(result, is, newSource);
						} catch (IOException e) {
							errors.add(new FlowFileErrorResult(e, newSource));
						}
					});
				} catch (IOException e) {
					errors.add(new FlowFileErrorResult(e, source));
				}
			} else if (Files.isRegularFile(inputPath)) {
				try(final InputStream is = Files.newInputStream(inputPath)) {
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

	@Command(name = "package", description = "Packages FlowFileStream(s), information regarding this operation sent to standard out. See command arguments for furher details.")
	public Integer packageFlowFileStream(
			@Option(names = { "-v", "--version" }, defaultValue = "3", description = {
					FlowFileStreamResult.VERSION_DESCRIPTION + " resulting FlowFile." }) final int version,
			@Option(names = { "-e",
					"--extension" }, defaultValue = "", description = FlowFileStreamResult.EXTENSION_PACKAGE_DESCRIPTION) final String extension,
			@Option(names = { "-i", "--in" }, description = "The input path."
					+ FlowFileStreamResult.INPUTPATH_PACKAGE_DESCRIPTION, required = true) final String inputOption,
			@Option(names = { "-o", "--out" }, description = "The output path."
					+ FlowFileStreamResult.OUTPUTPATH_PACKAGE_DESCRIPTION, required = true) final String outputOption,
			@Option(names = {"-r", "--results"}, description=FlowFileStreamResult.RESULTS_PATH_DESCRIPTION) final String resultsPath,
			@Option(names = {"-a", "--attribute"}, description=FlowFileStreamResult.DEFAULT_ATTRIBUTES_DESCRIPTION, required=false) final Map<String,String> rawAttributeExpressions,
			@Option(names = {"-k", "--keep-attributes"}, description=FlowFileStreamResult.KEEP_ATTRIBUTES_DESCRIPTION, defaultValue="true", required=false) final boolean keepAttributes) {
		final FlowFileStreamResult result = createResult(version, extension, true, inputOption, outputOption, resultsPath, rawAttributeExpressions == null ? new HashMap<>(): rawAttributeExpressions, keepAttributes);

		// Unpack Frequently Used Variables
		final Path inputPath = result.getInputPath();
		
		Path outputPath = result.getOutputPath();
		
		if(outputPath == null) {
			throw new IllegalArgumentException("Output Path must not be empty.");
		}
		
		final List<FlowFileErrorResult> errors = result.getErrors();

		final SourceFile source = SourceFile.fromPath(null, inputPath);
		final SourceFile output = SourceFile.fromPath(null, outputPath);

		
		// Get Packager For Current Version
		final FlowFilePackageVersion packageVersion = packageVersions.get(version);
		if (packageVersion == null) {
			errors.add(new FlowFileErrorResult(new Exception("Bad FlowFile Package Version Given: " + version),
					source));
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
				result.getErrors().add(new FlowFileErrorResult(e, output));
				printResult(result);
				return 1;
			}
		} else {
			final Exception exception = new FileNotFoundException(
					"Flowfile content not found at path: " + inputPath.toAbsolutePath().toString());
			result.getErrors().add(new FlowFileErrorResult(exception, output));
			printResult(result);
			return 1;
		}

		final FlowFilePackager packager = packageVersion.getPackager();

		final Map<String, PreparedQuery> attributeExpressions = new HashMap<>();

		for(final Entry<String, String> attribute: result.getDefaultAttributes().entrySet()) {
			attributeExpressions.put(attribute.getKey(), Query.prepare(attribute.getValue()));
		}
					
		try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
			for (Path contentPath : contentPaths) {
				final SourceFile content = SourceFile.fromPath(null, outputPath);

				try {
					final long contentSize = Files.size(contentPath);
					final Map<String, String> defaultAttributes = generateDefaultAttributes(contentPath, contentSize);
					defaultAttributes.putAll(result.getDefaultAttributes());
					final Map<String, String> attributes = new HashMap<>();
					
					if(result.keepAttributes) {
						attributes.putAll(defaultAttributes);
					}
					
					final StandardEvaluationContext evaluationContext = new StandardEvaluationContext(defaultAttributes);
					for(final Entry<String,PreparedQuery> attribute: attributeExpressions.entrySet()) {
						attributes.put(attribute.getKey(), attribute.getValue().evaluateExpressions(evaluationContext, null));
					}
										
					try (InputStream inputStream = Files.newInputStream(contentPath)) {
						packager.packageFlowFile(inputStream, outputStream, attributes, contentSize);

						final FlowFileResult packageResult = new FlowFileResult(output, source,
								attributes, contentSize);
						result.getOutputFiles().add(packageResult);
					}

				} catch (IOException e) {
					result.getErrors().add(new FlowFileErrorResult(e, content));
					printResult(result);
					return 1;
				}
			}
		} catch (IOException e) {
			result.getErrors().add(new FlowFileErrorResult(e, output));
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
			
			if(result.getResultsPath() != null) {
				Files.write(result.getResultsPath(), x.getBytes(StandardCharsets.UTF_8));
			}
			
		} catch (IOException e) {
			e.printStackTrace(spec.commandLine().getErr());
			return true;
		}

		return false;
	}

	public FlowFileStreamResult createResult(final int version, String extension, final boolean uuidFilenames,
			final String inputOption, String outputOption,  final String resultsOption, final Map<String, String> attributeExpressions, boolean keepAttributes) {
		final Path inputPath = Paths.get(inputOption == null ? "." : inputOption);
		final Path outputPath = outputOption == null || outputOption.length() <= 0 ? null : Paths.get(outputOption);
		Path resultsPath = resultsOption == null ? null : Paths.get(resultsOption);
		
		if (extension.length() <= 0) {
			extension = packageVersions.get(version).getFileExtension();
		}

		if(resultsPath != null && Files.isDirectory(resultsPath)) {
			resultsPath = resultsPath.resolve("results.json");
		}
		
		long unixTime = System.currentTimeMillis() / 1000L;
		return new FlowFileStreamResult(version, extension, uuidFilenames, inputPath, outputPath, resultsPath, unixTime, attributeExpressions, keepAttributes);
	}
	
	public static Map<String, String> generateDefaultAttributes(final Path path, final long contentSize) throws IOException {
		final Map<String, String> attributes = new HashMap<>();
		
		attributes.put(CoreAttributes.FILENAME.key(), path.getFileName().toString());
		if(path.getParent() != null) {
			attributes.put(CoreAttributes.PATH.key(), path.getParent().toString());
		}
		attributes.put(CoreAttributes.ABSOLUTE_PATH.key(), path.toString());
		attributes.put(FILE_SIZE_ATTRIBUTE, Long.toString(contentSize));
		
		return attributes;
	}

	public static void main(String[] args) {
		int rc = new CommandLine(new App()).execute(args);
		System.exit(rc);
	}
}
