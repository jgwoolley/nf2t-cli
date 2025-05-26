package com.yelloowstone.nf2t.cli.flowfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.tika.Tika;

public class FlowFileUtils {
	public static final String FILE_SIZE_ATTRIBUTE = "size";
		
	public static Map<String, String> generateDefaultAttributes(final Tika tika, final Path path, final long contentSize)
			throws IOException {
		final Map<String, String> attributes = new HashMap<>();
		
		try {
			final String mimeType = tika.detect(path);
			if (mimeType != null) {
				attributes.put(CoreAttributes.MIME_TYPE.key(), mimeType);
			}

		} catch (Exception e) {

		}

		attributes.put(CoreAttributes.FILENAME.key(), path.getFileName().toString());
		if (path.getParent() != null) {
			attributes.put(CoreAttributes.PATH.key(), path.getParent().toString());
		}
		attributes.put(CoreAttributes.ABSOLUTE_PATH.key(), path.toString());
		attributes.put(FILE_SIZE_ATTRIBUTE, Long.toString(contentSize));

		try {
			final BasicFileAttributes fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
			attributes.put("lastAccessTime", fileAttributes.lastAccessTime().toString());
			attributes.put("creationTime", fileAttributes.creationTime().toString());
			attributes.put("lastModifiedTime", fileAttributes.lastModifiedTime().toString());
		} catch (Exception e) {

		}

		return attributes;
	}
}
