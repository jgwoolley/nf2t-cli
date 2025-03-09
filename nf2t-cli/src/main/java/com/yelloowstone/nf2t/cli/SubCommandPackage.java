package com.yelloowstone.nf2t.cli;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.nifi.attribute.expression.language.PreparedQuery;
import org.apache.nifi.attribute.expression.language.Query;
import org.apache.nifi.attribute.expression.language.StandardEvaluationContext;
import org.apache.nifi.util.FlowFilePackager;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "package", description = "Packages FlowFileStream(s), information regarding this operation sent to standard out. See command arguments for furher details.")
public class SubCommandPackage extends AbstractSubCommand {
	@Option(names = { "-v", "--version" }, defaultValue = "3", description = {
			FlowFileStreamResult.VERSION_DESCRIPTION + " resulting FlowFile." })
	private int version;
	@Option(names = { "-e",
			"--extension" }, defaultValue = "", description = FlowFileStreamResult.EXTENSION_PACKAGE_DESCRIPTION)
	private String extension;
	@Option(names = { "-i", "--in" }, description = "The input path."
			+ FlowFileStreamResult.INPUTPATH_PACKAGE_DESCRIPTION, required = true)
	private String inputOption;
	@Option(names = { "-o", "--out" }, description = "The output path."
			+ FlowFileStreamResult.OUTPUTPATH_PACKAGE_DESCRIPTION, required = true)
	private String outputOption;
	@Option(names = { "-r", "--results" }, description = FlowFileStreamResult.RESULTS_PATH_DESCRIPTION)
	private String resultsPath;
	@Option(names = { "-a",
			"--attribute" }, description = FlowFileStreamResult.DEFAULT_ATTRIBUTES_DESCRIPTION, required = false)
	private Map<String, String> rawAttributeExpressions;
	@Option(names = { "-k",
			"--keep-attributes" }, description = FlowFileStreamResult.KEEP_ATTRIBUTES_DESCRIPTION, defaultValue = "true", required = false)
	private boolean keepAttributes;
	
	@Override
	public Integer call() throws Exception {
		final FlowFileStreamResult result = createResult(version, extension, false, inputOption, outputOption,
				resultsPath, rawAttributeExpressions == null ? new HashMap<>() : rawAttributeExpressions,
				keepAttributes);

		// Unpack Frequently Used Variables
		final Path inputPath = result.getInputPath();

		Path outputPath = result.getOutputPath();

		if (outputPath == null) {
			throw new IllegalArgumentException("Output Path must not be empty.");
		}

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

		if (Files.isDirectory(outputPath)) {
			outputPath = packageVersion.getDefaultName(outputPath);
			result.setOutputPath(outputPath);
		}

		final SourceFile output = SourceFile.fromPath(null, outputPath);

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

		for (final Entry<String, String> attribute : result.getDefaultAttributes().entrySet()) {
			attributeExpressions.put(attribute.getKey(), Query.prepare(attribute.getValue()));
		}

		try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
			for (Path contentPath : contentPaths) {
				final SourceFile content = SourceFile.fromPath(null, outputPath);

				try {
					final long contentSize = Files.size(contentPath);
					final Map<String, String> defaultAttributes = generateDefaultAttributes(contentPath,
							contentSize);
					defaultAttributes.putAll(result.getDefaultAttributes());
					final Map<String, String> attributes = new HashMap<>();

					if (result.keepAttributes) {
						attributes.putAll(defaultAttributes);
					}

					final StandardEvaluationContext evaluationContext = new StandardEvaluationContext(
							defaultAttributes);
					for (final Entry<String, PreparedQuery> attribute : attributeExpressions.entrySet()) {
						attributes.put(attribute.getKey(),
								attribute.getValue().evaluateExpressions(evaluationContext, null));
					}

					try (InputStream inputStream = Files.newInputStream(contentPath)) {
						packager.packageFlowFile(inputStream, outputStream, attributes, contentSize);

						final FlowFileResult packageResult = new FlowFileResult(output, source, attributes,
								contentSize);
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
	
	
}
