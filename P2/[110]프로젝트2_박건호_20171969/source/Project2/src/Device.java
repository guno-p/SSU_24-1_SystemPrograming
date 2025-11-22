public class Device {
    private String name;
    private byte[] data;
    private int index; // index는 RD WD 할때 증가시켜서 그대로 저장
    private int length; // data에 저장된 길이

    public Device(String name, int size) {
        this.name = name;
        this.data = new byte[size];
        this.index = 0;
        this.length = 0;
    }

    public String getName() {
        return name;
    }

    public byte read() {
        if (index < data.length) {
            return data[index++];
        } else {
            return 0; // No more data to read
        }
    }

    public void write(byte value) {
        if (index < data.length) {
            data[index++] = value;
            length++;
        }
        else {
            // data 길이를 초과한 index
        }
    }

    public void resetIndex() {
        this.index = 0;
    }
    public int getIndex() { return this.index; }
    public int getLength() { return this.length; }

    public void setData(byte[] data) {
        if (data.length <= this.data.length) {
            System.arraycopy(data, 0, this.data, 0, data.length);
        } // argument data 를 필드로 복사
        length = data.length; // set 한 길이로 length 설정
    }

    public String getData(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(String.format("%02X", data[i])); // Formatting as two digit hex value
        }
        return result.toString();
    }
}
