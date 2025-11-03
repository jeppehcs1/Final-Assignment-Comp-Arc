import java.io.FileInputStream;  // Import FileInputStream
import java.io.IOException;      // Import IOException


public class Main {
    static int pc;
    static int reg[] = new int[4];

    // Here the first program hard coded as an array
    static int progr[] = new int[30];

    public static void main(String[] args) {
        // try-with-resources: FileInputStream will be closed automatically
        try (FileInputStream input = new FileInputStream("filename.txt")) {
            int counter = 0;
            int i;  // variable to store each byte that is read
            byte[] buffer = new byte[4];
            // Read one byte at a time until end of file (-1 means "no more data")
            while ((i = input.read(buffer)) != -1) {
                // Convert the byte to a character and print it to the console
                progr[counter] = i;
                counter++;
            }

        } catch (IOException e) {
            // If an error happens (e.g. file not found), print an error message
            System.out.println("Error reading file.");
        }


        System.out.println("Hello RISC-V World!");

        pc = 0;

        for (; ; ) {

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
                default:
                    System.out.println("Opcode " + opcode + " not yet implemented");
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

        System.out.println("Program exit");

    }

    private static void handleRType(int instr) {

        int rd = (instr >> 7) & 0x01f;
        int funct3 = (instr >> 12) & 0x7;
        int rs1 = (instr >> 15) & 0x01f;
        int rs2 = (instr >> 20) & 0x01f;
        int funct7 = (instr >> 25) & 0x7f;
        switch (funct3) {
            case 0x0:
                reg[rd] = (funct7 == 0x00) ? reg[rs1] + reg[rs2]: reg[rs1] - reg[rs2];
                break;
            case 0x4:
                reg[rd] = reg[rs1] ^ reg[rs2];
                break;
            case 0x6:
                reg[rs1] = reg[rs1] | reg[rs2];
                break;
            case 0x7:
                reg[rs1] = reg[rs1] & reg[rs2];
                break;
            case 0x1:
                reg[rs1] = reg[rs1] << reg[rs2];
                break;
            case 0x5:
                System.out.println("TODO");
                reg[rs1] = reg[rs1] << reg[rs2];
                break;
            case 0x2:
                reg[rs1] = (rs1 < rs2) ? 1 : 0;
                break;
            case 0x3:
                System.out.println("TODO");
                reg[rs1] = (rs1 < rs2) ? 1 : 0;
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
        int imm = (instr >> 20);

        switch (funct3) {
            case 0x0:
                reg[rd] = reg[rs1] + imm;
                break;
            case 0x4:
                reg[rd] = reg[rs1] ^ imm;
                break;
            case 0x6:
                reg[rs1] = reg[rs1] | imm;
                break;
            case 0x7:
                reg[rs1] = reg[rs1] & imm;
                break;
            case 0x1:
                System.out.println("TODO");
                break;
            case 0x5:
                System.out.println("TODO");
                break;
            case 0x2:
                reg[rs1] = (rs1 < imm) ? 1 : 0;
                break;
            case 0x3:
                System.out.println("TODO");
                reg[rs1] = (rs1 < imm) ? 1 : 0;
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