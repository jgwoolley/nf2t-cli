<html>
    <head>
        <title>${name} ManPage</title>
        <style>
            table, th, td {
                border: 2px solid black; 
                border-collapse: collapse;
            }
        </style>
    </head>
    <body>
        <h1>${artifactId} ManPage</h1>

	<p>The following manpages are available:</p>

        <ul>
          <#list manPaths as manPath>
		    <li><a href="./${manPath}">${manPath}</a>.</li>
		  </#list>
        </ul>
    </body>
</html>