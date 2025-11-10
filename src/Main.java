import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Main {
    static int pc;
    static int reg[] = new int[32];

    // Here the first program hard coded as an array
    static int progr[] = new int[20];

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java Main <file>");
            return;
        }
        String inputPath = "src/" + args[1];
        String outputPath = inputPath.replaceAll("\\.bin$", "") + ".res";

        try (FileInputStream input = new FileInputStream(inputPath)) {
            int counter = 0;
            int i;  // variable to store each byte that is read
            byte[] buffer = new byte[4];
            // Read one byte at a time until end of file (-1 means "no more data")
            while ((i = input.read(buffer)) != -1) {
                // Convert the byte to a character and print it to the console
                ByteBuffer bb = ByteBuffer.wrap(buffer);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                int value = bb.getInt();
                progr[counter] = value;
                counter++;
            }

        } catch (IOException e) {
            // If an error happens (e.g. file not found), print an error message
            System.out.println("Error reading file. " + e.getMessage());
        }

        System.out.println("Hello RISC-V World!");

        pc = 0;

        main_loop: for (; ; ) {
            int instr = progr[pc >> 2];
            int opcode = instr & 0x7f;


            switch (opcode) {

                case 0b0010011:
                    handleIType(instr);
                    break;
                case 0b0110011:
                    handleRType(instr);
                    break;
                case 0b1100011:
                    handleBType(instr);


                case 0b0110111:
                    LoadUpperImm(instr);
                    break;
                case 0b0010111:
                    AddUpperImmToPC(instr);
                    break;
                case 0b1110011:
                    EnvironmentHandle(instr);
                    break main_loop;

                default:
                    System.out.println("Opcode " + Integer.toBinaryString(opcode) + " not yet implemented");
                    break;
            }

            pc += 4; // One instruction is four bytes
            if ((pc >> 2) >= progr.length) {
                break;
            }
            for (int i = 0; i < reg.length; ++i) {
                System.out.print(reg[i] + " ");
            }
            System.out.println();
        }

        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputPath))){
            for (int i = 0; i < reg.length; ++i) {
                outputWriter.write(reg[i]);
            }
        }

        System.out.println("Program exit");

    }

    private static void EnvironmentHandle(int instr) {
        int imm = (instr >> 20);

    }

    private static void AddUpperImmToPC(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int imm = (instr >> 12);
        System.out.println(Integer.toBinaryString(imm));
        reg[rd] = pc + (imm << 12);
    }

    private static void LoadUpperImm(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int imm = (instr >> 12);
        reg[rd] = imm << 12;
    }

    private static void handleRType(int instr) {

        int rd = (instr >> 7) & 0x01f;
        int funct3 = (instr >> 12) & 0x7;
        int rs1 = (instr >> 15) & 0x01f;
        int rs2 = (instr >> 20) & 0x01f;
        int funct7 = (instr >> 25) & 0x7f;
        switch (funct3) {
            case 0x0:
                //ADD or SUB
                reg[rd] = (funct7 == 0x00) ? reg[rs1] + reg[rs2]: reg[rs1] - reg[rs2];
                break;
            case 0x4:
                //XOR
                reg[rd] = reg[rs1] ^ reg[rs2];
                break;
            case 0x6:
                //OR
                reg[rs1] = reg[rs1] | reg[rs2];
                break;
            case 0x7:
                //AND
                reg[rs1] = reg[rs1] & reg[rs2];
                break;
            case 0x1:
                //Shift Left Logical
                reg[rs1] = reg[rs1] << reg[rs2];
                break;
            case 0x5:
                //Shift Right logical or Ahift Right Arithmetic
                if (funct7 == 0x00)
                    reg[rd] = reg[rs1] >>> reg[rs2];
                else if (funct7 == 0x20)
                    reg[rd] = reg[rs1] >> reg[rs2];
                break;
            case 0x2:
                //Set Less Than
                reg[rd] = (rs1 < rs2) ? 1 : 0;
                break;
            case 0x3:
                reg[rd] = (Integer.compareUnsigned(reg[rs1], reg[rs2]) < 0) ? 1 : 0;
                break;
            default:
                System.out.println("Funct3 for R type " + funct3 + " not yet implemented");
                break;
        }
    }

    private static void handleIType(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int rs1 = (instr >> 15) & 0x01f;
        int funct3 = (instr >> 12) & 0x7;
        int imm = (instr >> 20) & 0xFFF;
        int funct7 = (instr >> 25) & 0x7f;

        switch (funct3) {
            case 0x0:
                //Add immediate
                reg[rd] = reg[rs1] + imm;
                break;
            case 0x4:
                //XOR immediate
                reg[rd] = reg[rs1] ^ imm;
                break;
            case 0x6:
                //OR immediate
                reg[rd] = reg[rs1] | imm;
                break;
            case 0x7:
                //AND immediate
                reg[rd] = reg[rs1] & imm;
                break;
            case 0x1:
                //Shift left logical immediate
                reg[rd] = reg[rs1] << imm;
                break;
            case 0x5:
                //Shift right arith/logical immediate
                if (funct7 == 0x00){
                    reg[rd] = reg[rs1] >>> imm;
                } else {
                    reg[rd] = reg[rs1] >> imm;
                }
                break;
            case 0x2:
                //Set less than immediate
                reg[rd] = (rs1 < imm) ? 1 : 0;
                break;
            case 0x3:
                //Set less than immediate unsigned
                reg[rd] = (Integer.toUnsignedLong(reg[rs1]) < Integer.toUnsignedLong(imm)) ? 1 : 0;
                break;
            default:
                System.out.println("Funct3 for I type" + funct3 + " not yet implemented");
                break;
        }
    }

    private static void handleBType(int instr){
        int funct3 = (instr >> 12) & 0x7;
        int rs1 = (instr >> 15) & 0x1F;
        int rs2 = (instr >> 20) & 0x1F;


        int imm = ((instr >> 31) & 0x1) << 12 |
                ((instr >> 7) & 0x1) << 11 |
                ((instr >> 25) & 0x3F) << 5 |
                ((instr >> 8) & 0xF) << 1;

        if ((imm & 0x1000) != 0) imm |= 0xFFFFE000;

        switch (funct3) {
            case 0x0:
                if (reg[rs1] == reg[rs2]) pc += imm;
                break;
            case 0x1:
                if (reg[rs1] != reg[rs2]) pc += imm;
                break;
            case 0x4:
                if (reg[rs1] < reg[rs2]) pc += imm;
                break;
            case 0x5:
                if (reg[rs1] >= reg[rs2]) pc += imm;
                break;
        }

    }

}