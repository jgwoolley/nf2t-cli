package com.yelloowstone.nf2t.mavenCentralCli;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import com.yelloowstone.nf2t.maven.MavenCoordinate;
import com.yelloowstone.nf2t.maven.MavenParser;
import com.yelloowstone.nf2t.maven.MavenProject;

public abstract class AbstractSubCommand implements Callable<Integer> {

	@Option(names = { "-w",
			"--workdir" }, description = "The Working directory, which helps to determine the default paths of other arguments.", defaultValue = ".")
	private Path workdir;

	@Option(names = { "--gpgUser", "-u" }, required = false, description = {
			"The local GPG user that will be fed into GPG command." })
	private String gpgUser;
	
	@Option(names= {"--mavenCoordinateFilters", "-m"}, description = "The given Maven coordinates of the project to process.", required=false)
	private String[] mavenCoordinateFilters = new String[0];
	
	@Option(names= {"--isPicocli", "-p"}, description = "Is Picocli Project.", defaultValue="false")
	private boolean isPicocli;
	
	@Parameters(description = "The directories of the Maven projects to process, relative to the working directory.", defaultValue = ".")
	private String[] projectDirNames;

	public Path getWorkdir() {
		return workdir;
	}
	
	public String getGpgUser() {
		return gpgUser;
	}

	public String[] getMavenCoordinateFilters() {
		return mavenCoordinateFilters;
	}
	
	public String[] getProjectDirNames() {
		return projectDirNames;
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
		MavenCoordinate[] mavenCoordinateFiltersParsed;
		if(mavenCoordinateFilters == null) {
			mavenCoordinateFiltersParsed = new MavenCoordinate[mavenCoordinateFilters.length];
			for(int index = 0; index < mavenCoordinateFilters.length; index++) {
				final String mavenCoordinate = mavenCoordinateFilters[index];
				
				mavenCoordinateFiltersParsed[index] = MavenCoordinate.parseCoordinate(mavenCoordinate);
			}			
		} else {
			mavenCoordinateFiltersParsed = new MavenCoordinate[0];
		}
		
		
		final MavenParser parser = new MavenParser();
		
		//TODO: Determine Maven coordinates
		final List<MavenProject> mavenProjects = parser.parseMavenProjects(isPicocli, mavenCoordinateFiltersParsed, getInputPaths());
		if(mavenProjects == null) {
			return 1;
		}
		
		System.out.println(ConsoleColors.CYAN + "Projects: " + mavenProjects + ConsoleColors.RESET);

		return processProjects(mavenProjects);
	}
}
