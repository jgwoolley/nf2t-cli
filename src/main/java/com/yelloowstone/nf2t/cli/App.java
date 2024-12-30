package com.yelloowstone.nf2t.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

@Command(name = "nf2t", mixinStandardHelpOptions = true, description = "A Java CLI for parsing Apache NiFi FlowFiles.")
public class App implements Callable<Integer> {
	public static final String FILE_SIZE_ATTRIBUTE = "size";
	
    @Spec private CommandSpec spec;
	private final FlowFilePackageVersions packageVersions;
	private final ObjectMapper mapper;

	public App() {
		super();
		this.packageVersions = new FlowFilePackageVersions();
		this.mapper = new ObjectMapper();
	}
	
	@Command(name = "unpackage")
	public Integer unpackageFlowFileStream(@Option(names = {"-v", "--version"}, defaultValue="3", description= {FlowFileStreamResult.VERSION_DESCRIPTION + " incoming FlowFile."}) final int version, @Option(names= {"-i", "--in"}, description="The input path." + FlowFileStreamResult.INPUTPATH_UNPACKAGE_DESCRIPTION, required=true) final String inputOption, @Option(names= {"-o", "--out"}, description="The output path." + FlowFileStreamResult.OUTPUTPATH_UNPACKAGE_DESCRIPTION, required=true) final String outputOption, @Option(names= {"-u", "--uuid"}, description = { "Will make all unpackaged content filename(s) UUIDs, to prevent clobering." }, defaultValue="true") final boolean uuidFilenames) {
		final FlowFileStreamResult result = createResult(version, inputOption, outputOption);
		
		// Unpack Frequently Used Variables
		final Path inputPath = result.getInputPath();
		final Path outputPath = result.getOutputPath();
		final List<FlowFileErrorResult> errors = result.getErrors();
		final List<FlowFileResult> outputFiles = result.getOutputFiles();
		
		// Get Packager For Current Version
		final FlowFilePackageVersion packageVersion = packageVersions.get(version);
		if(packageVersion == null) {
			errors.add(new FlowFileErrorResult(new Exception("Bad FlowFile Package Version Given: " + version), inputPath));
			printResult(result);		
			return 1;
		}
		
		if (!Files.isDirectory(inputPath)) {
			final Exception exception = new FileNotFoundException(
					"Flowfile content not found at path: " + inputPath.toAbsolutePath().toString());
			errors.add(new FlowFileErrorResult(exception, inputPath));
		} else if (!Files.isDirectory(outputPath)) {
			final Exception exception = new FileNotFoundException(
					"Output path not found: " + outputPath.toAbsolutePath().toString());
			errors.add(new FlowFileErrorResult(exception, inputPath));
		} else {
			final FlowFileUnpackager unpackager = packageVersion.getUnpackager();

			try (final Stream<Path> files = Files.list(inputPath)) {
				files.forEach(flowFilePath -> {
					try {
						try(final InputStream in = Files.newInputStream(flowFilePath)) {
							do {
								final Path contentPath = result.getOutputPath().resolve(UUID.randomUUID().toString() + ".dat");
								FlowFileResult flowFileResult = null;
								try(OutputStream out = Files.newOutputStream(contentPath)) {
									final Map<String, String> attributes = unpackager.unpackageFlowFile(in, out);
									final long contentSize = Files.size(contentPath);
									flowFileResult = new FlowFileResult(flowFilePath, contentPath, attributes, contentSize);
									outputFiles.add(flowFileResult);
								} catch(Exception e) {
									throw new Exception("Could not unpackage " + flowFilePath, e);
								}
								
								if(uuidFilenames && flowFileResult != null) {
									String filename = flowFileResult.getAttributes().get(CoreAttributes.FILENAME.key());
									if(filename != null) {
										Path newContentPath = contentPath.getParent().resolve(filename);
										Files.move(contentPath, newContentPath);
										flowFileResult.setContentPath(newContentPath);
									}
								}
								
							} while(unpackager.hasMoreData());
						}							
					} catch (Exception e) {
						errors.add(new FlowFileErrorResult(e, flowFilePath));
					}
				});
			} catch (IOException e) {
				errors.add(new FlowFileErrorResult(e, inputPath));
			}
		}
		
		if(printResult(result)) {
			return 1;
		}
		
		return 0;
	}
		
