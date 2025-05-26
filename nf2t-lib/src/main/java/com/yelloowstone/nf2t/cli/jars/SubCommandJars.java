package com.yelloowstone.nf2t.cli.jars;

import java.util.concurrent.Callable;

import com.yelloowstone.nf2t.cli.App;
import com.yelloowstone.nf2t.mavenCentralCli.SubCommandGenerateDocs;

import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * @author 26191568+jgwoolley@users.noreply.github.com
 */
@Command(name = "jars", versionProvider = App.class, subcommands = { SubCommandValidateJar.class, SubCommandGenerateDocs.class,
		}, mixinStandardHelpOptions = true, description = "A Java CLI for parsing Apache NiFi FlowFileStreams. One or more FlowFiles can be serialized into a FlowFileStream, in one of three formats.")
public class SubCommandJars implements Callable<Integer>, IVersionProvider {

	@Spec
	private CommandSpec spec;
	
	@Override
	public String[] getVersion() throws Exception {
		// This method cannot be called, it it a sub-method.
		return new String[] {};
	}

	@Override
	public Integer call() throws Exception {
		throw new picocli.CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand.");
	}

	public CommandSpec getSpec() {
		return spec;
	}
}
