package com.yelloowstone.nf2t.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.util.FlowFilePackager;
import org.apache.nifi.util.FlowFilePackagerV1;
import org.apache.nifi.util.FlowFilePackagerV2;
import org.apache.nifi.util.FlowFilePackagerV3;
import org.apache.nifi.util.FlowFileUnpackager;
import org.apache.nifi.util.FlowFileUnpackagerV1;
import org.apache.nifi.util.FlowFileUnpackagerV2;
import org.apache.nifi.util.FlowFileUnpackagerV3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
	public static final String PACKAGE_EXTENSION = ".pkg";
	public static final String FLOWFILE_DEFAULT_FILENAME = "flowFiles" + PACKAGE_EXTENSION;
	public static final String FILE_SIZE_ATTRIBUTE = "size";
	
	private final PrintStream stderr;
	private final PrintStream stdout;
	private final ObjectMapper mapper;
	private final Options options;
	private final Map<Integer, Supplier<FlowFileUnpackager>> unpackagers;
	private final Map<Integer, Supplier<FlowFilePackager>> packagers;

	public App(final PrintStream stdout, final PrintStream stderr) {
		this.stdout = stdout;
		this.stderr = stderr;
		this.mapper = new ObjectMapper();
		this.options = new Options();
		this.unpackagers = Map.of(3, () -> new FlowFileUnpackagerV3(), 2, () -> new FlowFileUnpackagerV2(), 1,
				() -> new FlowFileUnpackagerV1());
		this.packagers = Map.of(3, () -> new FlowFilePackagerV3(), 2, () -> new FlowFilePackagerV2(), 1,
				() -> new FlowFilePackagerV1());

		if (!this.unpackagers.keySet().containsAll(this.packagers.keySet())) {
			throw new IllegalArgumentException("Provided versions do not match.");
		}

		final List<String> actions = List.of(Action.values()).stream().map(x -> x.toString().toLowerCase()).sorted()
				.collect(Collectors.toList());
		final List<String> versions = this.unpackagers.keySet().stream().map(x -> x.toString()).sorted()
				.collect(Collectors.toList());

		options.addOption("h", "help", false, "Shows help information.");
		options.addRequiredOption("v", "version", true,
				"The Apache FlowFile version <" + String.join(",", versions) + ">.");
		options.addRequiredOption("a", "action", true, "The action <" + String.join(",", actions) + ">.");
		options.addRequiredOption("i", "in", true,
				"The input path. For unpackage, a directory or file containing a FlowFileStream. For package, a directory or file containing FlowFile content.");
		options.addRequiredOption("o", "out", true,
				"The output path. For unpackage, a directory containing the FlowFile content. For package, a directory where the \""
						+ FLOWFILE_DEFAULT_FILENAME + "\" will be created or the name of the file.");
	}

	public void packageFiles(final FlowFileStreamResult result) {
		final Path inputPath = result.getInputPath();
		Path outputPath = result.getOutputPath();

		if (Files.isDirectory(outputPath)) {
			outputPath = outputPath.resolve(FLOWFILE_DEFAULT_FILENAME);
			result.setOutputPath(outputPath);
		}

		final List<Path> contentPaths = new LinkedList<>();
		if (Files.isRegularFile(inputPath)) {
			contentPaths.add(inputPath);
		} else if (Files.isDirectory(inputPath)) {
			try {
				Files.list(inputPath).forEach(x -> contentPaths.add(x));
			} catch (IOException e) {
				result.getErrors().add(new ErrorResult(e, outputPath));
				e.printStackTrace(this.stderr);
				return;
			}
		} else {
			final Exception exception = new FileNotFoundException(
					"Flowfile content not found at path: " + inputPath.toAbsolutePath().toString());
			result.getErrors().add(new ErrorResult(exception, inputPath));
			exception.printStackTrace(this.stderr);
			return;
		}

		final int version = result.getVersion();

		final FlowFilePackager packager = this.packagers.get(version).get();
		if (packager == null) {
			final Exception exception = new IllegalArgumentException(
					"No FlowFilePackager available for given version: " + version);
			result.getErrors().add(new ErrorResult(exception, inputPath));
			exception.printStackTrace(this.stderr);
			return;
		}

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
					result.getErrors().add(new ErrorResult(e, contentPath));
					e.printStackTrace(this.stderr);
					break;
				}
			}
		} catch (IOException e) {
			result.getErrors().add(new ErrorResult(e, outputPath));
			e.printStackTrace(this.stderr);
			return;
		}
	}

	public void unpackageFiles(final FlowFileStreamResult result) {
		final Path inputPath = result.getInputPath();
		final Path outputPath = result.getOutputPath();
		final List<ErrorResult> errors = result.getErrors();
		final int version = result.getVersion();
		final List<FlowFileResult> outputFiles = result.getOutputFiles();

		if (!Files.isDirectory(inputPath)) {
			final Exception exception = new FileNotFoundException(
					"Flowfile content not found at path: " + inputPath.toAbsolutePath().toString());
			errors.add(new ErrorResult(exception, inputPath));
		} else if (!Files.isDirectory(outputPath)) {
			final Exception exception = new FileNotFoundException(
					"Output path not found: " + outputPath.toAbsolutePath().toString());
			errors.add(new ErrorResult(exception, inputPath));
		} else {
			final FlowFileUnpackager unpackager = this.unpackagers.get(version).get();

			try (final Stream<Path> files = Files.list(inputPath)) {
				files.forEach(flowFilePath -> {
					try {
						try(final InputStream in = Files.newInputStream(flowFilePath)) {
							do {
								final Path contentPath = result.getOutputPath().resolve(UUID.randomUUID().toString());
								try(OutputStream out = Files.newOutputStream(contentPath)) {
									final Map<String, String> attributes = unpackager.unpackageFlowFile(in, out);
									final long contentSize = Files.size(contentPath);
									FlowFileResult flowFileResult = new FlowFileResult(flowFilePath, contentPath, attributes, contentSize);
									outputFiles.add(flowFileResult);
								} catch(Exception e) {
									throw new Exception("Could not unpackage " + flowFilePath, e);
								}
																
							} while(unpackager.hasMoreData());
						}							
					} catch (Exception e) {
						errors.add(new ErrorResult(e, flowFilePath));
						e.printStackTrace(stderr);
					}
				});
			} catch (IOException e) {
				errors.add(new ErrorResult(e, inputPath));
				e.printStackTrace(this.stderr);
			}
		}
	}

	public FlowFileStreamResult parse(final CommandLineParser parser, final String[] args) {
		try {
			final CommandLine commandLine = parser.parse(this.options, args);
			if (commandLine.hasOption("h")) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("CommandLineParameters", this.options);
				return null;
			}

			final String inputOption = commandLine.getOptionValue("i");
			final String outputOption = commandLine.getOptionValue("o");

			final Path inputPath = Paths.get(inputOption);
			final Path outputPath = Paths.get(outputOption != null ? outputOption : inputOption);
			final int version = Integer.parseInt(commandLine.getOptionValue("v"));

			final String actionString = commandLine.getOptionValue("a").toUpperCase();
			final Action action = Action.valueOf(actionString);

			long unixTime = System.currentTimeMillis() / 1000L;
			final FlowFileStreamResult result = new FlowFileStreamResult(version, inputPath, outputPath, unixTime);

			if (Action.PACKAGE == action) {
				packageFiles(result);
			} else if (Action.UNPACKAGE == action) {
				unpackageFiles(result);
			}

			this.stdout.print(this.mapper.writer().writeValueAsString(result));
			return result;

		} catch (ParseException | JsonProcessingException e) {
			this.stderr.print(e);
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CommandLineParameters", this.options);
		}

		return null;
	}

	public Map<Integer, Supplier<FlowFileUnpackager>> getUnpackagers() {
		return unpackagers;
	}

	public Map<Integer, Supplier<FlowFilePackager>> getPackagers() {
		return packagers;
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
		final CommandLineParser parser = new DefaultParser();
		final App app = new App(System.out, System.err);
		app.parse(parser, args);

	}
}
