import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ObjectCodeLoader {
    private ResourceManager resourceManager;

    public ObjectCodeLoader(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void load(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            pass1(br);
            br.close(); // Close and reopen to reset BufferedReader
            pass2(new BufferedReader(new FileReader(file)));

            // 시작 address를 PC로 설정하여 명령어 판단 시작
            resourceManager.setRegister("PC", resourceManager.getStart_address());
            // 임시로.... L 레지스터 값을 0x3000으로 설정 - return to caller
            resourceManager.setRegister("L", 0x3000);

            // resourceManager.printSymbolTable(); // 디버그 용도 - 심볼테이블 확인용
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pass1(BufferedReader br) throws IOException {
        String line;
        String current_sect_name = "";
        int cnt_H = 0;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("H")) {
                int startAddress;
                if (cnt_H == 0) {
                    current_sect_name = line.substring(1, 7).trim();
                    startAddress = 0;
                    startAddress += Integer.parseInt(line.substring(7, 13), 16);
                    resourceManager.setStart_address(startAddress); // startaddress setting
                    resourceManager.setProg_name(current_sect_name);
                    cnt_H++;
                }
                else { // H 레코드가 이미 나왔어서 2, 3번째 section임
                    startAddress = resourceManager.getSymbol(current_sect_name).getAddress();
                    startAddress += resourceManager.getSymbol(current_sect_name).getLength();
                    startAddress += Integer.parseInt(line.substring(7, 13), 16);
                    current_sect_name = line.substring(1, 7).trim();
                    cnt_H++; // 필요는 없지만... 일관성 있게
                }
                int length = Integer.parseInt(line.substring(13, 19), 16);
                resourceManager.addSymbol(current_sect_name, "-", startAddress, length);
            } else if (line.startsWith("D")) {
                for (int i = 1; i < line.length(); i += 12) {
                    String symbolName = line.substring(i, i + 6).trim();
                    int address = resourceManager.getSymbol(current_sect_name).getAddress();
                    // 현재 섹션의 시작주소
                    address += Integer.parseInt(line.substring(i + 6, i + 12), 16);
                    // D 레코드에서 표기된 주소 : 16진수로 변환

                    if (current_sect_name == "") {
                        // D 레코드 이전에 H가 등장하지 않음 오류 current section 모름
                        throw new RuntimeException("H record not found before D record : Current Section setting error");
                    }

                    resourceManager.addSymbol(symbolName, current_sect_name, address, -1);
                }
            } else if (line.startsWith("E")) {
                // End record 처리: 첫 명령어 주소 설정
                if (line.length() == 1) continue; // 내용 없으면 생략 ex) E
                // 내용 있으면 E 000000 : 000000을 리소스 매니저를 통해 넣기
                int firstInstructionAddress = Integer.parseInt(line.substring(1, 7).trim(), 16);
                resourceManager.setFirst_inst_address(firstInstructionAddress);
            }
        }
        int prog_lennth = 0;
        prog_lennth += resourceManager.getSymbol(current_sect_name).getAddress();
        prog_lennth += resourceManager.getSymbol(current_sect_name).getLength();
        prog_lennth -= resourceManager.getStart_address();
        resourceManager.setProg_length(prog_lennth);
    }

    private void pass2(BufferedReader br) throws IOException {
        String line;
        String current_sect_name;
        int current_sect_address = 0;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("H")) {
                current_sect_name = line.substring(1, 7).trim();
                current_sect_address = resourceManager.getSymbol(current_sect_name).getAddress();
                // 현재 섹션의 시작 주소를 저장
            }
            else if (line.startsWith("T")) {
                int startAddress = Integer.parseInt(line.substring(1, 7), 16);
                startAddress += current_sect_address; // 현재 섹션의 시작 주소를 추가

                int length = Integer.parseInt(line.substring(7, 9), 16); // 길이

                for (int i = 9; i < 9 + length * 2; i += 2) {
                    String byteString = line.substring(i, i + 2); // 2글자씩 판단
                    resourceManager.loadMemory(startAddress++, byteString);
                }
            }
            else if (line.startsWith("M")) {

                int modify_address = Integer.parseInt(line.substring(1, 7), 16);
                modify_address += current_sect_address;

                int length = Integer.parseInt(line.substring(7, 9), 16);
                // 05 혹은 06
                // length가 05라면?
                // byte[0004] = 0000 0000 여기의 half 부터...
                // byte[0005] = 0000 0000
                // byte[0006] = 0000 0000
                // length가 06라면?
                // 3바이트 전부 다...
                // 근데 둘 다 그냥 수정 해도 될거같기도 한데... 혹시 모르니
                String symbol_name = line.substring(10).trim();

                boolean isAddition = line.charAt(9) == '+';

                resourceManager.modifyMemory(modify_address, length, symbol_name, isAddition);
                // int address / int length / String label / is addition
            }
        }
    }
}