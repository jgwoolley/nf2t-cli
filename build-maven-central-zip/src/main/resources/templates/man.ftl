<html>
    <head>
        <title>${mavenProject.getMavenArtifact().getName()} ManPage</title>
        <style>
            table, th, td {
                border: 2px solid black; 
                border-collapse: collapse;
            }
        </style>
    </head>
    <body>
        <h1>${mavenProject.getMavenArtifact().getName()} ManPage</h1>
		<p><a href="../index.html">Go to parent</a></p>

	<p>The following manpages are available:</p>

        <ul>
          <#list manPaths as manPath>
		    <li><a href="./${manPath}">${manPath}</a>.</li>
		  </#list>
        </ul>
    </body>
</html>