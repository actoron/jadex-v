package jadex.idgenerator;

import java.io.File;
import java.util.Arrays;
import java.util.StringTokenizer;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import jadex.common.SUtil;

public class WordsClassGenerator 
{
	public static void main(String[] args) throws Exception
	{   
		//String pck = args.length>0? args[0]: "jadex.mj.core.impl"; // do we want that? would need dir created also here
		String pck = args.length>0? args[0]: "jadex.idgenerator";
		
	    String[] adj_a = readValuesFromFile("adj_a.txt", 256);
	    String[] adj_b = readValuesFromFile("adj_b.txt", 256);
	    String[] adj_d = readValuesFromFile("adj_d.txt", 256);
	    String[] adj_f = readValuesFromFile("adj_f.txt", 256);
	    
	    String[] noun_a = readValuesFromFile("noun_a.txt", 1024);
	    String[] noun_b = readValuesFromFile("noun_b.txt", 1024);
	    String[] noun_d = readValuesFromFile("noun_d.txt", 1024);
	    String[] noun_f = readValuesFromFile("noun_f.txt", 1024);
	    
	    String adj_init = generateArrayInitializer(256, adj_a, adj_b, adj_d, adj_f);
	    String noun_a_init = generateArrayInitializer(1024, noun_a);
	    String noun_b_init = generateArrayInitializer(1024, noun_b);
	    String noun_d_init = generateArrayInitializer(1024, noun_d);
	    String noun_f_init = generateArrayInitializer(1024, noun_f);
	    
	    generateClass(pck, adj_init, noun_a_init, noun_b_init, noun_d_init, noun_f_init);
	}
	
	public static String[] readValuesFromFile(String filename, int len)
	{
		String values = SUtil.readFile(filename);
		StringTokenizer stok = new StringTokenizer(values, "\n,\"");
		if(len==0)
			len = stok.countTokens();
		String[] ret = new String[len];
		for(int i=0; i<len; i++)
		{
			ret[i] = stok.nextToken();
		}
		return ret;
	}
	
	public static String generateArrayInitializer(int len, String[] ... values)
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("{");
		for(int i=0; i<values.length; i++)
		{
			for(int j=0; j<values[i].length && j<len; j++)
			{
				builder.append("\"").append(values[i][j]).append("\"");
				if(j+1<len || i+1<values.length)
					builder.append(",");
			}
		}
		builder.append("}");
		
		return builder.toString();
	}
	
	public static void generateClass(String pck, String adj_init, String noun_a_init, String noun_b_init, String noun_d_init, String noun_f_init) throws Exception
	{
		TypeSpec classspec = TypeSpec.classBuilder("Words")
			.addModifiers(Modifier.PUBLIC)
			.addField(FieldSpec.builder(String[].class, "ADJECTIVES", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
			.initializer(adj_init)
			.build())
			.addModifiers(Modifier.PUBLIC)
			.addField(FieldSpec.builder(String[].class, "NOUNS_A", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
			.initializer(noun_a_init)
			.build())
			.addModifiers(Modifier.PUBLIC)
			.addField(FieldSpec.builder(String[].class, "NOUNS_B", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
			.initializer(noun_b_init)
			.build())
			.addModifiers(Modifier.PUBLIC)
			.addField(FieldSpec.builder(String[].class, "NOUNS_D", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
			.initializer(noun_d_init)
			.build())
			.addModifiers(Modifier.PUBLIC)
			.addField(FieldSpec.builder(String[].class, "NOUNS_F", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
			.initializer(noun_f_init)
			.build())
			.addMethod(MethodSpec.methodBuilder("main")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(String[].class, "args")
            .addCode(CodeBlock.builder()
            	.addStatement("System.out.println(\"adjectives: \"+ADJECTIVES.length)")
            	.addStatement("$T.stream(ADJECTIVES).forEach(v -> System.out.print(v+\",\"))", Arrays.class)
            	.addStatement("System.out.println(\"\\nnouns a: \"+NOUNS_A.length)")
            	.addStatement("$T.stream(NOUNS_A).forEach(v -> System.out.print(v+\",\"))", Arrays.class)
            	.addStatement("System.out.println(\"\\nnouns b: \"+NOUNS_B.length)")
            	.addStatement("$T.stream(NOUNS_B).forEach(v -> System.out.print(v+\",\"))", Arrays.class)
            	.addStatement("System.out.println(\"\\nnouns d: \"+NOUNS_D.length)")
            	.addStatement("$T.stream(NOUNS_D).forEach(v -> System.out.print(v+\",\"))", Arrays.class)
            	.addStatement("System.out.println(\"\\nnouns f: \"+NOUNS_F.length)")
            	.addStatement("$T.stream(NOUNS_F).forEach(v -> System.out.print(v+\",\"))", Arrays.class)
            .build())
            .build())
			.build();
		
        JavaFile.Builder builder = JavaFile.builder("jadex.idgenerator", classspec);
        JavaFile file = builder.build();
	    file.writeTo(System.out);
	    file.writeTo(new File("src/main/java"));
	}
}
