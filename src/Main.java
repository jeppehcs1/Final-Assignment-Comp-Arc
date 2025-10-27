public class Main {
    static int pc;
    static int reg[] = new int[4];

    // Here the first program hard coded as an array
    static int progr[] = {
            // As minimal RISC-V assembler example
            0x00200093, // addi x1 x0 2
            0x00300113, // addi x2 x0 3
            0x002081b3, // add x3 x1 x2
    };

    public static void main(String[] args) {

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

    private static void handleIType(int instr) {
        int rd = (instr >> 7) & 0x01f;
        int rs1 = (instr >> 15) & 0x01f;
        int funct3 = (instr >> 12) & 0x01f;
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
                reg[rs1] = (rs1 < imm)?1:0;
                break;
            case 0x3:
                System.out.println("TODO");
                reg[rs1] = (rs1 < imm)?1:0;
                break;
            default:
                System.out.println("Funct3 " + funct3 + " not yet implemented");
                break;
        }

    }

}