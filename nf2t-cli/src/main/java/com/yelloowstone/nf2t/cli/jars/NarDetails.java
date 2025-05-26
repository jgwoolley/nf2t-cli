package com.yelloowstone.nf2t.cli.jars;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NarDetails {

	private final String systemApiVersion;
	private final List<NarExtensionDetails> extensions;
	
	public NarDetails(final String systemApiVersion, final List<NarExtensionDetails> extensions) {
		this.systemApiVersion = systemApiVersion;
		this.extensions = Collections.unmodifiableList(extensions);
	}
	
	public String getSystemApiVersion() {
		return systemApiVersion;
	}

	public List<NarExtensionDetails> getExtensions() {
		return extensions;
	}

	public static NarDetails parse(final ZipInputStream zin, final ZipEntry ze)  {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = zin.read(buffer)) > 0) {
				outputStream.write(buffer, 0, len);
			}

			final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
					outputStream.toByteArray());
	
			final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			final Document document = dBuilder.parse(byteArrayInputStream);
			
			return parse(document);
		} catch(Exception e) {
			return null;
		}
	}
	
	
	public static NarDetails parse(final Document document) {
		final NodeList nodeList = document.getElementsByTagName("systemApiVersion");
		final Element element = (Element) nodeList.item(0);

		final String systemApiVersion = element.getTextContent(); 
		final List<NarExtensionDetails> extensions = new ArrayList<>();
		
		final NodeList extensionsNodeList = document.getElementsByTagName("extensions");
		final Element extensionsNode = (Element) extensionsNodeList.item(0);

		
		
		final NodeList extensionNodeList = extensionsNode.getElementsByTagName("extension");
		for (int index = 0; index < extensionNodeList.getLength(); index++) {
			final Element extensionElement = (Element) extensionNodeList.item(index);
			NarExtensionDetails extension = NarExtensionDetails.parse(extensionElement);
			extensions.add(extension);
			
		}
		
		return new NarDetails(systemApiVersion, extensions);
	}
}
