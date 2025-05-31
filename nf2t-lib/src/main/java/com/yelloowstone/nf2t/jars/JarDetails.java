package com.yelloowstone.nf2t.jars;

import java.util.List;
import java.util.jar.Manifest;

public interface JarDetails {
	public NarDetails getNarDetails();
	public Manifest getManifest();
	public List<MavenDescriptorDetails> getMavenDescriptorPoms();
}
