package com.yelloowstone.nf2t.cli.jars;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "validateJar", aliases = { "validateNar" }, description = "This command parses a Java Archive file (JAR), as well as the extension-manifest.xml from a NiFi Archive File (NAR), which should be present in a valid NAR.")
public class SubCommandValidateJar implements Callable<Integer>, IVersionProvider {

	@Parameters
	private Path[] narPaths;
	private final ObjectMapper mapper;
	private final JarParser jarParser;

	@Spec
	private CommandSpec spec;

	public SubCommandValidateJar() {
		super();
		this.mapper = new ObjectMapper();
		this.jarParser = new JarParser();
	}

	@Override
	public String[] getVersion() throws Exception {
		// This method cannot be called, it it a sub-method.
		return new String[] {};
	}

	@Override
	public Integer call() throws Exception {
		for (final Path narPath : narPaths) {
			if (!Files.isRegularFile(narPath)) {
				System.err.println(narPath.toAbsolutePath());
				return 1;
			}

			try (final InputStream fileInputStream = Files.newInputStream(narPath)) {				
				final iJarDetails jarDetails = jarParser.parse(fileInputStream).build();

				final String result = this.mapper.writer().writeValueAsString(jarDetails);
				getSpec().commandLine().getOut().println(result);

			} catch (Exception e) {
				e.printStackTrace();
				return 1;
			}
		}

		return 0;
	}

	private CommandSpec getSpec() {
		return this.spec;
	}

	public ObjectMapper getMapper() {
		return this.mapper;
	}
}
