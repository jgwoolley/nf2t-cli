package com.yelloowstone.nf2t.mavenCentralCli;

public class MavenProjectProperty {
	private final String propertyName;
	private final String propertyValue;
	private final String url;
	
	public MavenProjectProperty(String propertyName, String propertyValue) {
		this(propertyName, propertyValue, null);
	}
	
	public MavenProjectProperty(String propertyName, String propertyValue, String url) {
		super();
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.url = url;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getPropertyValue() {
		return propertyValue;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "MavenProjectProperty [propertyName=" + propertyName + ", propertyValue=" + propertyValue + "]";
	}
}
