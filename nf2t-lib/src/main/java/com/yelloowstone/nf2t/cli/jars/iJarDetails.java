package com.yelloowstone.nf2t.cli.jars;

import java.util.List;
import java.util.jar.Manifest;

public interface iJarDetails {
	public NarDetails getNarDetails();
	public Manifest getManifest();
	public List<MavenPomDetails> getMavenPoms();
}
