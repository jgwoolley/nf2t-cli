package com.yelloowstone.nf2t.jars;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NarExtensionDetails {

	private final String name;
	private final String type;
	
	public NarExtensionDetails(final String name, final String type) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}



	public String getType() {
		return type;
	}

	public static NarExtensionDetails parse(final Element extensionElement) {
		final NodeList nameNodeList = extensionElement.getElementsByTagName("name");
		final Element extensionNameElement = (Element) nameNodeList.item(0);
		final String name = extensionNameElement.getTextContent();
		
		final NodeList extensionTypeNodeList = extensionElement.getElementsByTagName("type");
		final Element extensionTypeElement = (Element) extensionTypeNodeList.item(0);
		final String type = extensionTypeElement.getTextContent();

		return new NarExtensionDetails(name, type);
	}
}
