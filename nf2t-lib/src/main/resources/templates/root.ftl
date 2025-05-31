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
                    <td><a href="./${mavenProject.getBaseCoordinate().getArtifactId()}/index.html">${mavenProject.getProjectName()}</a></td>
                    <td><a href="./${mavenProject.getBaseCoordinate().getArtifactId()}/javadoc/index.html">JavaDocs Page</a></td>
                    <td>         
                    	<ul>
                    		<li>${mavenProject.isPicocli()?string('true', 'false')} </li>                    	               	                                       
	                    <#if mavenProject.isPicocli() >
	                        <li><a href="./${mavenProject.getBaseCoordinate().getArtifactId()}/man/index.html">Man Page</a></li>
	                    </#if>
	                    </ul>
                    </td>
                </tr>
			  </#list>
            </tbody>
        </table>
    </body>
</html>