package com.yelloowstone.nf2t.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.tika.Tika;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public abstract class AbstractSubCommand implements Callable<Integer>, IVersionProvider {

	@Spec
	private CommandSpec spec;
	private final FlowFilePackageVersions packageVersions;
	private final ObjectMapper mapper;

	private final Tika tika;

	public AbstractSubCommand() {
		super();
		this.packageVersions = new FlowFilePackageVersions();
		this.mapper = new ObjectMapper();
		this.tika = new Tika();
	}
	
	public CommandSpec getSpec() {
		return spec;
	}

	public FlowFilePackageVersions getPackageVersions() {
		return packageVersions;
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public Tika getTika() {
		return tika;
	}

	@Override
    public String[] getVersion() throws Exception{
    	return new String[] {};
    }
	
	public FlowFilePackageVersion getPackageVersion(final int version) {
		return packageVersions.get(version);
	}
	
	public Map<String, String> generateDefaultAttributes(final Path path, final long contentSize) throws IOException {
		return FlowFileUtils.generateDefaultAttributes(getTika(), path, contentSize);
	}
	
	public FlowFileStreamResult createResult(final int version, 
			String extension, 
			final boolean uuidFilenames,
			final String inputOption, 
			String outputOption, 
			final String resultsOption,
			final Map<String, String> attributeExpressions, 
			boolean keepAttributes) {
		final Path inputPath = Paths.get(inputOption == null ? "." : inputOption);
		final Path outputPath = outputOption == null || outputOption.length() <= 0 ? null : Paths.get(outputOption);
		Path resultsPath = resultsOption == null ? null : Paths.get(resultsOption);

		if (extension.length() <= 0) {
			extension = packageVersions.get(version).getFileExtension();
		}

		if (resultsPath != null && Files.isDirectory(resultsPath)) {
			resultsPath = resultsPath.resolve("results.json");
		}

		long unixTime = System.currentTimeMillis() / 1000L;
		return new FlowFileStreamResult(version, extension, uuidFilenames, inputPath, outputPath, resultsPath, unixTime,
				attributeExpressions, keepAttributes);
	}
	
	public boolean printResult(final FlowFileStreamResult result) {
		try {
			String x = this.mapper.writer().writeValueAsString(result);
			spec.commandLine().getOut().println(x);

			if (result.getResultsPath() != null) {
				Files.write(result.getResultsPath(), x.getBytes(StandardCharsets.UTF_8));
			}

		} catch (IOException e) {
			e.printStackTrace(spec.commandLine().getErr());
			return true;
		}

		return false;
	}
}
