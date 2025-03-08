<html>
    <head>
        <title>${mavenProject.getMavenArtifact().getName()}</title>
        <style>
            table, th, td {
                border: 2px solid black; 
                border-collapse: collapse;
            }
        </style>
    </head>
    <body>
        <h1>${mavenProject.getMavenArtifact().getName()}</h1>
        <p><a href="../index.html">Go to parent</a></p>
        <p>${mavenProject.getMavenArtifact().getDescription()}</p>
        <table>
            <thead>
                <tr>
                    <th>Property</th>
                    <th>Value</th>
                </tr>
            </thead>
            <tbody>
            	<tr>
                    <td>Maven Artifact Name</td>
                    <td>${mavenProject.getMavenArtifact().getName()}</td>
                </tr>
                <tr>
                    <td>Build Time</td>
                    <td>${mavenProject.getBuildTime()}</td>
                </tr>
                <tr>
                    <td>Git Hash</td>
                    <td>${mavenProject.getGitHash()}</td>
                </tr>
                <tr>
                    <td>Maven Artifact Id</td>
                    <td>${mavenProject.getMavenArtifact().getArtifactId()}</td>
                </tr>
                <tr>
                    <td>Maven Artifact GroupId</td>
                    <td>${mavenProject.getMavenArtifact().getGroupId()}</td>                   
                </tr>
                <tr>
                    <td>Maven Artifact Version</td>
                    <td>${mavenProject.getMavenArtifact().getVersion()}</td>                   
                </tr>
                <tr>
                	<td>JavaDocs</td>
                	<td><a href="./javadoc/index.html">./javadoc/index.html</a></td>
                </tr>
                <tr>
                	<td>ManPages</td>
                	<td><a href="./man/index.html">./man/index.html</a></td>
                </tr>
            </tbody>
        </table>
    </body>
</html>