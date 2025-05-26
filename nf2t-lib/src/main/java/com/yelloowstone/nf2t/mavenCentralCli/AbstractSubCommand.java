package com.yelloowstone.nf2t.mavenCentralCli;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import com.yelloowstone.nf2t.maven.MavenProject;
import com.yelloowstone.nf2t.maven.MavenUtils;

public abstract class AbstractSubCommand implements Callable<Integer> {

	@Option(names = { "-w",
			"--workdir" }, description = "The Working directory, which helps to determine the default paths of other arguments.", defaultValue = ".")
	private Path workdir;

	@Option(names = { "--gpgUser", "-u" }, required = false, description = {
			"The local GPG user that will be fed into GPG command." })
	private String gpgUser;

	@Option(names = { "--resolveStragety", "-r" }, defaultValue = "GENERATE", description = {
	"When run in resolve pom mode, will generate effective pom from pom.xml, and will look for artifacts in targets folder. When disabled will only look for artifacts in root folder." })
	private MavenProjectArtifactResolveStragety resolvePom;
	
	@Parameters(description = "The directories of the Maven projects to process, relative to the working directory.", defaultValue = ".")
	private String[] projectDirNames;

	public Path getWorkdir() {
		return workdir;
	}
	
	public String getGpgUser() {
		return gpgUser;
	}

	public String[] getProjectDirNames() {
		return projectDirNames;
	}
	
	public boolean isResolvePom() {
		return MavenProjectArtifactResolveStragety.GENERATE == resolvePom;
	}

	public Path[] getInputPaths() {
		final Path[] results = new Path[this.projectDirNames.length];
		for (int index = 0; index < this.projectDirNames.length; index++) {
			results[index] = workdir.resolve(this.projectDirNames[index]);
		}

		return results;
	}

	public abstract Integer processProjects(final List<MavenProject> mavenProjects);

	@Override
	public Integer call() throws Exception {
		final List<MavenProject> mavenProjects = MavenUtils.parseMavenProjects(isResolvePom(), getInputPaths());
		if(mavenProjects == null) {
			return 1;
		}
		
		System.out.println(ConsoleColors.CYAN + "Projects: " + mavenProjects + ConsoleColors.RESET);

		return processProjects(mavenProjects);
	}
}
