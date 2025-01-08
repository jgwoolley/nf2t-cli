package com.yelloowstone.nf2t.cli;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.nifi.util.FlowFilePackager;
import org.apache.nifi.util.FlowFileUnpackager;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class AppTest {
	private static final int[] versions = new int[] { 3, 2, 1 };
	
	@Test
	public void testPackageFiles() throws Exception {

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectReader reader = mapper.reader();

	
		for (int version : versions) {
			System.out.println("Package V" + version);

			try(final MockEnvironment environment = new MockEnvironment(version)) {
				int actualResults = 0;
				environment.createExampleContent("1.txt", "Hello World!");
				if(version != 1) {
					environment.createExampleContent("2.txt", "");
					environment.createExampleContent("3", new byte[] {});
				}
				
				final Path contentPath = environment.getContentPath();
				final Path packagedPath = environment.getPackagedPath();
				
				final String sw = environment.execute(x -> new String[] {"package", "--version", Integer.toString(version), "--in", contentPath.toString(), "--out", packagedPath.toString()});
				
				final FlowFileStreamResult result = reader.readValue(sw, FlowFileStreamResult.class);

				final FlowFileUnpackager unpackager = environment.getPackageVersions().get(version).getUnpackager();

				try (InputStream in = Files.newInputStream(result.getOutputPath())) {
					while (unpackager.hasMoreData()) {
						Map<String, String> attributes = unpackager.unpackageFlowFile(in,
								OutputStream.nullOutputStream());
						System.out.println("\t" + mapper.writer().writeValueAsString(attributes));
						
						if (attributes == null) {
							break;
						} else {
							actualResults += 1;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Assert.assertEquals(environment.getExpectedResults(), actualResults);
				System.out.println("\t" + mapper.writer().writeValueAsString(environment.getFilePaths()));
			}

		}
	}

	@Test
	public void testUnpackageFiles() throws Exception {

		final int[] versions = new int[] { 3, 2, 1 };

		final ObjectMapper mapper = new ObjectMapper();
		final ObjectReader reader = mapper.reader();

		for (int version : versions) {
			System.out.println("Unpackage V" + version);
			
			try(final MockEnvironment environment = new MockEnvironment(version)) {
				environment.createExampleContent("1.txt", "Hello World!");
				if(version != 1) {
					environment.createExampleContent("2.txt", "");
					environment.createExampleContent("3", new byte[] {});
					environment.createExampleZip();
					environment.createExampleTarGz();
				}
				
				final Path contentPath = environment.getContentPath();
				final Path packagedPath = environment.getPackagedPath();
				final Path unpackagedPath = environment.getUnpackagedPath();
								
				final FlowFilePackageVersion packageVersion = environment.getPackageVersions().get(version);
				
				final FlowFilePackager packager = packageVersion.getPackager();

				final Path flowFileStreamPath = packageVersion.getDefaultName(packagedPath);
				
				
				if(Files.exists(flowFileStreamPath)) {
					Assert.assertTrue("File already exists: " + flowFileStreamPath, false);
				}
				
				try (OutputStream out = Files.newOutputStream(flowFileStreamPath)) {								
					Files.list(contentPath).map(x -> Map.entry(null, null));
					
					List<Path> paths = Files.list(contentPath).collect(Collectors.toList());
					System.out.println("\t" + mapper.writer().writeValueAsString(paths));

					if(version == 1 && paths.size() != 1) {
						throw new Exception("Version 1 only supports 1 result: Got " + paths.size());
					}
					
					for(Path path: paths) {
						final Map<String, String> attributes = new HashMap<>();
						long size = App.updateDefaultAttributes(attributes, path);
						System.out.println("\t" + mapper.writer().writeValueAsString(attributes));
						try (InputStream in = Files.newInputStream(path)) {
							packager.packageFlowFile(in, out, attributes, size);
						} catch(Exception e) {
							throw new Exception("Failed to read " + path + " or write to " + flowFileStreamPath, e);
						}
					}
				}

				final String sw = environment.execute(x -> new String[] {"unpackage", "--version", Integer.toString(version),
						"--in", packagedPath.toString(), "--out", unpackagedPath.toString()});

				System.out.println("\t" + sw.toString());
				final FlowFileStreamResult result = reader.readValue(sw.toString(), FlowFileStreamResult.class);

				Assert.assertEquals(environment.getExpectedResults(), result.getOutputFiles().size());
				
				System.out.println("\t" + mapper.writer().writeValueAsString(environment.getFilePaths()));
			}			
		}
	}
}
