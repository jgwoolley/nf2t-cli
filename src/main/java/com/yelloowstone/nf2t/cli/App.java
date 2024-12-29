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
import java.util.List;
import java.util.Map;
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
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class App {
	public static final String ATTRIBUTES_EXTENSION = ".attributes.json";
	public static final String PACKAGE_EXTENSION = ".pkg";
	
	private final PrintStream stderr;
	private final PrintStream stdout;
	private final ObjectMapper mapper;
	private final TypeFactory factory;
	private final Options options;
	private final Map<Integer, Supplier<FlowFileUnpackager>> unpackagers;
	private final Map<Integer, Supplier<FlowFilePackager>> packagers;
	
	public App(final PrintStream stdout, final PrintStream stderr) {
		this.stdout = stdout;
		this.stderr = stderr;
		this.mapper = new ObjectMapper();
		this.factory = TypeFactory.defaultInstance();
		this.options = new Options();
		this.unpackagers = Map.of(3, () -> new FlowFileUnpackagerV3(), 2, () -> new FlowFileUnpackagerV2(), 1, () -> new FlowFileUnpackagerV1());
		this.packagers = Map.of(3, () -> new FlowFilePackagerV3(), 2, () -> new FlowFilePackagerV2(), 1, () -> new FlowFilePackagerV1());
	
		if(!this.unpackagers.keySet().containsAll(this.packagers.keySet())) {
			throw new IllegalArgumentException("Provided versions do not match.");
		}
		
		final List<String> actions = List.of(Action.values()).stream().map(x -> x.toString().toLowerCase()).sorted().collect(Collectors.toList());
		final List<String> versions = this.unpackagers.keySet().stream().map(x -> x.toString()).sorted().collect(Collectors.toList());
		
		options.addOption("h", "help", false, "Shows help information.");
		options.addRequiredOption("v", "version", true, "The Apache FlowFile version <" + String.join(",", versions) + ">.");
		options.addRequiredOption("a", "action", true, "The action <" + String.join(",", actions)  + ">.");
		options.addRequiredOption("i", "in", true, "The input directory path.");
		options.addRequiredOption("o", "out", true, "The output directory path.");
	}

	private Map<String,String> parseJsonAttributes(final Path attributesPath) throws IOException {
		final MapType type = factory.constructMapType(HashMap.class, String.class, String.class);
		final InputStream inputStream = Files.newInputStream(attributesPath);
        return mapper.readValue(inputStream, type);
	}
	
	private boolean updateAttributePaths(final Path path, final Map<String, Path> attributePaths) {
		final String fileName = path.getFileName().toString();
		if(fileName.endsWith(App.ATTRIBUTES_EXTENSION)) {
			final String payloadFileName = fileName.substring(0, fileName.length() - App.ATTRIBUTES_EXTENSION.length());
			
			attributePaths.put(payloadFileName, path);
			return false;
		}
		
		return true;
	}
	
	private SuccessResult packageFile(final FlowFilePackager packager, final Result result, final Path inputPath, final Map<String, Path> attributePaths) throws IOException {
		final long contentSize = Files.size(inputPath);
		final String fileName = inputPath.getFileName().toString();
		final Path attributePath = attributePaths.get(fileName);
		final Path absolutePath = inputPath.toAbsolutePath();
		final Path outputFilePath = result.outputPath.resolve(inputPath.getFileName() + PACKAGE_EXTENSION);

		Map<String, String> attributes = null;
		
		if(attributePath != null) {
			attributes = parseJsonAttributes(attributePath);
		}
		
		if(attributes == null) {
			attributes = new HashMap<>();
			attributes.put(CoreAttributes.FILENAME.key(), fileName);
			attributes.put(CoreAttributes.PATH.key(), absolutePath.getParent().toString());
			attributes.put(CoreAttributes.ABSOLUTE_PATH.key(), absolutePath.toString());
			attributes.put("size", Long.toString(contentSize));														
		}
		
        final InputStream inputStream = Files.newInputStream(inputPath);
        final OutputStream outputStream = Files.newOutputStream(outputFilePath);
        packager.packageFlowFile(inputStream, outputStream, attributes, contentSize);
        
        return new SuccessResult(outputFilePath, inputPath, attributePath, contentSize);
	}
	
	public void packageFiles(final Result result) {
		if(!Files.isDirectory(result.inputPath)) {
			final Exception exception = new FileNotFoundException("Flowfile content not found at path: " + result.inputPath.toAbsolutePath().toString());		
			result.errors.add(new ErrorResult(exception, result.inputPath));
		}   
		else if(!Files.isDirectory(result.outputPath)) {
			final Exception exception = new FileNotFoundException("Output path not found: " + result.outputPath.toAbsolutePath().toString());
			result.errors.add(new ErrorResult(exception, result.inputPath));
		} else {
			final FlowFilePackager packager = this.packagers.get(result.version).get();
			final Map<String, Path> attributePaths = new HashMap<>();
			
			try(final Stream<Path> files = Files.list(result.inputPath)) {			
				files.filter(path -> { 
					return this.updateAttributePaths(path, attributePaths);
				}).forEach(path -> {
					try {
						final SuccessResult outputFile = this.packageFile(packager, result, path, attributePaths);
						result.outputFiles.add(outputFile);
						
					} catch (IOException e) {
						result.errors.add(new ErrorResult(e, path));
					}
				});
			} catch (IOException e) {
				result.errors.add(new ErrorResult(e, result.inputPath));
			}
		}
	}
	
	private SuccessResult unpackageFile(final FlowFileUnpackager unpackager, final Result result, final Path inputPath) throws IOException {
		final String fileName = inputPath.getFileName().toString();	
		final String contentFileName = fileName.substring(0, fileName.length() - PACKAGE_EXTENSION.length());
		
		final Path flowFilePath = inputPath.toAbsolutePath();
		final Path contentPath = result.outputPath.resolve(contentFileName);
		final Path attributesPath = result.outputPath.resolve(contentFileName + ATTRIBUTES_EXTENSION);
		
        final InputStream in = Files.newInputStream(flowFilePath);
        final OutputStream out = Files.newOutputStream(contentPath);
		       
		final Map<String,String> attributes = unpackager.unpackageFlowFile(in, out);
		final long contentSize = Files.size(contentPath);

        final OutputStream attributesOutputStream = Files.newOutputStream(attributesPath);		
		this.mapper.writer().writeValue(attributesOutputStream, attributes);
		
		if(unpackager.hasMoreData()) {
			// See: https://github.com/apache/nifi/blob/main/nifi-extension-bundles/nifi-standard-bundle/nifi-standard-processors/src/main/java/org/apache/nifi/processors/standard/UnpackContent.java#L634
			throw new UnsupportedOperationException("FlowFile had more data, this operation is currently unsupported.");
		}
		
        return new SuccessResult(flowFilePath, contentPath, attributesPath, contentSize);
	}
	
	public void unpackageFiles(final Result result) {
		if(!Files.isDirectory(result.inputPath)) {
			final Exception exception = new FileNotFoundException("Flowfile content not found at path: " + result.inputPath.toAbsolutePath().toString());		
			result.errors.add(new ErrorResult(exception, result.inputPath));
		}   
		else if(!Files.isDirectory(result.outputPath)) {
			final Exception exception = new FileNotFoundException("Output path not found: " + result.outputPath.toAbsolutePath().toString());
			result.errors.add(new ErrorResult(exception, result.inputPath));
		}
		else {
			final FlowFileUnpackager unpackager = this.unpackagers.get(result.version).get();
			
			try(final Stream<Path> files = Files.list(result.inputPath)) {			
				files.forEach(path -> {
					try {
						final SuccessResult outputFile = this.unpackageFile(unpackager, result, path);
						result.outputFiles.add(outputFile);
						
					} catch (Exception e) {
						result.errors.add(new ErrorResult(e, path));
					}
				});
			} catch (IOException e) {
				result.errors.add(new ErrorResult(e, result.inputPath));
			}
		}		
	}
	
	public void parse(final CommandLineParser parser, final String[] args) {
		try {
			final CommandLine commandLine = parser.parse(this.options, args);			
			if (commandLine.hasOption("h")) {
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("CommandLineParameters", this.options);
				return;
			}
			
			final String inputOption = commandLine.getOptionValue("i");
			final String outputOption = commandLine.getOptionValue("o");
			
			final Path inputPath = Paths.get(inputOption);
			final Path outputPath = Paths.get(outputOption != null ? outputOption : inputOption);
			final int version = Integer.parseInt(commandLine.getOptionValue("v"));
			
			final String actionString = commandLine.getOptionValue("a").toUpperCase();
			final Action action = Action.valueOf(actionString);

			long unixTime = System.currentTimeMillis() / 1000L;
			final Result result = new Result(version, inputPath, outputPath, unixTime);
						
			if(Action.PACKAGE == action) {
				packageFiles(result);
			} else if(Action.UNPACKAGE == action) {
				unpackageFiles(result);
			}
			
			final PrintStream out = result.errors.isEmpty() ? this.stdout: this.stderr;			
			out.println(this.mapper.writer().writeValueAsString(result));
			
		} catch (ParseException | JsonProcessingException e) {
			this.stderr.println(e);
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("CommandLineParameters", this.options);
		}
	}
	
	public static void main(String[] args) {
		final CommandLineParser parser = new DefaultParser();
		final App app = new App(System.out, System.err);
		app.parse(parser, args);
		
	}
}
