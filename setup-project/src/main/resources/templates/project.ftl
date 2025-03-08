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
			  	 <#list properties as property>
	                <tr>
					    <td>${property.getPropertyName()}</td>
					    <#if property.getUrl()?? >
	                    	<td><a href="${property.getUrl()}">${property.getPropertyValue()}</a></td>
	                    <#else>
	                    	<td>${property.getPropertyValue()}</td>	                   
	                    </#if>
	                </tr>
				</#list>   			  
            </tbody>
        </table>
    </body>
</html>