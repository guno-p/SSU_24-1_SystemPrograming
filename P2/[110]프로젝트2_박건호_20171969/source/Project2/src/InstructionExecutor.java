import Inst.*;
public class InstructionExecutor {
    private ResourceManager resourceManager;
    private InstructionSet instructionSet;
    private int current_inst_length; // 2, 3, 4 : 형식 정보를 알 수 있음
    private int current_inst_nixbpe; // 00ni xbpe 0000 저장
    private boolean n_flag;
    private boolean i_flag;
    private boolean x_flag;
    private boolean b_flag;
    private boolean p_flag;
    private boolean e_flag;

    public InstructionExecutor(ResourceManager resourceManager) {
        instructionSet = new InstructionSet(); // Set 인스턴스 생성
        this.resourceManager = resourceManager;
    }

    public void executeNext() {
        // 2형식은 간단하게 작성

        // 3/4형식은 nixbpe에 따라 생각

        // n == 1, i == 0 : indirect : target 구하고 해당 구역의 값을 target으로 한다
        // ex) J @RETADR : RETADR 의 값을 가져와서 target으로 세팅
        // target에 담긴게 3B라는 가정이 전제

        // n == 0, i == 1 : immediate : m..m2 에 있는 값을 상수로서 사용 한다
        // ex) COMP #0 / LDA #3 : 00 00 00 과 00 00 03

        // n, i 둘 다 00 혹은 11 : 심플하게 e 판단 후 pc, b 판단

        // x 가 1이면 target address에 (x) 더하기

        initialize_flag(); // nixbpe flag 초기화

        int pc = resourceManager.getRegister("PC"); // 현재 명령어 시작 주소

        // 메모리에 저장된 명령어가 17 02 29 라면 17 0001 0111 중에 0001 01만 opcode
        int opcode = Integer.parseInt(resourceManager.readMemory(pc), 16) & 0xFC;
        // opcode의 상위 6비트를 사용

        // 3/4 형식의 nixbpe 저장 -> 2형식에서는 사용 X
        current_inst_nixbpe = ( Integer.parseInt(resourceManager.readMemory(pc), 16) & 0x03 ) << 8; // ni
        current_inst_nixbpe += Integer.parseInt(resourceManager.readMemory(pc + 1), 16) & 0xF0; // xbpe
        // __nixbpe : 0000 00ni xbpe 0000
        //                   21 8421

        // flag setting
        if ( (current_inst_nixbpe & 0x200) == 0 && (current_inst_nixbpe & 0x100) != 0 ) { // n == 0, i == 1 : immediate
            i_flag = true;
        }
        if ( (current_inst_nixbpe & 0x200) != 0 && (current_inst_nixbpe & 0x100) == 0 ) { // n == 1, i == 0 : indirect
            n_flag = true;
        }
        if ((current_inst_nixbpe & 0x80) != 0) { // x == 1 : index setting
            x_flag = true;
        }
        if ((current_inst_nixbpe & 0x40) != 0) { // b == 1 : base relative
            b_flag = true;
        }
        if ((current_inst_nixbpe & 0x20) != 0) { // p == 1 : PC relative
            p_flag = true;
        }
        if ((current_inst_nixbpe & 0x10) != 0) { // e == 1 : extended form 4형식
            e_flag = true;
        }

        // instruction 정보 가지고 오기
        Instruction instruction = instructionSet.getInstruction(opcode);
        if (instruction == null) {
            throw new IllegalStateException("Unknown instruction: " + Integer.toHexString(opcode));
        }

        setInstructionLength(instruction); // 필드에 명령어 길이 저장
        // System.out.println("inst : " + instruction.getName()); // 디버그용

        switch (instruction.getName()) {
            case "STL": // 3/4
                executeSTL(pc);
                break;
            case "JSUB": // 3/4
                executeJSUB(pc);
                break;
            case "LDA": // 3/4
                executeLDA(pc);
                break;
            case "COMP": // 3/4
                executeCOMP(pc);
                break;
            case "JEQ": // 3/4
                executeJEQ(pc);
                break;
            case "J": // 3/4
                executeJ(pc);
                break;
            case "STA": // 3/4
                executeSTA(pc);
                break;
            case "CLEAR": // 2
                executeCLEAR(pc);
                break;
            case "LDT": // 3/4
                executeLDT(pc);
                break;
            case "TD": // 3/4
                executeTD(pc);
                break;
            case "RD": // 3/4
                executeRD(pc);
                break;
            case "COMPR": // 2
                executeCOMPR(pc);
                break;
            case "STCH": // 3/4
                executeSTCH(pc);
                break;
            case "TIXR": // 2
                executeTIXR(pc);
                break;
            case "JLT": // 3/4
                executeJLT(pc);
                break;
            case "STX": // 3/4
                executeSTX(pc);
                break;
            case "RSUB": // 3/4
                executeRSUB(pc);
                break;
            case "LDCH": // 3/4
                executeLDCH(pc);
                break;
            case "WD": // 3/4
                executeWD(pc);
                break;
            default:
                throw new IllegalStateException("Unsupported instruction: " + instruction.getName());
        }

        resourceManager.add_inst_to_log(instruction.getName()); // log 추가 !!!
    }

