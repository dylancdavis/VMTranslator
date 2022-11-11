import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Parser {

    ArrayList<String> virtualLines; // virtual machine code
    ArrayList<String> assemblyLines; // translated assembly
    String inputFileName;

    public Parser(String fileName) {
        this.inputFileName = fileName;
        this.virtualLines = new ArrayList<>();
        this.assemblyLines = new ArrayList<>();


    }

    public void createInstructionList() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFileName));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("//")) {
                    int commentIndex = line.indexOf("//");
                    line = line.substring(0,commentIndex);
                }
                line = line.trim();
                if (line.length() == 0)
                    continue;
                virtualLines.add(line);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void printVirtual() { this.print(virtualLines); }
    public void printAssembly() { this.print(assemblyLines); }

    private void print(ArrayList<String> arrList) {
        int i=0;
        for (String s : arrList) {
            System.out.println(i + ". " + s);
            i++;
        }
    }

    public void convertVirtualtoAssembly() {
        VirtualTranslator translator = new VirtualTranslator(this.inputFileName);
        for (String s : virtualLines) {
            assemblyLines.add("// " + s);
            assemblyLines.addAll(translator.translateLine(s));
            assemblyLines.add("");
        }
    }

    public ArrayList<String> getAssemblyLines() {
        return assemblyLines;
    }
}
