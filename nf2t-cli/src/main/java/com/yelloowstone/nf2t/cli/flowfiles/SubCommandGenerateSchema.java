package com.yelloowstone.nf2t.cli.flowfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "gen-schema", description = "Generates a JSONSchema for the result of the Unpackage/Package commands.")
public class SubCommandGenerateSchema extends AbstractFlowFilesSubCommand {

	@Option(names = { "-o", "--out" }, description = "The output path.") 
	private String outputOption;
	
	@Override
	public Integer call() throws Exception {
		SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
		getMapper().acceptJsonFormatVisitor(FlowFileStreamResult.class, visitor);
		JsonSchema personSchema = visitor.finalSchema();
		String result = getMapper().writer().writeValueAsString(personSchema);
		getSpec().commandLine().getOut().println(result);
		
		if(outputOption != null) {
			Path out = Paths.get(outputOption);
			if(Files.isDirectory(out)) {
				out = out.resolve("result.schema.json");
			}

			if(!Files.isDirectory(out.getParent())) {
				Files.createDirectories(out.getParent());
			}
			
			Files.write(out, result.getBytes(StandardCharsets.UTF_8));
		}
		
		return 0;
	}

}
