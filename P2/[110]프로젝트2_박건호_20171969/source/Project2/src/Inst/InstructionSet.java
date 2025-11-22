package Inst;

import java.util.HashMap;
import java.util.Map;

public class InstructionSet {
    private Map<Integer, Instruction> instructions;

    public InstructionSet() {
        instructions = new HashMap<>();
        addInstruction("STL", 0x14, "3/4");
        addInstruction("JSUB", 0x48, "3/4");
        addInstruction("LDA", 0x00, "3/4");
        addInstruction("COMP", 0x28, "3/4");
        addInstruction("JEQ", 0x30, "3/4");
        addInstruction("J", 0x3C, "3/4");
        addInstruction("STA", 0x0C, "3/4");
        addInstruction("CLEAR", 0xB4, "2");
        addInstruction("LDT", 0x74, "3/4");
        addInstruction("TD", 0xE0, "3/4");
        addInstruction("RD", 0xD8, "3/4");
        addInstruction("COMPR", 0xA0, "2");
        addInstruction("STCH", 0x54, "3/4");
        addInstruction("TIXR", 0xB8, "2");
        addInstruction("JLT", 0x38, "3/4");
        addInstruction("STX", 0x10, "3/4");
        addInstruction("RSUB", 0x4C, "3/4");
        addInstruction("LDCH", 0x50, "3/4");
        addInstruction("WD", 0xDC, "3/4");
    }

    private void addInstruction(String name, int opcode, String format) {
        instructions.put(opcode, new Instruction(name, opcode, format));
    }

    public Instruction getInstruction(int opcode) {
        return instructions.get(opcode);
    }
}
