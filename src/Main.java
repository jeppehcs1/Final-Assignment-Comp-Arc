import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Main {
    static int pc;
    static int sp;
    static int reg[] = new int[32];
    static byte[] mem = new byte[1048576];

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java IsaSim <file>");
            return;
        }
        String inputPath = args[0];
        String outputPath = inputPath.replaceAll("\\.bin$", "") + ".out";

        try (FileInputStream input = new FileInputStream(inputPath)) {
            int counter = 0;
            int i;  //
            // Read one byte at a time until end of file (-1 means "no more data")
            while ((i = input.read()) != -1) {
                mem[counter] = (byte) i;
                counter++;
            }

        } catch (IOException e) {
            // If an error happens (e.g. file not found), print an error message
            System.out.println("Error reading file. " + e.getMessage());
        }



        pc = 0;

        main_loop: for (; ; ) {
            int instr = ((mem[pc]   & 0xFF)) |
                    ((mem[pc+1] & 0xFF) << 8) |
                    ((mem[pc+2] & 0xFF) << 16) |
                    ((mem[pc+3] & 0xFF) << 24);
            int opcode = instr & 0x7f;

            switch (opcode) {
                case 0b1100111:
                    jumpAndLinkReg(instr);
                    break;
                case 0b1101111:
                    jumpAndLink(instr);
                    break;
                case 0b0010011:
                    handleIType(instr);
                    break;
                case 0b0110011:
                    handleRType(instr);
                    break;
                case 0b1100011:
                    handleBType(instr);
                    break;
                case 0b0000011:
                    handleITypeMem(instr);
                    break;
                case 0b0100011:
                    handleSType(instr);
                    break;
                case 0b0110111:
                    LoadUpperImm(instr);
                    break;
                case 0b0010111:
                    AddUpperImmToPC(instr);
                    break;
                case 0b1110011:
                    if (EnvironmentCall()) {

                        break main_loop;
                    }
                    break;
                default:
                    System.out.println("Opcode " + Integer.toBinaryString(opcode) + " not yet implemented");
                    break main_loop;
            }
            reg[0] = 0;
            pc += 4; // One instruction is four bytes


            for (int i = 0; i < reg.length; ++i) {
                System.out.print(reg[i] + " ");
            }
            System.out.println();


        }

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            for (int i = 0; i < reg.length; ++i) {
                buffer.clear();
                buffer.putInt(reg[i]);
                fos.write(buffer.array());
            }
        }
        System.out.println("Program exit");

    }
    private static void jumpAndLinkReg(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int rs1 = (instr >> 15) & 0x01f;
        int funct3 = (instr >> 12) & 0x7;
        int imm = (instr >> 20) & 0xFFF;
        if ((imm & 0x800) != 0)
            imm |= 0xFFFFF000;
        int funct7 = (instr >> 25) & 0x7f;
        reg[rd] = pc + 4;
        pc = reg[rs1] + imm - 4;
    }
    private static void jumpAndLink(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int imm = ((instr >> 31) & 0x1) << 20 |
                ((instr >> 21) & 0x3FF) << 1 |
                ((instr >> 20) & 0x1) << 11 |
                ((instr >> 12) & 0xFF) << 12;
        if ((imm & (1 << 20)) != 0) imm |= 0xFFF00000;
        reg[rd] = pc + 4;
        pc += imm - 4;

    }

    private static boolean EnvironmentCall() {
        int id = reg[17];
        int val = reg[10];
        switch (id) {
            case 1:
                // print_int
                System.out.println(val);
                return false;
            case 2:
                // print_float
                System.out.println(Float.intBitsToFloat(val));
                return false;
            case 4:
                // print_string

                while (mem[val] != 0) {
                    System.out.print((char) mem[val]);
                    val += 1;
                }
                return false;
            case 10:
                // exit
                return true;
            case 11:
                // print_char
                System.out.println((char) val);
                return false;
            case 34:
                // print_hex
                System.out.println(Integer.toHexString(val));
                return false;
            case 35:
                // print_bin
                System.out.println(Integer.toBinaryString(val));
                return false;
            case 36:
                // print_unsigned
                System.out.println(Integer.toUnsignedLong(val));
                return false;
            case 93:
                // exit 2

                return true;
            default:
                System.out.println("Unknown environment");
                return false;
        }
    }

    private static void AddUpperImmToPC(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int imm = (instr >> 12);
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
                reg[rd] = reg[rs1] | reg[rs2];
                break;
            case 0x7:
                //AND
                reg[rd] = reg[rs1] & reg[rs2];
                break;
            case 0x1:
                //Shift Left Logical
                reg[rd] = reg[rs1] << reg[rs2];
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
                reg[rd] = (reg[rs1] < reg[rs2]) ? 1 : 0;
                break;
            case 0x3:
                reg[rd] = (Integer.compareUnsigned(reg[rs1], reg[rs2]) < 0) ? 1 : 0;
                break;
            default:
                System.out.println("Funct3 for R type " + funct3 + " not yet implemented");
                break;
        }
    }
    private static void handleITypeMem(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int rs1 = (instr >> 15) & 0x01f;
        int funct3 = (instr >> 12) & 0x7;
        int imm = (instr >> 20);
        int funct7 = (instr >> 25) & 0x7f;
        int result = 0;
        int addr = reg[rs1] + imm;
        switch (funct3) {
            case 0x0:
                reg[rd] = mem[reg[rs1]+imm];
                break;
            case 0x1:

                result = (mem[addr + 1] & 0xFF) << 8;
                result |= (mem[addr] & 0xFF);
                reg[rd] = (short) result;
                break;
            case 0x2:
                result = (mem[reg[rs1]+imm+3] & 0xFF) << 24;
                result |= (mem[reg[rs1]+imm+2] & 0xFF) << 16;
                result |= (mem[reg[rs1]+imm+1] & 0xFF) << 8;
                result |= (mem[reg[rs1]+imm] & 0xFF);
                reg[rd] = result;
                break;
            case 0x4:
                reg[rd] = mem[reg[rs1]+imm] & 0xFF;
                break;
            case 0x5:
                result = (mem[reg[rs1] + imm + 1] & 0xFF) << 8;
                result |= (mem[reg[rs1] + imm] & 0xFF);
                reg[rd] = result;
                break;
        }
    }
    private static void handleIType(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int rs1 = (instr >> 15) & 0x01f;
        int funct3 = (instr >> 12) & 0x7;
        int imm = (instr >> 20);
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
                reg[rd] = (reg[rs1] < imm) ? 1 : 0;
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
    private static void handleSType(int instr) {
        int funct3 = (instr >> 12) & 0x7;
        int rs1 = (instr >> 15) & 0x01f;
        int rs2 = (instr >> 20) & 0x01f;
        int imm = ((instr >> 25) & 0x7F) << 5   |
                ((instr >> 7)  & 0x1F);


        if ((imm & 0x800) != 0) {    // if bit 11 is 1
            imm |= 0xFFFFF000;
        }
        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(reg[rs2]).array();
        int addr = reg[rs1] + imm;

        switch (funct3) {
            case 0x0: // SB
                mem[addr] = bytes[0];
                break;

            case 0x1: // SH
                mem[addr]     = bytes[0];
                mem[addr + 1] = bytes[1];
                break;

            case 0x2: // SW
                mem[addr]     = bytes[0];
                mem[addr + 1] = bytes[1];
                mem[addr + 2] = bytes[2];
                mem[addr + 3] = bytes[3];
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
                if (reg[rs1] == reg[rs2]) pc += imm - 4;
                break;
            case 0x1:
                if (reg[rs1] != reg[rs2]) pc += imm - 4;
                break;
            case 0x4:
                if (reg[rs1] < reg[rs2]) pc += imm - 4;
                break;
            case 0x5:
                if (reg[rs1] >= reg[rs2]) pc += imm - 4;
                break;
            case 0x6:
                if (Integer.toUnsignedLong(reg[rs1]) < Integer.toUnsignedLong(reg[rs2])) pc += imm - 4;
                break;
            case 0x7:
                if (Integer.toUnsignedLong(reg[rs1]) >= Integer.toUnsignedLong(reg[rs2])) pc += imm - 4;
                break;
        }

    }

}