    private void initialize_flag () { // nixbpe flag 초기화
        n_flag = false;
        i_flag = false;
        x_flag = false;
        b_flag = false;
        p_flag = false;
        e_flag = false;
    }

    private void setInstructionLength(Instruction instruction) { // 현재 명령어의 길이 정보 저장
        if (instruction.getFormat().equals("2")) {
            current_inst_length = 2;
        } else if (instruction.getFormat().equals("3/4")) {
            // 명령어의 e 비트를 확인하여 길이 결정 (3 또는 4)
            if (e_flag) current_inst_length = 4; // 4형식
            else current_inst_length = 3; // 3형식
        } else {
            throw new IllegalStateException("Unknown instruction format: " + instruction.getFormat());
        }
    }

    // 각 명령어별 실행 메소드
    private void executeSTL(int pc) {
        // STL m : opcode 14 : m..m+2 <- (L)

        // m..m+2 구하기
        int target_address = getTargetAddress(pc); // n과 i가 둘 다 0 혹은 1 인 경우의 처리

        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }
        if (i_flag) { // immediate 호출
            target_address = get_immediate_value(pc);
        }

        // (L) 구하기
        int l = resourceManager.getRegister("L");
        // 값이 0x3000 이었다면 String 003000으로 바꾸기
        String l_String = String.format("%06X", l).toUpperCase();

        // m..m+2 <- (L)
        resourceManager.loadMemory(target_address, l_String.substring(0, 2));
        resourceManager.loadMemory(target_address + 1, l_String.substring(2, 4));
        resourceManager.loadMemory(target_address + 2, l_String.substring(4, 6));

        // 다음 명령어로 가기 위해 PC + length 세팅
        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    private void executeJSUB(int pc) {
        // JSUB m : opcode 48 : L <- (PC); PC <- (m)

        // L <- (PC) : 현재 명령어가 PC 값이고... 돌아왔을때 가는 곳은 PC + length으로 설정
        resourceManager.setRegister("L", pc + current_inst_length);

        // (m) 구하기
        int target_address = getTargetAddress(pc);
        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }
        if (i_flag) { // immediate 호출
            target_address = get_immediate_value(pc);
        }

