package com.yelloowstone.nf2t.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppTest {
	@Test
    public void testPackageFile() throws IOException
    {
    	final String content = "Hello World!";
    	final Map<String, String> attributes = new HashMap<>();
    	attributes.put(CoreAttributes.FILENAME.key(), "test");
    	
    	final String attributesText = new ObjectMapper().writeValueAsString(attributes);
    	
    	final Path tmpPath = Files.createTempDirectory("testPackageFile"); 

		for(int version = 1; version <= 3; version+=1) {
			final Path versionPath = tmpPath.resolve(Integer.toString(version));
			Files.createDirectory(versionPath);
			
	    	for(boolean hasAttributes: new boolean[] {true, false }) {
	    		final Path attributesModePath = versionPath.resolve(Boolean.toString(hasAttributes));	    		
	    		final Path contentPath = attributesModePath.resolve("content");
	        	final Path packagedPath = attributesModePath.resolve("packaged");
	        	final Path unpackagedPath = attributesModePath.resolve("unpackaged");	
	        	
	    		Files.createDirectories(attributesModePath);
	    		Files.createDirectories(contentPath);
	    		Files.createDirectories(packagedPath);
	    		Files.createDirectories(unpackagedPath);  	
	        	Files.writeString(contentPath.resolve("content.bin"), content);
	        	
	        	if(hasAttributes) {
		        	Files.writeString(contentPath.resolve("content.bin" + App.ATTRIBUTES_EXTENSION), attributesText);
	        	}
	        	
	    		final App app = new App(System.out, System.err);

	    		final CommandLineParser parser = new DefaultParser();
	        	
	    		app.parse(parser, new String[] {"--action", "package", "--version", Integer.toString(version), "--in", contentPath.toString(), "--out", packagedPath.toString()});
	    		app.parse(parser, new String[] {"--action", "unpackage", "--version", Integer.toString(version), "--in", packagedPath.toString(), "--out", unpackagedPath.toString()});    		
	    		
	    		Files.list(unpackagedPath).filter(x -> true).forEach(x -> {
	    			if(!x.endsWith(App.ATTRIBUTES_EXTENSION)) {
	    				return;
	    			}
	    			
	    			String actual = null;
	        		try {
						actual = Files.readString(x);
					} catch (IOException e) {
						e.printStackTrace();
					}

	        		Assert.assertEquals(content, actual);
	    		});
	    		
	    		
	    		for(final Path path: new Path[] {contentPath, packagedPath, unpackagedPath}) {
	        		Files.list(path).forEach(x -> {
	        			try {
							Files.delete(x);
						} catch (IOException e) {
							e.printStackTrace();
						}
	        		});
					Files.delete(path);
	    		}      		
	    		Files.delete(attributesModePath);
	    	}
    		Files.delete(versionPath);
	    }
		Files.delete(tmpPath);
	}
}
