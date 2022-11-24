import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Main {

    static final String DIR_NAME = "VMFiles";
    static final String OUTPUT_NAME = "SimpleFunction";

    static final boolean USE_BOOTSTRAP = false;

    public static void main(String[] args) {

        ArrayList<String> bootstrap = getBootstrapAssembly();

        int functionNum = 1;

        File dir = new File(DIR_NAME);
        File[] files = dir.listFiles();

        if (files.length == 0) {
            throw new RuntimeException("No files ending in .vm in directory " + DIR_NAME);
        }

        ArrayList<String>[] vmInstructions = new ArrayList[files.length];

        int fileNum = 0;

        for (File f : files) {
            String fileName = f.getName();
            String extension = fileName.substring(fileName.indexOf('.'));
            if (f.isFile() && extension.equals(".vm")) {

                Parser parser = new Parser(DIR_NAME+"/"+fileName, functionNum);
                parser.createInstructionList();

                System.out.println("Extracted Virtual Instructions for "+fileName+":");
                parser.printVirtual();
                System.out.println();

                parser.convertVirtualtoAssembly();
                System.out.println("Converted Assembly Instructions for "+fileName+":");
                parser.printAssembly();
                System.out.println();

                vmInstructions[fileNum] = parser.getAssemblyLines();

                functionNum = parser.getFunctionNum();
            }
            fileNum++;

        }

        BufferedWriter writer = null;
        try {
            int i = 0;

            writer = new BufferedWriter(new FileWriter(OUTPUT_NAME+".asm"));
            BufferedWriter lineNumWriter = new BufferedWriter(new FileWriter(OUTPUT_NAME+"LineNumbers.asm"));

            if (USE_BOOTSTRAP) {
                for (String s : bootstrap) {
                    writer.write(s);
                    writer.newLine();
                    if (s != "" && !s.startsWith("//")) {
                        lineNumWriter.write(i + ". " + s);
                        i++;
                    } else {
                        lineNumWriter.write(s);
                    }
                    lineNumWriter.newLine();
                }
            }
            for (ArrayList<String> lines : vmInstructions) {
                for (String s : lines) {
                    writer.write(s);
                    writer.newLine();
                    if (s != "" && !s.startsWith("//") && !s.startsWith("(")) {
                        lineNumWriter.write(i + ". " + s);
                        i++;
                    } else {
                        lineNumWriter.write(s);
                    }
                    lineNumWriter.newLine();
                }
            }
            writer.close();
            lineNumWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static ArrayList<String> getBootstrapAssembly() {

        ArrayList<String> bootstrap = new ArrayList<>();

        bootstrap.add("// Bootstrap Code:");
        bootstrap.add("@256");
        bootstrap.add("D=A");
        bootstrap.add("@SP");
        bootstrap.add("M=D");
        bootstrap.add("");

        Parser bootstrapParser = new Parser("Bootstrap/Bootstrap.vm", 0);
        bootstrapParser.createInstructionList();

        System.out.println("Extracted Virtual Instructions for Bootstrap:");
        bootstrapParser.printVirtual();
        System.out.println();

        bootstrapParser.convertVirtualtoAssembly();
        System.out.println("Converted Assembly Instructions for Bootstrap:");
        bootstrapParser.printAssembly();
        System.out.println();

        bootstrap.addAll(bootstrapParser.getAssemblyLines());

        return bootstrap;
    }
}