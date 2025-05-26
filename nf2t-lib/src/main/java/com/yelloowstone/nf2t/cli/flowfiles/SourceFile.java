package com.yelloowstone.nf2t.cli.flowfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author 26191568+jgwoolley@users.noreply.github.com
 */
public class SourceFile {
	@JsonProperty("parent")
	private final SourceFile parent;
	
	@JsonProperty("absolutePath")
	private final String absolutePath;
	
	@JsonProperty("filename")
	private final String filename;
	
	@JsonProperty("size")
	private final long size;
	
	@JsonProperty("uuid")
	private final UUID uuid;
	
	@JsonCreator
	public SourceFile(@JsonProperty("parent") SourceFile parent, @JsonProperty("absolutePath") String absolutePath, @JsonProperty("filename") String filename, @JsonProperty("size") long size, @JsonProperty("uuid") UUID uuid) {
		this.parent = parent;
		this.absolutePath = absolutePath;
		this.filename = filename;
		this.size = size;
		this.uuid = uuid;
	}

	public SourceFile(SourceFile parent, String absolutePath, String filename, long size) {
		this(parent, absolutePath, filename, size, UUID.randomUUID());
	}

	/**
	 * @see org.apache.nifi.flowfile.attributes.CoreAttributes#ABSOLUTE_PATH
	 * 
	 * @return
	 */
	public String getAbsolutePath() {
		return absolutePath;
	}

	/**
	 * @see org.apache.nifi.flowfile.attributes.CoreAttributes#FILENAME
	 * 
	 * @return
	 */
	public String getFilename() {
		return filename;
	}
	
	public long getSize() {
		return size;
	}

	/**
	 * @see org.apache.nifi.flowfile.attributes.CoreAttributes#UUID
	 * 
	 * @return
	 */
	public UUID getUuid() {
		return uuid;
	}
	
	public static SourceFile fromPath(SourceFile parent, Path inputPath) {
		final String absolutePath = inputPath.toAbsolutePath().toString();
		final String filename = inputPath.getFileName().toString();
		long size = -1;
		
		try {
			size = Files.size(inputPath);
		} catch (IOException e) {
			
		}
		
		return new SourceFile(parent, absolutePath, filename, size);
	}
	
}
