<html>
    <head>
        <title>${artifactId}</title>
        <style>
            table, th, td {
                border: 2px solid black; 
                border-collapse: collapse;
            }
        </style>
    </head>
    <body>
        <h1>${artifactId}</h1>
        <table>
            <thead>
                <tr>
                    <th>Property</th>
                    <th>Value</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>artifactId</td>
                    <td>${artifactId}</td>
                </tr>
                <tr>
                    <td>groupId</td>
                    <td>${groupId}</td>
                </tr>
                <tr>
                    <td>version</td>
                    <td>${version}</td>
                </tr>
                <tr>
                    <td>Build Time</td>
                    <td>${buildTime}</td>
                </tr>
            </tbody>
        </table>
        <ul>
        	<li><a href="./javadoc/index.html">See the JavaDocs</a>.</li>
        	<li><a href="./man/index.html">See the ManPages.</a></li>
        </ul>
    </body>
</html>