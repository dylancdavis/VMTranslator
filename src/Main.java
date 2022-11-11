import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        //final String[] FILES = {"BasicTest.vm","PointerTest.vm","StaticTest.vm","SimpleAdd.vm","StackTest.vm"};

        final String DIR_NAME = "VMFiles";
        final String OUTPUT_NAME = "NestedCall";

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

                Parser parser = new Parser(DIR_NAME+"/"+fileName);
                parser.createInstructionList();

                System.out.println("Extracted Virtual Instructions for "+fileName+":");
                parser.printVirtual();
                System.out.println();

                parser.convertVirtualtoAssembly();
                System.out.println("Converted Assembly Instructions for "+fileName+":");
                parser.printAssembly();
                System.out.println();

                vmInstructions[fileNum] = parser.getAssemblyLines();
            }

            fileNum++;
        }

        ArrayList<String> bootstrap = new ArrayList<>();
        bootstrap.add("// Bootstrap Code:");
        bootstrap.add("@256");
        bootstrap.add("D=A");
        bootstrap.add("@SP");
        bootstrap.add("M=D");
        bootstrap.add("@Sys.init");
        bootstrap.add("D;JMP");
        bootstrap.add("");

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(OUTPUT_NAME+".asm"));
            for (String s : bootstrap) {
                writer.write(s);
                writer.newLine();
            }
            for (ArrayList<String> lines : vmInstructions) {
                for (String s : lines) {
                    writer.write(s);
                    writer.newLine();
                }
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }
}