package com.yelloowstone.nf2t.maven;

import java.io.BufferedInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.yelloowstone.nf2t.git.GitUtils;
import com.yelloowstone.nf2t.jars.ModifiableJarDetails;
import com.yelloowstone.nf2t.jars.ModifiableMavenDescriptorDetails;
import com.yelloowstone.nf2t.jars.NarDetails;
import com.yelloowstone.nf2t.jars.UnmodifiableJarDetails;
import com.yelloowstone.nf2t.utils.ProcessUtils;

public class MavenParser {

	private final Map<String, Consumer<Context>> parseFileLut;
	private final List<Function<Context, Boolean>> parseFileList;
	private final Map<String, String> packagingToExtensionLut;
	
	public MavenParser() {
		final Map<String, Consumer<Context>> parseFileLut = new HashMap<>();
		final List<Function<Context, Boolean>> parseFileList = new ArrayList<>();
		final Map<String, String> packagingToExtensionLut = new HashMap<>();
		
		parseFileLut.put("META-INF/docs/extension-manifest.xml", (x) -> {
			final NarDetails narDetails = NarDetails.parse(x.getZipInputStream(), x.getZipEntry());
			x.getBuilder().setNarDetails(narDetails);
		});

		parseFileLut.put("META-INF/MANIFEST.MF", (x) -> {
			try {
				final Manifest manifest = new Manifest(x.getZipInputStream());
				x.getBuilder().setManifest(manifest);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		final Map<String, BiConsumer<Context, String[]>> mavenFileParser = new HashMap<>();
		mavenFileParser.put("pom.xml", (x, elements) -> {
			final String groupId = elements[2];
			final String artifactId = elements[3];

			x.getBuilder().addMavenPomDetails(groupId, artifactId);
		});

		mavenFileParser.put("pom.properties", (x, elements) -> {
			final String groupId = elements[2];
			final String artifactId = elements[3];

			final ModifiableMavenDescriptorDetails details = x.getBuilder().addMavenPomDetails(groupId, artifactId);

			@SuppressWarnings("resource")
			final Scanner sc = new Scanner(x.getZipInputStream());

			while (sc.hasNext()) {
				final String line = sc.next();
				if (line.startsWith("#")) {
					continue;
				}
				final String[] keyValue = line.split("=");
				if (keyValue.length != 2) {
					continue;
				}
				details.addProperties(keyValue[0], keyValue[1]);
			}
		});

		parseFileList.add((x) -> {
			final String name = x.getZipEntry().getName();
			final String[] elements = name.split("/");
			if (5 != elements.length) {
				return false;
			}
			if (!"META-INF".equals(elements[0])) {
				return false;
			}

			if (!"maven".equals(elements[1])) {
				return false;
			}

			final String filename = elements[4];

			final BiConsumer<Context, String[]> consumer = mavenFileParser.get(filename);

			if (consumer != null) {
				consumer.accept(x, elements);
				return true;
			}

			return false;
		});

		packagingToExtensionLut.put("jar", "jar");
		packagingToExtensionLut.put("war", "war");
		packagingToExtensionLut.put("nar", "nar");
		packagingToExtensionLut.put("pom", "pom");
		
		
		this.parseFileLut = Collections.unmodifiableMap(parseFileLut);
		this.parseFileList = Collections.unmodifiableList(parseFileList);
		this.packagingToExtensionLut = Collections.unmodifiableMap(packagingToExtensionLut);
	}

	// TODO: Decide whether or not you want to do this...

//	public static JarDetails.Builder parseMavenProject(final Path path) throws IOException {
//		if (Files.isDirectory(path)) {
//			final Path sourcePomPath = path.resolve("pom.xml");
//			final Path targetPath = path.resolve("target");
//
//			final boolean sourcePomIsFile = Files.isRegularFile(sourcePomPath);
//			final boolean targetIsDirectory = Files.isDirectory(targetPath);
//
//			if (sourcePomIsFile && targetIsDirectory) {
//				// If the source pom.xml, and target directory exist, assume we are in the
//				// project folder
//
//			} else {
//				// If the above is not the case, assume we are in a folder with Maven artifacts.
//				final List<MavenCoordinate> effectivePoms = new ArrayList<>();
//
//				Files.list(path).forEach(artifactPath -> {
//					final String artifactFileName = artifactPath.getFileName().toString();
//
//					final MavenCoordinate artifact = parsePomFromFileName(artifactFileName);
//					if (artifact != null) {
//						effectivePoms.add(artifact);
//					}
//				});
//
//				Files.list(path).forEach(artifactPath -> {
//					final String artifactFileName = artifactPath.getFileName().toString();
//
//					for (MavenCoordinate effectivePom : effectivePoms) {
//						final String artifactId = effectivePom.getArtifactId();
//						if (artifactFileName.length() <= artifactId.length()
//								|| !artifactFileName.startsWith(artifactId)) {
//							return;
//						}
//
//						final String fileNameWithoutArtifactId = artifactFileName.substring(artifactId.length() + 1);
//
//						final String version = effectivePom.getVersion();
//						if (fileNameWithoutArtifactId.length() <= version.length()
//								|| !fileNameWithoutArtifactId.startsWith(version)) {
//							return;
//						}
//
//						final String fileNameWithoutVersion = fileNameWithoutArtifactId.substring(version.length());
//
//						final int lastPeriod = fileNameWithoutVersion.lastIndexOf('.');
//						if (lastPeriod == -1 || fileNameWithoutVersion.length() <= 1) {
//							return;
//						}
//
//						String extension = null;
//						String classifier = null;
//
//						if (lastPeriod == 0) {
//							extension = fileNameWithoutVersion.substring(1);
//						} else {
//							classifier = fileNameWithoutVersion.substring(1, lastPeriod);
//							extension = fileNameWithoutVersion.substring(lastPeriod + 1);
//						}
//
//						String packaging = extension;
//
//						final MavenCoordinate artifact = new MavenCoordinate(null, artifactId, version, packaging, null,
//								null, null);
//						System.out.println(artifact);
//					}
//				});
//			}
//
//		} else if (Files.isRegularFile(path)) {
//			// If it is a regular file treat it as a single artifact.
//
//		}
//
//		return null;
//	}

	public ModifiableJarDetails parseJar(final InputStream fileInputStream) throws IOException {
		try (final BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				final ZipInputStream zin = new ZipInputStream(bufferedInputStream)) {

			final ModifiableJarDetails builder = new ModifiableJarDetails();
			ZipEntry ze;
			while ((ze = zin.getNextEntry()) != null) {
				final String name = ze.getName();

				final Consumer<Context> parseFile = parseFileLut.get(name);
				final Context context = new Context(builder, zin, ze);
				if (parseFile != null) {
					parseFile.accept(context);
					continue;
				}

				for (Function<Context, Boolean> parseFileElement : this.parseFileList) {
					final boolean matched = parseFileElement.apply(context);
					if (matched == true) {
						break;
					}
				}
			}

			return builder;
		}
	}

	private static class Context {
		private final ModifiableJarDetails builder;
		private final ZipInputStream zin;
		private final ZipEntry ze;

		private Context(final ModifiableJarDetails builder, final ZipInputStream zin, final ZipEntry ze) {
			super();
			this.builder = builder;
			this.zin = zin;
			this.ze = ze;
		}

		public ModifiableJarDetails getBuilder() {
			return builder;
		}

		public ZipInputStream getZipInputStream() {
			return zin;
		}

		public ZipEntry getZipEntry() {
			return ze;
		}

	}

	private static final String SNAPSHOT_POM_ENDSWITH = "-SNAPSHOT.pom";
	private static final String POM_ENDSWITH = ".pom";

	public String getExtensionFromMavenPackaging(String packaging) {
		return this.packagingToExtensionLut.get(packaging);
	}
	
	public MavenProject parseMavenProject(final boolean checkPicocli, final Instant buildTime,
			final MavenCoordinate[] mavenCoordinateFilters, final Path projectPath) {

		final Path pomPath = projectPath.resolve("pom.xml");
		final Path targetPath = projectPath.resolve("target");

		Path artifactsPath;
		Model mavenModel;

		if (Files.isRegularFile(pomPath) && Files.isDirectory(targetPath)) {
			artifactsPath = targetPath;
			mavenModel = MavenParser.generateEffectivePom(projectPath);
		} else {
			artifactsPath = projectPath;
			mavenModel = MavenParser.findEffectivePom(projectPath);
		}

		if (mavenModel == null) {
			return null;
		}

		final String gitHash = GitUtils.createGitHash(projectPath);
		final MavenCoordinate baseCoordinate = MavenCoordinate.readModel(mavenModel);
		final Path baseArtifactPath = baseCoordinate.resolveFilePath(artifactsPath);

		if (!Files.isRegularFile(baseArtifactPath)) {
			new Exception("Path must be a file: " + baseArtifactPath).printStackTrace();
			return null;
		}

		try (final InputStream is = Files.newInputStream(baseArtifactPath)) {
			final UnmodifiableJarDetails jarDetails = parseJar(is).build();
			final MavenJarArtifact baseJarArtifact = new MavenJarArtifact(baseArtifactPath, baseCoordinate, jarDetails);
			final String mainClass = jarDetails.getMainClass();	
			final boolean isPicocli = checkPicocli ? mainClass != null : false;
			
			final MavenProject mavenProject = new MavenProject(isPicocli, buildTime, projectPath, artifactsPath,
					mavenModel, baseCoordinate, baseJarArtifact, gitHash);

			final Path artifactParent = mavenProject.getArtifactPath();
			if (!Files.isDirectory(artifactParent)) {
				new Exception("Path must be a directory: " + artifactParent).printStackTrace();
				return null;
			}

			return mavenProject;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}

	public List<MavenProject> parseMavenProjects(final boolean isPicocli,
			final MavenCoordinate[] mavenCoordinateFilters, final Path[] paths) {
		final List<MavenProject> projects = new ArrayList<>();

		final Instant buildTime = Instant.now();

		for (final Path projectPath : paths) {
			final MavenProject project = parseMavenProject(isPicocli, buildTime, mavenCoordinateFilters, projectPath);
			if (project == null) {
				System.err.println("Could not process project: " + projectPath);
				return null;
			}
			projects.add(project);
		}

		return projects;
	}

	public static String getFileName(Model model, String postfix) {
		return model.getArtifactId() + "-" + model.getVersion() + postfix;
	}

	public static MavenCoordinate parsePomFromFileName(final String fileName) {
		if (!fileName.endsWith(POM_ENDSWITH)) {
			return null;
		}

		final boolean endsWithSnapshot = fileName.endsWith(SNAPSHOT_POM_ENDSWITH);

		final String fileNameWithoutPostfix = endsWithSnapshot
				? fileName.substring(0, fileName.length() - SNAPSHOT_POM_ENDSWITH.length())
				: fileName.substring(0, fileName.length() - POM_ENDSWITH.length());
		final int lastDash = fileNameWithoutPostfix.lastIndexOf('-');
		if (lastDash == -1) {
			return null;
		}

		final String artifactId = fileNameWithoutPostfix.substring(0, lastDash);
		final String versionRaw = fileNameWithoutPostfix.substring(lastDash + 1);
		final String version = endsWithSnapshot ? versionRaw + "-SNAPSHOT" : versionRaw;

		return new MavenCoordinate(null, artifactId, version, null, null);
	}

	public static Model parseEffectivePom(final Path artifactPath) {
		try (FileReader reader = new FileReader(artifactPath.toFile())) {
			MavenXpp3Reader mavenReader = new MavenXpp3Reader();
			return mavenReader.read(reader);
		} catch (IOException | XmlPullParserException e) {
			System.err.println("Could not parse pom.xml: " + artifactPath);
			e.printStackTrace();
			return null;
		}
	}

	public static Model generateEffectivePom(final Path projectPath) {
		final Path targetPath = projectPath.resolve("target");
		if (!Files.isDirectory(targetPath)) {
			System.err.println(
					"Target directory does not exist. Use \"mvn\" command to build artifacts if you haven't already. ["
							+ targetPath.toAbsolutePath() + "]");
			return null;
		}

		final Path artifactPath = targetPath.resolve("effective.pom");

		try {
			final int exitCode = ProcessUtils.exec(projectPath.toFile(), "mvn", "help:effective-pom",
					"-Doutput=" + artifactPath.toAbsolutePath().toString(), "--quiet");
			if (exitCode != 0) {
				System.err.println(
						"Could not create effective pom. Project path is [" + projectPath.toAbsolutePath() + "].");
				return null;
			}
		} catch (Exception e) {
			System.err.println("Could not execute mvn help:effective-pom command. Project path is ["
					+ projectPath.toAbsolutePath() + "].");
			e.printStackTrace();
			return null;
		}

		if (!Files.isRegularFile(artifactPath)) {
			return null;
		}

		Model model = parseEffectivePom(artifactPath);

		final Path newArtifactPath = targetPath.resolve(MavenParser.getFileName(model, ".pom"));

		if (model != null) {
			try {
				Files.deleteIfExists(newArtifactPath);
				Files.move(artifactPath, newArtifactPath);
			} catch (Exception e) {
				System.err.println("Could not move pom file: " + newArtifactPath);
				e.printStackTrace();
				return null;
			}
		}

		return model;
	}

	public static Model findEffectivePom(final Path projectPath) {
		final List<Path> pomPaths;
		try {
			pomPaths = Files.list(projectPath).filter(x -> x.toString().endsWith(".pom")).collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		if (pomPaths.size() == 0) {
			new Exception("Only one .pom file must exist in the directory: " + projectPath
					+ ". The following .pom file(s) were found: " + pomPaths + ".").printStackTrace();
			return null;
		}

		final Path pomPath = pomPaths.get(0);

		return parseEffectivePom(pomPath);
	}

	public static void main(String[] args) {
		final Path projectPath = Path.of("..", "nf2t-cli");
		final MavenParser parser = new MavenParser();

		List<MavenProject> mavenProjects = parser.parseMavenProjects(true, new MavenCoordinate[0],
				new Path[] { projectPath });
		mavenProjects.forEach(System.out::println);
	}
}
