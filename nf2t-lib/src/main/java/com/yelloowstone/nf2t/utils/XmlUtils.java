package com.yelloowstone.nf2t.utils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlUtils {
	public static Element getChildByTagName(final Element parentElement,final String tagName) {
        final NodeList children = parentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
        	final Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }
	
	public static String getTextContentByTagName(final Element parentElement, final String tagName) {
		Element element = getChildByTagName(parentElement, tagName);
		if(element == null) {
			return null;
		}
		
		final String textContent = element.getTextContent();
		if(textContent == null) {
			return null;
		}
		return textContent.strip();
	}
	
	public static String getTextContentByTagNameWithDefault(final Element parentElement, final String defaultTag, final String tagName) {
		final String localValue = getTextContentByTagName(parentElement, tagName);
		if(localValue != null) {
			return localValue;
		}
		final Element defaultElement = getChildByTagName(parentElement, defaultTag);
		return getTextContentByTagName(defaultElement, tagName);
		
	}

	public static String getTextContentByTagNameWithDefault(Element parentElement, String defaultTag, String tagName, String defaultValue) {
		final String value = getTextContentByTagNameWithDefault(parentElement, defaultTag, tagName);
		if(value == null) {
			return defaultValue;
		}
		return value;
	}
}
