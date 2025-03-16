package com.yelloowstone.nf2t.cli;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Parameters;

@Command(name = "validateNar", description = "This command parses the extension-manifest.xml from a NiFi Archive File (NAR), which should be present in a valid NAR.")
public class SubCommandValidateNar implements Callable<Integer>, IVersionProvider {

	@Parameters
	private Path[] narPaths;

	@Override
	public String[] getVersion() throws Exception {
		return new String[] {};
	}

	@Override
	public Integer call() throws Exception {

		for (final Path narPath : narPaths) {
			if (!Files.isRegularFile(narPath)) {
				System.err.println(narPath.toAbsolutePath());
				return 1;
			}

			try (InputStream fileInputStream = Files.newInputStream(narPath);
					BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
					ZipInputStream zin = new ZipInputStream(bufferedInputStream)) {

				ZipEntry ze;
				while ((ze = zin.getNextEntry()) != null) {
					String name = ze.getName();

					if (!"META-INF/docs/extension-manifest.xml".equals(name)) {
						continue;
					}

					try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
						byte[] buffer = new byte[1024];
						int len;
						while ((len = zin.read(buffer)) > 0) {
							outputStream.write(buffer, 0, len);
						}

						ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
								outputStream.toByteArray());

						DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
						DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
						Document document = dBuilder.parse(byteArrayInputStream);

						NodeList nodeList = document.getElementsByTagName("systemApiVersion");
						Element element = (Element) nodeList.item(0);

						System.out.println("NiFi Version: " + element.getTextContent());

						NodeList extensionsNodeList = document.getElementsByTagName("extensions");
						Element extensionsNode = (Element) extensionsNodeList.item(0);

						NodeList extensionNodeList = extensionsNode.getElementsByTagName("extension");
						for (int index = 0; index < extensionNodeList.getLength(); index++) {
							Element extensionElement = (Element) extensionNodeList.item(index);
							NodeList nameNodeList = extensionElement.getElementsByTagName("name");
							Element extensionNameElement = (Element) nameNodeList.item(0);
							
							NodeList extensionTypeNodeList = extensionElement.getElementsByTagName("type");
							Element extensionTypeElement = (Element) extensionTypeNodeList.item(0);			
							
							System.out.println(extensionTypeElement.getTextContent() + " " + extensionNameElement.getTextContent());
						}

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return 1;
			}

		}

		return 0;
	}
}
