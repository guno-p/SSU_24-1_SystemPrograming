package Inst;

public class Instruction {
    private String name;
    private int opcode;
    private String format;

    public Instruction(String name, int opcode, String format) {
        this.name = name;
        this.opcode = opcode;
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public int getOpcode() {
        return opcode;
    }

    public String getFormat() {
        return format;
    }
}
