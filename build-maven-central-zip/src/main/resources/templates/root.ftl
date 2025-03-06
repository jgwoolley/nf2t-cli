<html>
    <head>
        <title>${name}</title>
        <style>
            table, th, td {
                border: 2px solid black; 
                border-collapse: collapse;
            }
        </style>
    </head>
    <body>
        <h1>${name}</h1>
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
                    <td>${name}</td>
                </tr>
                <tr>
                    <td>Build Time</td>
                    <td>${buildTime}</td>
                </tr>
                <tr>
                    <td>Git Hash</td>
                    <td>${gitHash}</td>
                </tr>
                <tr>
                    <td>Maven Artifact Id</td>
                    <td>${artifactId}</td>
                </tr>
                <tr>
                    <td>Maven Artifact GroupId</td>
                    <td>${groupId}</td>
                </tr>
                <tr>
                    <td>Maven Artifact Version</td>
                    <td>${version}</td>
                </tr>
            </tbody>
        </table>
        <ul>
        	<li><a href="./javadoc/index.html">See the JavaDocs</a>.</li>
        	<li><a href="./man/index.html">See the ManPages.</a></li>
        </ul>
    </body>
</html>