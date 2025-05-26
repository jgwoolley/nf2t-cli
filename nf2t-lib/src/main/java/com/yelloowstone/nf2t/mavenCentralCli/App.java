package com.yelloowstone.nf2t.mavenCentralCli;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.ParserConfigurationException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "Build Maven Central Zip", versionProvider = App.class, subcommands = { SubCommandGenerateDocs.class,
		SubCommandGenerateMavenCentralZip.class}, mixinStandardHelpOptions = true )
public class App implements Callable<Integer>, IVersionProvider {

	@Spec
	private CommandSpec spec;

	@Override
	public Integer call() throws Exception {
		throw new picocli.CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand.");
	}

	public static void main(String[] args) throws ParserConfigurationException, IOException {
		int rc = new CommandLine(new App()).execute(args);
		System.exit(rc);
	}

	@Override
	public String[] getVersion() throws Exception {
		final ClassLoader classLoader = this.getClass().getClassLoader();
		final URL url = classLoader.getResource("META-INF/MANIFEST.MF");
		final Manifest manifest = new Manifest(url.openStream());
		final Attributes attributes = manifest.getMainAttributes();
		final String version = attributes.getValue("Implementation-Version");
		return new String[] { spec.qualifiedName() + " v" + version };
	}
}
