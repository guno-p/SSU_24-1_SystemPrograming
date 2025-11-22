import java.util.HashMap;
import java.util.Map;

public class ResourceManager {
    /*
    메모리 저장은 어떻게 할것인가.
    input은 String으로 들어온다. - 2개의 String이 1Byte
    판단시에는 String을 int 로 변환하여 판단 - 0000 0000
    - opcode 판단을 위해서는 1111 1100과 & 처리
        2형식인지, 3/4형식인지 판단
        2형식이라면 r1 r2 판단
        3/4형식이라면 nixbpe 판단 후 e비트를 보고 0이면 3형식 (TA가 12bit) 1이면 4형식 (TA가 20bit)
    - nixbpe 판단을 위해서는 0000 0011 1111 와 & 처리
    - TA 판단을 위해서는 0000 0000 0000 1111 ... 와 & 처리

    byte[address] memory
    [0x1000] : 0000 0000
    [0x1001] : 0000 0000
    ...
     */
    private byte[] memory; // 가상 메모리 배열
    private Map<String, Integer> registers; // 레지스터
    private Map<String, Symbol> symtab; // 심볼 테이블
    private String prog_name; // 프로그램 이름 : 첫 H 레코드 이름
    private int start_address; // 프로그램 시작 주소 : 첫 H 레코드 시작주소
    private int prog_length; // 프로그램 길이 : 마지막 section의 Address + length
    private int first_inst_address; // E 레코드 에서 값이 있었다면 저장
    private String log; // 로그 관리 : 명령어 실행 시 append 해나감
    private Map<String, Device> devices; // device 목록
    private Device currentDevice; // 현재 사용중인 디바이스

    // 기타 변수들
    public ResourceManager() {
        memory = new byte[0x3010];
        registers = new HashMap<>();
        registers.put("A", 0);
        registers.put("X", 0);
        registers.put("L", 0);
        registers.put("B", 0);
        registers.put("S", 0);
        registers.put("T", 0);
        registers.put("F", 0);
        registers.put("PC", 0);
        registers.put("SW", 0);
        // SW 설정... 임의로 다음과 같이 하였음
        // < 0x 0001 : 1
        // = 0x 0000 : 0
        // > 0x 0010 : 2

        symtab = new HashMap<>();
        log = "";
        devices = new HashMap<>();
        initializeDevices();
    }

