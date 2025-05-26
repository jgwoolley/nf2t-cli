<html>
    <head>
        <title>Projects</title>
        <style>
            table, th, td {
                border: 2px solid black; 
                border-collapse: collapse;
            }
        </style>
    </head>
    <body>
        <h1>Projects</h1>
        <p>The following projects are available:</p>
        
        <table>
            <thead>
                <tr>
                    <th>Project</th>
                    <th>JavaDocs</th>
                    <th>ManPages</th>
                </tr>
            </thead>
            <tbody>
            	<#list mavenProjects as mavenProject>
            	<tr>
                    <td><a href="./${mavenProject.getMavenArtifact().getArtifactId()}/index.html">${mavenProject.getProjectName()}</a></td>
                    <td><a href="./${mavenProject.getMavenArtifact().getArtifactId()}/javadoc/index.html">JavaDocs Page</a></td>
                    <td><a href="./${mavenProject.getMavenArtifact().getArtifactId()}/man/index.html">Man Page</a></td>
                </tr>
			  </#list>
            </tbody>
        </table>
    </body>
</html>