	@Command(name = "package")
	public Integer packageFlowFileStream(@Option(names = {"-v", "--version"}, defaultValue="3", description={FlowFileStreamResult.VERSION_DESCRIPTION + " resulting FlowFile."}) final int version, @Option(names= {"-i", "--in"}, description="The input path." + FlowFileStreamResult.INPUTPATH_PACKAGE_DESCRIPTION, required=true) final String inputOption, @Option(names= {"-o", "--out"}, description="The output path." + FlowFileStreamResult.OUTPUTPATH_PACKAGE_DESCRIPTION, required=true) final String outputOption) {
		final FlowFileStreamResult result = createResult(version, inputOption, outputOption);
		
		// Unpack Frequently Used Variables
		final Path inputPath = result.getInputPath();
		Path outputPath = result.getOutputPath();
		final List<FlowFileErrorResult> errors = result.getErrors();
		
		// Get Packager For Current Version
		final FlowFilePackageVersion packageVersion = packageVersions.get(version);
		if(packageVersion == null) {
			errors.add(new FlowFileErrorResult(new Exception("Bad FlowFile Package Version Given: " + version), inputPath));
			printResult(result);		
			return 1;
		}
		
		if (Files.isDirectory(outputPath)) {
			outputPath = outputPath.resolve(FlowFileStreamResult.FLOWFILE_DEFAULT_FILENAME);
			result.setOutputPath(outputPath);
		}

		final List<Path> contentPaths = new LinkedList<>();
		if (Files.isRegularFile(inputPath)) {
			contentPaths.add(inputPath);
		} else if (Files.isDirectory(inputPath)) {
			try {
				Files.list(inputPath).filter(Files::isRegularFile).forEach(x -> contentPaths.add(x));
			} catch (IOException e) {
				result.getErrors().add(new FlowFileErrorResult(e, outputPath));
				printResult(result);		
				return 1;
			}
		} else {
			final Exception exception = new FileNotFoundException(
					"Flowfile content not found at path: " + inputPath.toAbsolutePath().toString());
			result.getErrors().add(new FlowFileErrorResult(exception, inputPath));
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

						final FlowFileResult packageResult = new FlowFileResult(outputPath, inputPath, attributes,
								contentSize);
						result.getOutputFiles().add(packageResult);
					}

				} catch (IOException e) {
					result.getErrors().add(new FlowFileErrorResult(e, contentPath));
					printResult(result);		
					return 1;
				}
			}
		} catch (IOException e) {
			result.getErrors().add(new FlowFileErrorResult(e, outputPath));
			printResult(result);		
			return 1;
		}

		if(printResult(result)) {
			return 1;
		}
		
		return 0;
	}

	@Command(name = "generateSchema")
	public Integer generateSchema() throws JsonProcessingException {
		SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
		mapper.acceptJsonFormatVisitor(FlowFileStreamResult.class, visitor);
		JsonSchema personSchema = visitor.finalSchema();		
		spec.commandLine().getOut().println(this.mapper.writer().writeValueAsString(personSchema));
		return 0;
	}
	
	
	@Override
	public Integer call() throws Exception {
	    spec.commandLine().getOut().println("Subcommand needed: 'unpackage', 'package' or 'generateSchema'");
	    return 1;
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
		
	public static FlowFileStreamResult createResult(final int version, final String inputOption, final String outputOption) {
		final Path inputPath = Paths.get(inputOption == null ? ".": inputOption);
		final Path outputPath = outputOption == null ? inputPath : Paths.get(outputOption) ;

		long unixTime = System.currentTimeMillis() / 1000L;
		return new FlowFileStreamResult(version, inputPath, outputPath, unixTime);
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
