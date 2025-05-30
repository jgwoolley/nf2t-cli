package com.yelloowstone.nf2t.cli;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.yelloowstone.nf2t.cli.flowfiles.SubCommandFlowFiles;
import com.yelloowstone.nf2t.cli.jars.SubCommandJars;
import com.yelloowstone.nf2t.mavenCentralCli.SubCommandGenerateMavenCentralZip;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * @author 26191568+jgwoolley@users.noreply.github.com
 */
@Command(name = "nf2t", versionProvider = App.class, subcommands = { SubCommandFlowFiles.class,
		SubCommandJars.class,
		SubCommandGenerateMavenCentralZip.class
		}, mixinStandardHelpOptions = true, description = "A Java CLI for doing development involving Java, Maven, and Apache NiFi.")
public class App implements Callable<Integer>, IVersionProvider {
	public static final String FILE_SIZE_ATTRIBUTE = "size";

	@Spec
	private CommandSpec spec;

	public App() {
		super();
	}

	@Override
	public Integer call() throws Exception {
		throw new picocli.CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand.");
	}

	public static void main(String[] args) throws IOException {
		final App app = new App();
		final CommandLine cmd = new CommandLine(app);
		int rc = cmd.execute(args);
		System.exit(rc);
	}

	@Override
	public String[] getVersion() throws IOException {
		try {
			final ClassLoader classLoader = this.getClass().getClassLoader();
			final URL url = classLoader.getResource("META-INF/MANIFEST.MF");
			final Manifest manifest = new Manifest(url.openStream());
			final Attributes attributes = manifest.getMainAttributes();
			final String version = attributes.getValue("Implementation-Version");
			return new String[] { spec.qualifiedName() + " v" + version };
		} catch(Exception e) {
			return new String[] { spec.qualifiedName() };
		}
	}
}
