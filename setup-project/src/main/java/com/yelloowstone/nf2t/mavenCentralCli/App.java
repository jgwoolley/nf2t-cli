package com.yelloowstone.nf2t.mavenCentralCli;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "Build Maven Central Zip", subcommands = { SubCommandGenerateDocs.class, SubCommandGenerateMavenCentralZip.class })
public class App implements Callable<Integer> {

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
}