        // PC <- (m)
        // 다음 명령어 위치 세팅
        resourceManager.setRegister("PC", target_address);
    }

    private void executeLDA(int pc) {
        // LDA m : opcode 00 : A <- (m..m+2)

        // m..m+2
        int target_address = getTargetAddress(pc);
        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }

        // (m..m+2)
        int value = Integer.parseInt(
                resourceManager.readMemory(target_address)
                        + resourceManager.readMemory(target_address + 1)
                        + resourceManager.readMemory(target_address + 2)
                , 16);

        if (i_flag) {
            value = get_immediate_value(pc);
        }

        // A <- (m..m+2)
        resourceManager.setRegister("A", value);

        // 다음 명령어로 가기 위해 PC + length 세팅
        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    private void executeCOMP(int pc) {
        // COMP m : opcode 28 : (A) 비교 (m..m+2) - set SW

        // m..m+2
        int target_address = getTargetAddress(pc);
        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }

        // (A)
        int a = resourceManager.getRegister("A");
        // (m..m+2)
        int value = Integer.parseInt(
                resourceManager.readMemory(target_address)
                        + resourceManager.readMemory(target_address + 1)
                        + resourceManager.readMemory(target_address + 2)
                , 16);

        if (i_flag) {
            value = get_immediate_value(pc);
        }

        // set SW
        // < 0x 0001
        // = 0x 0000
        // > 0x 0010
        int sw;
        if (a < value) {
            sw = 0x01;
        } else if (a == value) {
            sw = 0x00;
        } else {
            sw = 0x10;
        }

        resourceManager.setRegister("SW", sw);
        resourceManager.setRegister("PC", pc + current_inst_length); // 다음 명령어로 가기 위해 PC + length 세팅
    }

    private void executeJEQ(int pc) {
        // JEQ m : opcode 30 : PC <- m if SW(CC) set to =

        // m
        int target_address = getTargetAddress(pc);
        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }
        if (i_flag) { // immediate 호출
            target_address = get_immediate_value(pc);
        }

        if (resourceManager.getRegister("SW") == 0x00) { // if SW(CC) set to =
            // PC <- m
            resourceManager.setRegister("PC", target_address);
        } else {
            // 다음 명령어로 가기 위해 PC + length 세팅
            resourceManager.setRegister("PC", pc + current_inst_length);
        }
    }

    private void executeJ(int pc) {
        // J m : opcode 3C : PC <- m

        // m
        int target_address = getTargetAddress(pc);
        if (n_flag) { // i_flag 라면 indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }
        if (i_flag) { // immediate 호출
            target_address = get_immediate_value(pc);
        }

        resourceManager.setRegister("PC", target_address);
    }

    private void executeSTA(int pc) {
        // STA m : opcode 0C : m..m+2 <- (A)
        int target_address = getTargetAddress(pc);

        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }
        if (i_flag) { // immediate 호출
            target_address = get_immediate_value(pc);
        }

        int a = resourceManager.getRegister("A");
        String a_String = String.format("%06X", a).toUpperCase();

        resourceManager.loadMemory(target_address, a_String.substring(0, 2));
        resourceManager.loadMemory(target_address + 1, a_String.substring(2, 4));
        resourceManager.loadMemory(target_address + 2, a_String.substring(4, 6));

        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    private void executeCLEAR(int pc) {
        // CLEAR r1 : opcode B4 : r1 <- 0
        int r1 = (getRegisterNumber(pc + 1) & 0xF0) >> 4; // B4 10 : 0001 0000
        resourceManager.setRegister(getRegisterName(r1), 0);

        resourceManager.setRegister("PC", pc + current_inst_length); // 다음 명령어 위치 세팅
    }

    private void executeLDT(int pc) {
        // LDT m : opcode 74 : T <- (m..m+2)

        // m..m+2
        int target_address = getTargetAddress(pc);
        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }

        // (m..m+2)
        int value = Integer.parseInt(
                resourceManager.readMemory(target_address) +
                        resourceManager.readMemory(target_address + 1) +
                        resourceManager.readMemory(target_address + 2)
                , 16);

        if (i_flag) {
            value = get_immediate_value(pc);
        }

        // T <- (m..m+2)
        resourceManager.setRegister("T", value);

        // 다음 명령어로 가기 위해 PC + length 세팅
        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    private void executeTD(int pc) {
        // TD m : opcode E0 : Test device (m) - set CC
        int target_address = getTargetAddress(pc);

        String deviceName = resourceManager.readMemory(target_address);
        Device device = resourceManager.getDevice(deviceName);
        if (device != null) {
            resourceManager.setDevice(deviceName);
            resourceManager.setRegister("SW", 0x01); // Device ready
        } else {
            resourceManager.setRegister("SW", 0x00); // Device not ready
            // 만약 임의 설정 오류시 여기서 무한루프...
            // throw new RuntimeException("Device not found");
        }

        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    private void executeRD(int pc) {
        // RD m : opcode D8 : A[rightmost byte] <- data from device (m)
        // RD INPUT DB 20 15 : 1101 10 11 0010 0000 0001 0101
        int target_address = getTargetAddress(pc);

        String deviceName = resourceManager.readMemory(target_address);
        Device device = resourceManager.getDevice(deviceName);
        if (device != null) {
            byte data = device.read();
            int value = (resourceManager.getRegister("A") & 0xFFFF00) | (data & 0xFF);
            resourceManager.setRegister("A", value);
        }
        else {
            throw new RuntimeException("device not found : " + deviceName);
        }

        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    private void executeCOMPR(int pc) {
        // COMPR r1,r2 : opcode A0 : (r1) 비교 (r2) - set CC
        // COMPR r1,r2 : opcode A0 : (r1) 비교 (r2) - set CC
        int r1 = (getRegisterNumber(pc + 1) & 0xF0) >> 4;
        int r2 = getRegisterNumber(pc + 1) & 0x0F;

        int sw = resourceManager.getRegister(getRegisterName(r1)) - resourceManager.getRegister(getRegisterName(r2));

        if (sw < 0) {
            sw = 0x01;
        } else if (sw == 0) {
            sw = 0x00;
        } else {
            sw = 0x10;
        }

        resourceManager.setRegister("SW", sw);

        resourceManager.setRegister("PC", pc + current_inst_length); // 다음 명령어 위치 세팅
    }

    private void executeSTCH(int pc) {
        // STCH m : opcode 54 : m <- (A)[rightmost byte]
        int target_address = getTargetAddress(pc);

        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }
        if (i_flag) { // immediate 호출
            target_address = get_immediate_value(pc);
        }

        int a = resourceManager.getRegister("A") & 0xFF; // 하위 1바이트
        resourceManager.loadMemory(target_address, String.format("%02X", a).toUpperCase());

        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    private void executeTIXR(int pc) {
        // TIXR r1 : opcode 2C : X <- (X) + 1; (X) 비교 (r1) - set CC
        int r1 = (getRegisterNumber(pc + 1) & 0xF0) >> 4;
        int x = resourceManager.getRegister("X") + 1;
        resourceManager.setRegister("X", x);

        // System.out.println("X value : " + x); // 디버그 용도
        int sw = resourceManager.getRegister(getRegisterName(r1)) - x;
        // T - x : T 1000 : x 0 : x가 더 작음 : 양수
        // x가 더 큼 : 음수
        if (sw < 0) {
            sw = 0x10;
        } else if (sw == 0) {
            sw = 0x00;
        } else {
            sw = 0x01;
        }
        resourceManager.setRegister("SW", sw);

        resourceManager.setRegister("PC", pc + current_inst_length); // 다음 명령어 위치 세팅
    }

    private void executeJLT(int pc) {
        // JLT m : opcode 38 : PC <- m if SW(CC) set to <
        if (resourceManager.getRegister("SW") == 0x01) {
            int target_address = getTargetAddress(pc);

            if (n_flag) { // indirect 호출
                target_address = get_indirect_TargetAddress(target_address);
            }
            if (i_flag) { // immediate 호출
                target_address = get_immediate_value(pc);
            }
            resourceManager.setRegister("PC", target_address);
        } else {
            resourceManager.setRegister("PC", pc + current_inst_length);
        }
    }

    private void executeSTX(int pc) {
        // STX m : opcode 10 : m..m+2 <- (X)
        int target_address = getTargetAddress(pc);

        if (n_flag) { // indirect 호출
            target_address = get_indirect_TargetAddress(target_address);
        }
        if (i_flag) { // immediate 호출
            target_address = get_immediate_value(pc);
        }

        int x = resourceManager.getRegister("X");
        String x_String = String.format("%06X", x).toUpperCase();

        resourceManager.loadMemory(target_address, x_String.substring(0, 2));
        resourceManager.loadMemory(target_address + 1, x_String.substring(2, 4));
        resourceManager.loadMemory(target_address + 2, x_String.substring(4, 6));

        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    private void executeRSUB(int pc) {
        // RSUB : opcode 4C : PC <- (L)
        resourceManager.setRegister("PC", resourceManager.getRegister("L"));
    }
    private void executeLDCH(int pc) {
        // A[rightmost byte] <- (m)
        int target_address = getTargetAddress(pc);

        if (n_flag) {
            target_address = get_indirect_TargetAddress(target_address);
        }

        int value = Integer.parseInt(resourceManager.readMemory(target_address), 16);

        resourceManager.setRegister("A", (resourceManager.getRegister("A") & 0xFFFF00) | value);
        resourceManager.setRegister("PC", pc + current_inst_length);
    }
    private void executeWD(int pc) {
        // Device specified by (m) <- (A)[rightmost byte]


        int target_address = getTargetAddress(pc);

        String deviceName = resourceManager.readMemory(target_address);
        Device device = resourceManager.getDevice(deviceName);
        if (device != null) {
            int value = resourceManager.getRegister("A") & 0xFF;
            device.write((byte) value);
        }
        else {
            throw new RuntimeException("device not found : " + deviceName);
        }

        resourceManager.setRegister("PC", pc + current_inst_length);
    }

    // 유틸리티 메서드
    private int getRegisterNumber(int address) {
        return Integer.parseInt(resourceManager.readMemory(address), 16);
    }

    private String getRegisterName(int number) {
        switch (number) {
            case 0: return "A";
            case 1: return "X";
            case 2: return "L";
            case 3: return "B";
            case 4: return "S";
            case 5: return "T";
            case 6: return "F";
            case 8: return "PC";
            case 9: return "SW";
            default: throw new IllegalArgumentException("Invalid register number: " + number);
        }
    }

    private int getTargetAddress(int pc) { // 3, 4 형식의 target 을 계산하여 반환
        int displacement = Integer.parseInt(
                resourceManager.readMemory(pc + 1) + resourceManager.readMemory(pc + 2)
                , 16) & 0xFFF; // 16진수로 저장하고 disp 영역만 남기기 ___FFF
        if (e_flag) { // 4형식일 경우 1바이트 더 필요
            displacement = (displacement << 8) | Integer.parseInt(resourceManager.readMemory(pc + 3), 16);
        }

        int target_address = displacement;

        if (!e_flag) { // 3형식의 경우 B 또는 PC 상대 주소 판단
            if (p_flag) { // pc relative
                // System.out.println("disp : " + Integer.toHexString(displacement)); // 디버그 용도
                target_address = (pc + displacement);
                target_address += 3; // TA 계산시의 PC는 다음 명령어 시작주소
                if ( ((displacement + ((pc + 3) & 0xFFF) ) & 0x1000 ) != 0) { // carry 발생
                    target_address -= 0x1000;
                }
                // System.out.println("TA : " + Integer.toHexString(target_address)); // 디버그 용도

                // 본 구현에서는 명령어 실행이 끝난 후 PC에 현재 길이를 더해서 다음 명령어 시작주소를 가리키도록 했기 때문에
                // 강제로 +3 해줘야 했다.
            } else if (b_flag) { // base relative : COPY 에서는 쓰일 일이 없긴 함
                int base = resourceManager.getRegister("B");
                target_address = (base + displacement);
                // System.out.println("TA : " + Integer.toHexString(target_address)); // 디버그 용도
            }
        }

        if (x_flag) { // index가 사용되면 X 레지스터 값을 더한다
            target_address += resourceManager.getRegister("X");
            // System.out.println("TA : " + Integer.toHexString(target_address)); // 디버그 용도
        }

        return target_address;
    }

    private int get_indirect_TargetAddress(int address) { // indirect 라면 호출 할 것
        int target_address = Integer.parseInt(
                resourceManager.readMemory(address)
                        + resourceManager.readMemory(address + 1)
                        + resourceManager.readMemory(address + 2)
                , 16); // 16진수로 가지고 오기
        return target_address;
    }

    private int get_immediate_value(int pc) { // immediate 라면 호출 할 것
        // pc pc+1 pc+2
        // ___FFF disp 가져오기
        int value = Integer.parseInt(
                resourceManager.readMemory(pc+1)
                        + resourceManager.readMemory(pc+2)
                , 16) & 0xFFF; // 16진수로 가지고 오기
        return value;
    }
}