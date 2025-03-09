package com.yelloowstone.nf2t.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * @author 26191568+jgwoolley@users.noreply.github.com
 */
@Command(name = "nf2t", subcommands = { SubCommandUnpackage.class, SubCommandPackage.class,
		SubCommandGenerateSchema.class }, mixinStandardHelpOptions = true, description = "A Java CLI for parsing Apache NiFi FlowFileStreams. One or more FlowFiles can be serialized into a FlowFileStream, in one of three formats.")
public class App implements Callable<Integer> {
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

	public static void main(String[] args) {
		int rc = new CommandLine(new App()).execute(args);
		System.exit(rc);
	}
}