    private void initializeDevices() {
        Device inputDevice = new Device("F1", 256);
        inputDevice.setData(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05}); // 임의로 데이터 설정
        devices.put("F1", inputDevice); // F1 : 01 02 03 04 05 가 저장된 디바이스로 임의 설정!!!
        Device outputDevice = new Device("05", 256);
        devices.put("05", outputDevice); // 05 : 비어있는 디바이스로 임의 설정!!!
        /*
        만약... 디바이스의 내용이 0x1000을 넘긴다면? (COPY 프로그램의 BUFFER 크기를 넘기는 경우)
         */
    }

    public void addSymbol(String name, String section, int address, int length) {
        symtab.put(name, new Symbol(name, section, address, length));
    } // symtab에 심볼을 추가

    public Symbol getSymbol(String name) {
        return symtab.get(name);
    } // symtab 에서 심볼을 찾아서 리턴

    public void loadMemory(int address, String value) { // Memory 에 value를 저장
        // value는 2개의 16진수 문자 String이다. ex) "1F"
        // Ex) 1F -> 0x0001 1111
        int byteValue = Integer.parseInt(value, 16);
        memory[address] = (byte) byteValue;
    }

    public String readMemory(int address) {
        return String.format("%02X", memory[address] & 0xFF);
    } // 메모리를 읽어와서 리턴 (String으로)

    public void setRegister(String name, int value) {
        registers.put(name, value);
    } // 레지스터 값 세팅

    public int getRegister(String name) {
        return registers.getOrDefault(name, 0);
    } // 레지스터로부터 값을 가지고온다. 디폴트는 0

    public void setProg_name(String name) {
        this.prog_name = name;
    } // 리소스매니저가 저장하고있는 프로그램 이름을 세팅

    public void setStart_address(int value) {
        this.start_address = value;
    } // 시작 Address 세팅

    public void setProg_length(int value) { prog_length = value; } // Program 길이 세팅

    public void setFirst_inst_address(int value) { this.first_inst_address = value; } // End 레코드에서 확인하는 END 후 이동하는 Address

    public int getStart_address() { return start_address; }

    public void modifyMemory(int address, int length, String label, boolean isAddition) {
        // 로더의 M Record 만날 시 호출

        // 추가할 값임 : BUFFER : 000033
        int labelValue = symtab.get(label).getAddress();

        // 지정된 길이만큼 메모리를 읽어온다.
        int originalValue = 0;
        if (length == 5 || length == 6) {
            for (int i = 0; i < 3; i++) { // length : 5 or 6
                originalValue <<= 8;
                originalValue |= (memory[address + i] & 0xFF);
            }
        }
        else {
            // 길이가 05나 06이 아니라 처리 못해... 다른 처리 정의 혹은 에러처리 해야함
        }

        // 더하거나 뺀다.
        int modifiedValue;
        if (isAddition) {
            modifiedValue = originalValue + labelValue;
        } else {
            modifiedValue = originalValue - labelValue;
        }

        // 수정된 값을 다시 메모리에 저장한다.
        for (int i = 2; i >= 0; i--) { // 2 1 0
            memory[address + i] = (byte) (modifiedValue & 0xFF);
            modifiedValue >>= 8;
        }
    }

    public String getMemoryStatus() {
        // 메모리 상태를 반환하는 로직
        // 시작 address부터 readMemory 해나간다.
        // 만약 값이 있다면 쓰고, 없다면 --인데, 그러면 그만 쓰도록 한다.
        // 시작address 00 00 00 00 00 00 00 00 00 00\n
        // 시작address 00 00 00 00 00
        // 결과 String을 반환
        StringBuilder result = new StringBuilder();
        int startAddress = 0x0000;
        int endAddress = memory.length;

        for (int address = startAddress; address < endAddress; address += 16) {
            StringBuilder line = new StringBuilder(String.format("0x%04X: ", address));

            for (int offset = 0; offset < 16; offset++) {
                int currentAddress = address + offset;
                String data = readMemory(currentAddress);
                line.append(data).append(" ");
            }

            result.append(line);
            result.append("\n");
        }
        return result.toString();
    }

    public boolean isEndOfProgram() { // 프로그램 종료 조건 검사
        int endPC = registers.get("PC");
        if (endPC == 0x3000) {
            return true;
        }
        return false;
    }

    public void printSymbolTable() { // 디버그 용도
        System.out.println("Symbol Table:");
        for (Symbol symbol : symtab.values()) {
            System.out.println(symbol);
        }
    }

    public void add_inst_to_log(String instruction) { // log에 inst추가
        StringBuilder result_log = new StringBuilder();
        result_log.append(log);
        result_log.append(instruction).append('\n');
        log = result_log.toString(); // 만든 로그를 로그 필드에 저장
    }

    public String getLog() {
        return log;
    }

    public String getDeviceStatus() {
        StringBuilder result = new StringBuilder();
        Device device = currentDevice;
        if (device != null) {
            result.append("Current Device: ").append(device.getName()).append("\n");
            result.append(device.getData(device.getLength()));
        }
        else {
            result.append("Current Device: null\n");
        }

        return result.toString();
    }

    public String getProgramInfo() {
        // 프로그램 정보를 반환
        return String.format(
                "Program Name: %s\nStart Address: %06X\nLength of Program: %06X",
                prog_name,
                start_address,
                prog_length
        );
    }

    public String getEndRecordInfo() {
        // 종료 레코드 정보를 반환
        return String.format(
                "End Record: %06X",
                first_inst_address
        );
    }

    public void setDevice(String name) {
        currentDevice = devices.get(name);
    }

    public Device getDevice(String name) {
        return devices.get(name);
    }
}