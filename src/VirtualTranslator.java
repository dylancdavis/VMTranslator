import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class VirtualTranslator {

    HashMap<String,String> segmentMap;
    Integer num = 0;

    String fileName;

    List<String> operators = Arrays.asList("add","sub","neg","eq","gt","lt","and","or","not");

    public VirtualTranslator(String fileName) {
        this.fileName = fileName;
        segmentMap = new HashMap<>();
        segmentMap.put("local","LCL");
        segmentMap.put("argument","ARG");
        segmentMap.put("this","THIS");
        segmentMap.put("that","THAT");
    }

    public ArrayList<String> translateLine(String vmLine) {
        if (vmLine.startsWith("push")) {
            return handlePushInstruction(vmLine.substring(5));
        } else if (vmLine.startsWith("pop")) {
            return handlePopInstruction(vmLine.substring(4));
        } else if (operators.contains(vmLine)) {
            System.out.println("Handling operator");
            return handleOperator(vmLine);
        } else if (vmLine.startsWith("goto") || vmLine.startsWith("if-goto") || vmLine.startsWith("label")) {
             return handleBranching(vmLine);
        } else {
            return handleFunction(vmLine);
        }
    }

    private ArrayList<String> handleFunction(String instruction) {
        return null;

    }

    private ArrayList<String> handleBranching(String instruction) {
        ArrayList<String> ret = new ArrayList<>();

        String[] arr = instruction.split(" ");
        String statement = arr[0];
        String label = arr[1];

        switch (statement) {
            case "goto":
                ret.add("@"+label);
                ret.add("D;JMP");
                break;
            case "if-goto":
                ret.add("@SP");
                ret.add("M=M-1");
                ret.add("A=M");
                ret.add("D=M");
                ret.add("@"+label);
                ret.add("D;JNE");
                break;
            case "label":
                ret.add("("+label+")");
                break;
        }

        return ret;

    }

    public ArrayList<String> handlePushInstruction(String instruction) {
        ArrayList<String> ret = new ArrayList<>();

        String[] arr = instruction.split(" ");
        String segment = arr[0];
        String num = arr[1];

        // Push local, argument, this, that, or temp
        if (segmentMap.containsKey(segment)) {
            segment = segmentMap.get(segment);
            ret.add("@" + segment); //load segment address
            ret.add("D=M"); // set into data contents of address
            ret.add("@" + num); // load num
            ret.add("D=D+A"); // change address to increment by num
            ret.add("A=D"); // load address into A
            ret.add("D=M"); // set D to contents
        } else {
            switch (segment) {
                case "constant": // Push constant
                    ret.add("@" + num); //load number
                    ret.add("D=A"); // into D register
                    break;
                case "static":
                    ret.add("@"+fileName+"."+num);
                    ret.add("D=M"); // load into D register
                    break;
                case "temp":
                    ret.add("@" + (5+Integer.parseInt(num)));
                    ret.add("D=M"); // load contents into D register
                    break;
                case "pointer":
                    ret.add("@" + ((num.equals("0")) ? "THIS" : "THAT"));
                    ret.add("D=M"); // load address reference into data
                    break;
                default:
                    System.out.println("Unrecogized segment: " + segment);
            }
        }

        // Once contents are loaded into D, process the rest.
        ret.add("@SP"); // load stack pointer
        ret.add("A=M"); // set address to pointer contents
        ret.add("M=D"); // set memory contents (*SP) to loaded contents
        ret.add("@SP"); // load SP again
        ret.add("M=M+1"); // increment the pointer

        return ret;
    }

    public ArrayList<String> handlePopInstruction(String instruction) {
        ArrayList<String> ret = new ArrayList<>();

        String[] arr = instruction.split(" ");
        String segment = arr[0];
        String num = arr[1];

        // First, decrement the stack pointer
        // So *SP will now reference our contents to pop
        ret.add("@SP"); // load stack pointer
        ret.add("M=M-1"); // decrement pointer

        // Pop local, argument, this, that, or temp
        if (segmentMap.containsKey(segment)) {
            segment = segmentMap.get(segment);
            ret.add("@" + segment); //load segment address
            ret.add("D=M"); // set into data contents of address
            ret.add("@" + num); // load num
            ret.add("D=D+A"); // change address to increment by num, store to D
            ret.add("@R13"); // load ram 13.
            ret.add("M=D"); // address is now stored in R13.
            ret.add("@SP"); // load SP again
            ret.add("A=M"); // change address to reference pointer contents
            ret.add("D=M"); // load contents from address
            ret.add("@R13"); // reload R13
            ret.add("A=M"); // change to memory address to write to
            ret.add("M=D"); // write to that address. whew.
        } else {
            // If we didn't do above, then SP is still loaded into A
            // And points to the first address
            ret.add("A=M"); // so first, load that address into A
            ret.add("D=M"); // and set its contents to the D register.
            switch (segment) { // next load the address to set
                case "constant":
                    System.out.println("Cannot pop to constant segment.");
                    break;
                case "static":
                    ret.add("@"+fileName+"."+num); // load address to write to
                    break;
                case "temp":
                    ret.add("@" + (5+Integer.parseInt(num))); //load address to write to
                    break;
                case "pointer":
                    ret.add("@" + ((num.equals("0")) ? "THIS" : "THAT")); // load address to write to
                    break;
                default:
                    System.out.println("Unrecogized segment: " + segment);
            }
            ret.add("M=D"); // then write to that address
        }
        return ret;
    }

    public ArrayList<String> handleOperator(String instruction) {
        String labelNum = getLabelNum();
        ArrayList<String> ret = new ArrayList<>();
        switch (instruction) {
            case "add":
                ret.add("@SP");
                ret.add("M=M-1"); //decrement SP
                ret.add("A=M");
                ret.add("D=M"); // load first number
                ret.add("@SP");
                ret.add("M=M-1");// decrement SP
                ret.add("A=M"); // load second (into RAM[A])
                ret.add("D=D+M"); // set D to added num
                ret.add("@SP");
                ret.add("A=M");
                ret.add("M=D"); // set stack space to D
                ret.add("@SP");
                ret.add("M=M+1"); // increment SP again
                break;

            case "sub":
                ret.add("@SP");
                ret.add("M=M-1"); //decrement SP
                ret.add("A=M");
                ret.add("D=M"); // load first number
                ret.add("@SP");
                ret.add("M=M-1");// decrement SP
                ret.add("A=M"); // load second (into RAM[A])
                ret.add("D=M-D"); // sub first from second
                ret.add("@SP");
                ret.add("A=M");
                ret.add("M=D"); // set stack space to D
                ret.add("@SP");
                ret.add("M=M+1"); // increment SP again
                break;

            case "eq":
                ret.add("@SP");
                ret.add("M=M-1"); //decrement SP
                ret.add("A=M");
                ret.add("D=M"); // load first number
                ret.add("@SP");
                ret.add("M=M-1");// decrement SP
                ret.add("A=M"); // load second (into RAM[A])
                ret.add("D=D-M"); // set D equal to difference
                ret.add("@ISEQ"+ labelNum); // load jump for eq
                ret.add("D;JEQ"); // jump if D is zero to ISEQ

                ret.add("D=0"); // otherwise set D to zero
                ret.add("@NOTEQ"+ labelNum); // load notEQ
                ret.add("D,JMP"); // and jump to that

                ret.add("(ISEQ"+ labelNum+")");
                ret.add("D=-1"); // set D to 1

                ret.add("(NOTEQ"+ labelNum+")"); // jump point for not eq

                ret.add("@SP");
                ret.add("A=M");
                ret.add("M=D"); // set stack space to D (result)
                ret.add("@SP");
                ret.add("M=M+1"); // increment SP again
                break;

            case "gt":
                ret.add("@SP");
                ret.add("M=M-1"); //decrement SP
                ret.add("A=M");
                ret.add("D=M"); // load first number
                ret.add("@SP");
                ret.add("M=M-1");// decrement SP
                ret.add("A=M"); // load second (into RAM[A])
                ret.add("D=M-D"); // set D equal to difference
                ret.add("@ISGT"+ labelNum); // load jump for eq
                ret.add("D;JGT"); // jump if D is zero to ISEQ

                ret.add("D=0"); // otherwise set D to zero
                ret.add("@NOTGT"+ labelNum); // load notEQ
                ret.add("D,JMP"); // and jump to that

                ret.add("(ISGT"+ labelNum+")");
                ret.add("D=-1"); // set D to 1

                ret.add("(NOTGT"+ labelNum+")"); // jump point for not eq

                ret.add("@SP");
                ret.add("A=M");
                ret.add("M=D"); // set stack space to D (result)
                ret.add("@SP");
                ret.add("M=M+1"); // increment SP again
                break;

            case "lt":
                ret.add("@SP");
                ret.add("M=M-1"); //decrement SP
                ret.add("A=M");
                ret.add("D=M"); // load first number
                ret.add("@SP");
                ret.add("M=M-1");// decrement SP
                ret.add("A=M"); // load second (into RAM[A])
                ret.add("D=M-D"); // set D equal to difference
                ret.add("@ISLT"+ labelNum); // load jump for eq
                ret.add("D;JLT"); // jump if D is zero to ISEQ

                ret.add("D=0"); // otherwise set D to zero
                ret.add("@NOTLT"+ labelNum); // load notEQ
                ret.add("D,JMP"); // and jump to that

                ret.add("(ISLT"+ labelNum+")");
                ret.add("D=-1"); // set D to 1

                ret.add("(NOTLT"+ labelNum+")"); // jump point for not eq

                ret.add("@SP");
                ret.add("A=M");
                ret.add("M=D"); // set stack space to D (result)
                ret.add("@SP");
                ret.add("M=M+1"); // increment SP again
                break;

            case "and":
                ret.add("@SP");
                ret.add("M=M-1"); //decrement SP
                ret.add("A=M");
                ret.add("D=M"); // load first number
                ret.add("@SP");
                ret.add("M=M-1");// decrement SP
                ret.add("A=M"); // load second (into RAM[A])
                ret.add("D=D&M"); // set D to and num
                ret.add("@SP");
                ret.add("A=M");
                ret.add("M=D"); // set stack space to D
                ret.add("@SP");
                ret.add("M=M+1"); // increment SP again
                break;

            case "or":
                ret.add("@SP");
                ret.add("M=M-1"); //decrement SP
                ret.add("A=M");
                ret.add("D=M"); // load first number
                ret.add("@SP");
                ret.add("M=M-1");// decrement SP
                ret.add("A=M"); // load second (into RAM[A])
                ret.add("D=D|M"); // set D to or num
                ret.add("@SP");
                ret.add("A=M");
                ret.add("M=D"); // set stack space to D
                ret.add("@SP");
                ret.add("M=M+1"); // increment SP again
                break;

            case "neg":
                ret.add("@SP");
                ret.add("A=M-1"); //decrement SP
                ret.add("M=-M");
                break;

            case "not":
                ret.add("@SP");
                ret.add("A=M-1"); //decrement SP
                ret.add("M=!M");
                break;
        }
        return ret;
    }

    private String getLabelNum() {
        this.num++;
        return num.toString();
    }





}
