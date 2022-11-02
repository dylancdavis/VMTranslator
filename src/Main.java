import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        //final String[] FILES = {"BasicTest.vm","PointerTest.vm","StaticTest.vm","SimpleAdd.vm","StackTest.vm"};

        final String FILE_PATH = "BasicLoop.vm";
        final boolean MANUAL_INPUT = false;

        Scanner input = new Scanner(System.in);

        String fileName;

        if (MANUAL_INPUT) {
            System.out.println("Please enter the path to the file you want to assemble:");
            fileName = input.nextLine();
        } else {
            System.out.println("Manual input disabled, using: " + FILE_PATH);
            fileName = FILE_PATH;
        }
        System.out.println();

            Parser parser = new Parser(fileName);
            parser.createInstructionList();

            System.out.println("Extracted Virtual Instructions:");
            parser.printVirtual();
            System.out.println();

            parser.convertVirtualtoAssembly();
            System.out.println("Converted Assembly Instructions");
            parser.printAssembly();
            System.out.println();

            parser.writeToFile();



    }
}