public class Symbol {
    private String name;
    private String section;
    private int address;
    private int length;

    public Symbol(String name, String section, int address, int length) {
        this.name = name;
        this.section = section;
        this.address = address;
        this.length = length;
    }

    // Getter 메서드들
    public String getName() {
        return name;
    }

    public String getSection() {
        return section;
    }

    public int getAddress() {
        return address;
    }

    public int getLength() {
        return length;
    }

    // toString 메서드 (디버깅 용도)
    @Override
    public String toString() {
        return "Symbol " +
                "name='" + name + '\'' +
                ", section='" + section + '\'' +
                ", address=" + String.format("%06X", address) +
                ", length=" + (length == -1 ? "-" : String.format("%04X", length));
    }
}
