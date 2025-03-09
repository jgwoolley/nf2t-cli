package com.yelloowstone.nf2t.cli;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.nifi.util.FlowFileUnpackager;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class AppTest {

	private final ObjectMapper mapper;
	private final ObjectReader reader;

	public AppTest() {
		this.mapper = new ObjectMapper();
		this.reader = mapper.reader();
	}
	
	public void testPackageFiles(int version) throws Exception {
		
		System.out.println(ConsoleColors.GREEN + "Package V" + version + ConsoleColors.RESET);

		try(final MockEnvironment environment = new MockEnvironment(mapper, version)) {
			int actualResults = 0;
			environment.createExampleContent("1.txt", "Hello World!");
			if(version != 1) {
				environment.createExampleContent("2.txt", "");
				environment.createExampleContent("3", new byte[] {});
			}
			
			final Path contentPath = environment.getContentPath();
			final Path packagedPath = environment.getPackagedPath();
			
			final String sw = environment.execute(x -> new String[] {"package", "--version", Integer.toString(version), "--in", contentPath.toString(), "--out", packagedPath.toString(), "--attribute", "key=${foo}$${foo}$$${foo}", "--attribute", "key2=${size}"});
			
			final FlowFileStreamResult result = reader.readValue(sw, FlowFileStreamResult.class);

			final FlowFileUnpackager unpackager = environment.getPackageVersions().get(version).getUnpackager();

			try (InputStream in = Files.newInputStream(result.getOutputPath())) {
				while (unpackager.hasMoreData()) {
					Map<String, String> attributes = unpackager.unpackageFlowFile(in,
							OutputStream.nullOutputStream());
					System.out.println("\t" + "Attributes: " + mapper.writer().writeValueAsString(attributes));
					
					if (attributes == null) {
						break;
					} else {
						actualResults += 1;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			Assert.assertEquals("Actual results did not match actual results.", environment.getPackageInputs().size(), actualResults);
			System.out.println("\t" + ConsoleColors.YELLOW + "Output Paths: " + mapper.writer().writeValueAsString(environment.getFilePaths()) + ConsoleColors.RESET);
		}
	}
	
	@Test
	public void testPackageFilesVersion3() throws Exception {
		testPackageFiles(3);
	}

	@Test
	public void testPackageFilesVersion2() throws Exception {
		testPackageFiles(2);
	}
	
	@Test
	public void testPackageFilesVersion1() throws Exception {
		testPackageFiles(1);
	}
	
	
	public void testUnpackageFiles(final int version) throws Exception {	
		System.out.println(ConsoleColors.GREEN + "Unpackage V" + version + ConsoleColors.RESET);
		
		try(final MockEnvironment environment = new MockEnvironment(mapper, version)) {
			environment.createExampleContent("1.txt", "Hello World!");
			if(version != 1) {
				environment.createExampleContent("2.txt", "");
				environment.createExampleContent("3", new byte[] {});
				environment.createExampleUnpackageZip();
				environment.createExampleUnpackageTarGz();
			}
			
			environment.createExampleUnpackageFromContent();
			
			final Path packagedPath = environment.getPackagedPath();
			final Path unpackagedPath = environment.getUnpackagedPath();
							
			final String sw = environment.execute(x -> new String[] {"unpackage", "--version", Integer.toString(version),
					"--in", packagedPath.toString(), "--out", unpackagedPath.toString()});

			System.out.println("\t" + ConsoleColors.RED + "Result:\n" + sw.toString() + ConsoleColors.RESET);
			final FlowFileStreamResult result = reader.readValue(sw.toString(), FlowFileStreamResult.class);
			final List<Path> unpackagedPaths = Files.list(unpackagedPath).collect(Collectors.toList());
			
			System.out.println("\t" + "Unpackaged Paths: " + mapper.writer().writeValueAsString(unpackagedPaths));

			Assert.assertEquals("Results object output files size should be the same as the files inputted.", environment.getFlowFileSize(), result.getOutputFiles().size());			
			Assert.assertEquals("Unpackaged files directory size should be the same as the files inputted.", environment.getFlowFileSize(), unpackagedPaths.size());			
		}		
	}
	
	@Test
	public void testUnpackageFilesVersion3() throws Exception {
		testUnpackageFiles(3);
	}
	
	@Test
	public void testUnpackageFilesVersion2() throws Exception {
		testUnpackageFiles(2);
	}
	
	@Test
	public void testUnpackageFilesVersion1() throws Exception {
		testUnpackageFiles(1);
	}
}
