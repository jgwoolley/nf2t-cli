package com.yelloowstone.nf2t.cli.jars;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.yelloowstone.nf2t.cli.jars.JarDetails.Builder;

public class JarParser {
	private final Map<String, Consumer<Context>> parseFileLut;
	private final List<Function<Context, Boolean>> parseFileList;

	public JarParser() {
		final Map<String, Consumer<Context>> parseFileLut = new HashMap<>();
		final List<Function<Context, Boolean>> parseFileList = new ArrayList<>();

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

			final MavenPomDetails details = x.getBuilder().addMavenPomDetails(groupId, artifactId);

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

		this.parseFileLut = Collections.unmodifiableMap(parseFileLut);
		this.parseFileList = Collections.unmodifiableList(parseFileList);
	}

	public JarDetails.Builder parse(final InputStream fileInputStream) throws IOException {
		try (final BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				final ZipInputStream zin = new ZipInputStream(bufferedInputStream)) {

			final JarDetails.Builder builder = new JarDetails.Builder();
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
		private final JarDetails.Builder builder;
		private final ZipInputStream zin;
		private final ZipEntry ze;

		private Context(final Builder builder, final ZipInputStream zin, final ZipEntry ze) {
			super();
			this.builder = builder;
			this.zin = zin;
			this.ze = ze;
		}

		public JarDetails.Builder getBuilder() {
			return builder;
		}

		public ZipInputStream getZipInputStream() {
			return zin;
		}

		public ZipEntry getZipEntry() {
			return ze;
		}

	}
}
