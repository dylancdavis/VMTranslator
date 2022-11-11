import java.util.*;

public class VirtualTranslator {

    HashMap<String,String> segmentMap;
    Integer num_labels = 0;
    Integer num_functions = 0;

    String currentFunction = "init";

    String[] segmentNames = {"LCL","ARG","THIS","THAT"};

    String fileName;

    List<String> operators = Arrays.asList("add","sub","neg","eq","gt","lt","and","or","not");

    public VirtualTranslator(String fileName) {
        this.fileName = fileName.substring(0,fileName.indexOf('.'));
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
            return handleOperator(vmLine);
        } else if (vmLine.startsWith("goto") || vmLine.startsWith("if-goto") || vmLine.startsWith("label")) {
             return handleBranching(vmLine);
        } else if (vmLine.startsWith("call")) {
            return handleFunctionCall(vmLine);
        } else if (vmLine.startsWith("function")) {
            return handleFunctionDeclaration(vmLine);
        } else if (vmLine.startsWith("return")) {
            return handleReturn(vmLine);
        } else {
            throw new RuntimeException("Unrecognized vmLine command: " + vmLine);
        }
    }

    private ArrayList<String> handleFunctionCall(String instruction) {
        ArrayList<String> ret = new ArrayList<>();

        String[] arr = instruction.split(" ");
        String functionName = arr[1];
        int nArgs = Integer.parseInt(arr[2]);
        int toSub = 5+nArgs;

        // names return label with current function and an arbitrary (unique) number.
        String returnVar = functionName+".returnAddress."+getFunctionNum();

        ret.add("@"+returnVar); // should be replaced with proper return address.
        ret.add("D=A"); // set D equal to the return value.
        ret.addAll(addDRegisterToStack());

        // All addresses to save
        for (String s : segmentNames) {
            ret.add("@"+s);
            ret.add("D=M");
            ret.addAll(addDRegisterToStack());
        }

        ret.add("@SP");
        ret.add("D=M"); // load SP pointed addr into D

        ret.add("@LCL"); // load LCL addr
        ret.add("M=D"); // set new LCL to the SP pointed one

        ret.add("@"+toSub); // load num to sub into A
        ret.add("D=D-A"); // change D to be new sub value
        ret.add("@ARG"); // load ARG location
        ret.add("M=D"); // change ARG value to appropriate location

        ret.add("@"+functionName); // load addr of function name label
        ret.add("D;JMP"); // and jump to it

        ret.add("("+returnVar+")"); // add label of return value to go to.

        return ret;
    }

    private ArrayList<String> handleFunctionDeclaration(String instruction) {
        ArrayList<String> ret = new ArrayList<>();
        String[] arr = instruction.split(" ");
        String functionName = arr[1];
        int nVars = Integer.parseInt(arr[2]);

        ret.add("("+functionName+")");

        if (nVars != 0) {
            ret.add("@SP");
            // Push stack forward the number of local variables needed.
            for (int i=0;i<nVars;i++) {
                ret.add("A=M");
                ret.add("M=0");
                ret.add("@SP");
                ret.add("M=M+1");
            }
        }

        return ret;

    }

    private ArrayList<String> handleReturn(String instruction) {
        ArrayList<String> ret = new ArrayList<>();

        // Create the endFrame variable
        // Note- it actually starts at the beginning, lol.
        ret.add("@LCL");
        ret.add("D=M");
        ret.add("@5");
        ret.add("D=D-A");
        ret.add("@endFrame");
        ret.add("M=D"); // endFrame contains reference at beginning of frame (return address)

        // Create the returnAddress variable
        ret.add("@endFrame");
        ret.add("A=M"); // change A equal to *endFrame
        ret.add("D=M"); // store in D the return address found at endFrame
        ret.add("@returnAddress"); // load new var for return address
        ret.add("M=D"); // store the return address in that var

        // Store the top of the stack onto arg[0]
        ret.add("@SP");
        ret.add("A=M"); // load location of top of stack
        ret.add("A=A-1");
        ret.add("D=M"); // store value of top of stack into D
        ret.add("@ARG");
        ret.add("A=M"); // load addr of ARG
        ret.add("M=D"); // store in ARG[0], the value of D

        // SP = ARG+1
        ret.add("@ARG");
        ret.add("D=M"); // store ARG pointer location into D
        ret.add("@SP");
        ret.add("M=D+1"); // change SP contents to reference ARG pointer +1

        // Reinstate Segments
        for (String s : segmentNames) {
            ret.add("@endFrame");
            ret.add("M=M+1"); // move endFrame contents to point to appropriate segment
            ret.add("A=M"); // perform * so A now has reference to saved segment
            ret.add("D=M"); // store saved value in D
            ret.add("@"+s); // load segment
            ret.add("M=D"); // change segment pointer to reference saved value
        }

        // Return to return address
        ret.add("@returnAddress");
        ret.add("A=M"); // load whatever value is stored there into A
        ret.add("D;JMP");

        return ret;

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
        ret.addAll(addDRegisterToStack());
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
        this.num_labels++;
        return num_labels.toString();
    }

    private String getFunctionNum() {
        this.num_functions++;
        return num_functions.toString();
    }

    private ArrayList<String> addDRegisterToStack() {
        ArrayList<String> ret = new ArrayList<>();

        ret.add("@SP"); // load stack pointer
        ret.add("A=M"); // set address to pointer contents
        ret.add("M=D"); // set memory contents (*SP) to loaded contents
        ret.add("@SP"); // load SP again
        ret.add("M=M+1"); // increment the pointer

        return ret;
    }





}
