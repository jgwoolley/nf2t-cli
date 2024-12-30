package com.yelloowstone.nf2t.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.nifi.util.FlowFilePackager;
import org.apache.nifi.util.FlowFileUnpackager;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class AppTest {
	private static final int[] versions = new int[] { 3, 2, 1 };
	private static final String[] examples = new String[] { "1.txt", "2.txt", "3.txt" };
	
	public static void delete(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					throw exc;
				}
			}
		});
	}

	@Test
	public void testPackageFiles() throws Exception {
		int successes = 0;
		final Path tmpPath = Files.createTempDirectory("testPackageFile");

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectReader reader = mapper.reader();

		try {
			for (int version : versions) {
				System.out.println("Package V" + version);

				final Path versionPath = tmpPath.resolve(Integer.toString(version));

				Files.createDirectory(versionPath);

				final Path contentPath = versionPath.resolve("content");
				final Path packagedPath = versionPath.resolve("packaged");
				final Path unpackagedPath = versionPath.resolve("unpackaged");

				Files.createDirectories(versionPath);
				Files.createDirectories(contentPath);
				Files.createDirectories(packagedPath);
				Files.createDirectories(unpackagedPath);

				for (String example : examples) {
					Files.writeString(contentPath.resolve(example), "Hello World!");
				}

				final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

				final App app = new App(new PrintStream(stdout), System.err);
				final CommandLineParser parser = new DefaultParser();

				app.parse(parser, new String[] { "--action", "package", "--version", Integer.toString(version), "--in",
						contentPath.toString(), "--out", packagedPath.toString() });

				final FlowFileStreamResult result = reader.readValue(stdout.toString(), FlowFileStreamResult.class);

				final FlowFileUnpackager unpackager = app.getUnpackagers().get(version).get();

				try (InputStream in = Files.newInputStream(result.getOutputPath())) {
					while (true) {
						Map<String, String> attributes = unpackager.unpackageFlowFile(in,
								OutputStream.nullOutputStream());
						System.out.println("\t" + attributes);
						if (attributes == null) {
							break;
						} else {
							successes += 1;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				app.parse(parser, new String[] { "--action", "unpackage", "--version", Integer.toString(version),
						"--in", packagedPath.toString(), "--out", unpackagedPath.toString() });

			}
		} finally {
			delete(tmpPath);
		}

		int expected = 0;
		for (int version : versions) {
			expected += version == 1 ? 1 : examples.length;
		}

		Assert.assertEquals(expected, successes);
	}

	@Test
	public void testUnpackageFiles() throws Exception {
		int successes = 0;
		final int[] versions = new int[] { 3, 2, 1 };
		final String[] examples = new String[] { "1.txt"};

		final Path tmpPath = Files.createTempDirectory("testUnpackageFile");

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectReader reader = mapper.reader();

		try {
			for (int version : versions) {
				System.out.println("Unpackage V" + version);

				final Path versionPath = tmpPath.resolve(Integer.toString(version));

				Files.createDirectory(versionPath);

				final Path contentPath = versionPath.resolve("content");
				final Path packagedPath = versionPath.resolve("packaged");
				final Path unpackagedPath = versionPath.resolve("unpackaged");

				Files.createDirectories(versionPath);
				Files.createDirectories(contentPath);
				Files.createDirectories(packagedPath);
				Files.createDirectories(unpackagedPath);

				final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

				final App app = new App(new PrintStream(stdout), System.err);

				final FlowFilePackager packager = app.getPackagers().get(version).get();

				for (String example : examples) {
					Path examplePath = contentPath.resolve(example);
					byte[] content = "Hello World!".getBytes(StandardCharsets.UTF_8);
					Files.write(examplePath, content);
				}

				final Path flowFileStreamPath = packagedPath.resolve(FlowFileStreamResult.FLOWFILE_DEFAULT_FILENAME);
				
				try (OutputStream out = Files.newOutputStream(flowFileStreamPath)) {					
					for(Path path: Files.list(contentPath).collect(Collectors.toList())) {
						final Map<String, String> attributes = new HashMap<>();
						long size = App.updateDefaultAttributes(attributes, path);
						System.out.println("\t" + attributes);
						try (InputStream in = Files.newInputStream(path)) {
							packager.packageFlowFile(in, out, attributes, size);
						} catch(Exception e) {
							throw new Exception("Failed to read " + path + " or write to " + flowFileStreamPath, e);
						}
					}
				}

				final CommandLineParser parser = new DefaultParser();

				app.parse(parser, new String[] { "--action", "unpackage", "--version", Integer.toString(version),
						"--in", packagedPath.toString(), "--out", unpackagedPath.toString() });

				System.out.println("\t" + stdout.toString());
				final FlowFileStreamResult result = reader.readValue(stdout.toString(), FlowFileStreamResult.class);

				successes += result.getOutputFiles().size();
			}
			
			int expected = 0;
			for (int version : versions) {
				expected += version == 1 ? 1 : examples.length;
			}

			Assert.assertEquals(expected, successes);
		} finally {
			delete(tmpPath);
		}


	}
}
