import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class VisualSimulator extends JFrame {
    private SICXESimulator sicxeSimulator;

    private JButton btnOpen;
    private JButton btnRunStep;
    private JButton btnRunAll;
    private JButton btnExit;
    private JTextField fileNameField;
    private JTextArea programInfoArea;
    private JTextArea endRecordArea;
    private JTextArea logArea;
    private JTextArea deviceArea;

    // 레지스터 표시용 JTextField
    private JTextField regADec;
    private JTextField regAHex;
    private JTextField regXDec;
    private JTextField regXHex;
    private JTextField regLDec;
    private JTextField regLHex;
    private JTextField regBDec;
    private JTextField regBHex;
    private JTextField regSDec;
    private JTextField regSHex;
    private JTextField regTDec;
    private JTextField regTHex;
    private JTextField regFDec;
    private JTextField regFHex;
    private JTextField regPCDec;
    private JTextField regPCHex;
    private JTextField regSWDec;
    private JTextField regSWHex;
    // 메모리 필드
    private JTextArea memoryArea;
    private Highlighter highlighter;
    private Highlighter.HighlightPainter painter;

    public VisualSimulator() {
        sicxeSimulator = new SICXESimulator(this); // 시뮬레이터
        initializeGUI(); // GUI 업데이트
        setupEventListeners(); // 버튼 셋업
    }

    private void initializeGUI() {
        // GUI 구성 요소 초기화 (버튼, 텍스트 필드 등)
        setTitle("SIC/XE Simulator");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 상단 패널 (File Open, ProgramInfo : H, E)
        JPanel panelTop = new JPanel();
        panelTop.setLayout(new BorderLayout());

        JPanel panelFile = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileNameField = new JTextField("", 20);
        btnOpen = new JButton("Open");
        panelFile.add(new JLabel("FileName: "));
        panelFile.add(fileNameField);
        panelFile.add(btnOpen);

        JPanel panelProgramInfo = new JPanel(new GridLayout(1, 2));
        programInfoArea = new JTextArea(3, 20);
        programInfoArea.setBorder(BorderFactory.createTitledBorder("H (Header Record)"));
        programInfoArea.setEditable(false);

        endRecordArea = new JTextArea(3,20);
        endRecordArea.setBorder(BorderFactory.createTitledBorder("E (End Record)"));
        endRecordArea.setEditable(false);
        panelProgramInfo.add(new JScrollPane(programInfoArea));
        panelProgramInfo.add(new JScrollPane(endRecordArea));

        panelTop.add(panelFile, BorderLayout.NORTH);
        panelTop.add(panelProgramInfo, BorderLayout.CENTER);

        // 레지스터 패널
        JPanel panelRegisters = new JPanel(new GridLayout(10, 3));
        panelRegisters.setBorder(BorderFactory.createTitledBorder("Registers"));

        regADec = new JTextField("0", 10);
        regAHex = new JTextField("0", 10);
        regXDec = new JTextField("0", 10);
        regXHex = new JTextField("0", 10);
        regLDec = new JTextField("0", 10);
        regLHex = new JTextField("0", 10);
        regBDec = new JTextField("0", 10);
        regBHex = new JTextField("0", 10);
        regSDec = new JTextField("0", 10);
        regSHex = new JTextField("0", 10);
        regTDec = new JTextField("0", 10);
        regTHex = new JTextField("0", 10);
        regFDec = new JTextField("0", 10);
        regFHex = new JTextField("0", 10);
        regPCDec = new JTextField("0", 10);
        regPCHex = new JTextField("0", 10);
        regSWDec = new JTextField("0", 10);
        regSWHex = new JTextField("0", 10);

        panelRegisters.add(new JLabel("Reg"));
        panelRegisters.add(new JLabel("Dec"));
        panelRegisters.add(new JLabel("Hex"));
        panelRegisters.add(new JLabel("A (#0):"));
        panelRegisters.add(regADec);
        panelRegisters.add(regAHex);
        panelRegisters.add(new JLabel("X (#1):"));
        panelRegisters.add(regXDec);
        panelRegisters.add(regXHex);
        panelRegisters.add(new JLabel("L (#2):"));
        panelRegisters.add(regLDec);
        panelRegisters.add(regLHex);
        panelRegisters.add(new JLabel("B (#3):"));
        panelRegisters.add(regBDec);
        panelRegisters.add(regBHex);
        panelRegisters.add(new JLabel("S (#4):"));
        panelRegisters.add(regSDec);
        panelRegisters.add(regSHex);
        panelRegisters.add(new JLabel("T (#5):"));
        panelRegisters.add(regTDec);
        panelRegisters.add(regTHex);
        panelRegisters.add(new JLabel("F (#6):"));
        panelRegisters.add(regFDec);
        panelRegisters.add(regFHex);
        panelRegisters.add(new JLabel("PC (#8):"));
        panelRegisters.add(regPCDec);
        panelRegisters.add(regPCHex);
        panelRegisters.add(new JLabel("SW (#9):"));
        panelRegisters.add(regSWDec);
        panelRegisters.add(regSWHex);

        // 메모리 패널
        JPanel panelMemory = new JPanel(new BorderLayout());

        memoryArea = new JTextArea();
        memoryArea.setBorder(BorderFactory.createTitledBorder("Memory"));
        memoryArea.setEditable(false);
        panelMemory.add(new JScrollPane(memoryArea), BorderLayout.CENTER);

        highlighter = memoryArea.getHighlighter();
        painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        // 하단 패널 (명령 실행, 로그)
        JPanel panelBottom = new JPanel(new BorderLayout());

        // 실행 버튼
        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRunStep = new JButton("Run Step");
        btnRunAll = new JButton("Run All");
        btnExit = new JButton("Exit");
        panelButtons.add(btnRunStep);
        panelButtons.add(btnRunAll);
        panelButtons.add(btnExit);

        JPanel panelLogAndDevice = new JPanel(new GridLayout(1, 2));
        logArea = new JTextArea(5,20);
        logArea.setBorder(BorderFactory.createTitledBorder("Log (명령어 수행 관련)"));
        logArea.setEditable(false);
        panelLogAndDevice.add(new JScrollPane(logArea));

        deviceArea = new JTextArea(5,20);
        deviceArea.setBorder(BorderFactory.createTitledBorder("Devices"));
        deviceArea.setEditable(false);
        panelLogAndDevice.add(new JScrollPane(deviceArea));

        panelBottom.add(panelButtons, BorderLayout.NORTH);
        panelBottom.add(panelLogAndDevice, BorderLayout.CENTER);

        // 패널들을 프레임에 추가
        add(panelTop, BorderLayout.NORTH);
        add(panelRegisters, BorderLayout.WEST);
        add(panelMemory, BorderLayout.CENTER);
        add(panelBottom, BorderLayout.SOUTH);
    }

    private void setupEventListeners() {
        btnOpen.addActionListener(new ActionListener() { // 파일 오픈 버튼
            public void actionPerformed(ActionEvent arg0) {
                JFileChooser chooser = new JFileChooser();
                int ret = chooser.showOpenDialog(null);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    try {
                        File file = chooser.getSelectedFile();
                        sicxeSimulator.loadObjectCode(file); // 로더 호출
                        fileNameField.setText(file.getName()); // 파일 이름을 JTextField 에 설정
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        btnRunStep.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                sicxeSimulator.executeNextInstruction(); // oneStep
                updateGUI();
            }
        });

        btnRunAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                sicxeSimulator.executeAllInstructions(); // allStep
                updateGUI();
            }
        });

        btnExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) { // Exit
                System.exit(0);
            }
        });
    }

    public void updateGUI() {
        // 리소스 매니저로부터 값을 받아와 GUI 업데이트
        ResourceManager rm = sicxeSimulator.getResourceManager();

        // 레지스터 업데이트
        regADec.setText(String.valueOf(rm.getRegister("A")));
        regAHex.setText(Integer.toHexString(rm.getRegister("A")));
        regXDec.setText(String.valueOf(rm.getRegister("X")));
        regXHex.setText(Integer.toHexString(rm.getRegister("X")));
        regLDec.setText(String.valueOf(rm.getRegister("L")));
        regLHex.setText(Integer.toHexString(rm.getRegister("L")));
        regBDec.setText(String.valueOf(rm.getRegister("B")));
        regBHex.setText(Integer.toHexString(rm.getRegister("B")));
        regSDec.setText(String.valueOf(rm.getRegister("S")));
        regSHex.setText(Integer.toHexString(rm.getRegister("S")));
        regTDec.setText(String.valueOf(rm.getRegister("T")));
        regTHex.setText(Integer.toHexString(rm.getRegister("T")));
        regFDec.setText(String.valueOf(rm.getRegister("F")));
        regFHex.setText(Integer.toHexString(rm.getRegister("F")));
        regPCDec.setText(String.valueOf(rm.getRegister("PC")));
        regPCHex.setText(Integer.toHexString(rm.getRegister("PC")));
        regSWDec.setText(String.valueOf(rm.getRegister("SW")));
        regSWHex.setText(Integer.toHexString(rm.getRegister("SW")));

        // 메모리 업데이트
        memoryArea.setText(rm.getMemoryStatus());

        // 현재 실행 중인 명령어 강조 표시
        int pc = rm.getRegister("PC");
        int instructionLength = calculateInstructionLength(pc, rm);
        highlightCurrentInstruction(pc, instructionLength);

        // 프로그램 정보 업데이트
        programInfoArea.setText(rm.getProgramInfo());
        endRecordArea.setText(rm.getEndRecordInfo());

        // 로그 업데이트
        logArea.setText(rm.getLog());

        // 디바이스 업데이트
        deviceArea.setText(rm.getDeviceStatus());
    }

    private int calculateInstructionLength(int pc, ResourceManager rm) {
        // 현재 PC의 opcode 와 e bit 를 기준으로 명령어의 형태를 판단하여 명령어 길이를 리턴한다.
        // InstructionSet을 import 해서 사용해도 되지만... 귀찮아서 그냥 판단로직을 작성했다.
        int opcode = Integer.parseInt(rm.readMemory(pc), 16) & 0xFC;

        // 2형식 명령어 리스트
        int[] format2Opcodes = {0xB4, 0xA0, 0xB8}; // CLEAR, COMPR, TIXR

        for (int format2Opcode : format2Opcodes) {
            if (opcode == format2Opcode) {
                return 2;
            }
        }

        // 3/4형식 명령어 처리
        int xbpe = Integer.parseInt(rm.readMemory(pc + 1), 16) & 0xF0;
        boolean isFormat4 = (xbpe & 0x10) != 0;
        if (isFormat4) {
            return 4;
        } else {
            return 3;
        }
    }

    private void highlightCurrentInstruction(int address, int instructionLength) {
        try {
            highlighter.removeAllHighlights();

            // 바이트의 위치를 직접 계산
            int lineIndex = (address / 16) * (48 + 9); // 라인 건너뛰기
            int start = (lineIndex + 8) + ((address % 16) * 3); // 해당 라인에서의 시작점 계산

            int index = address % 16; // 몇번째인지 기록
            int iterate = instructionLength; // 남은 반복 횟수

            int end = start;

            for (int i = 0; i < instructionLength; i++) { // 0 1 2 : iterate 3
                end += 3; // 각 바이트는 2자리 16진수, 한 칸 공백 있으니까 크기가 3
                index++;
                iterate--;
                if (index == 16) {
                    break;
                }
            }
            highlighter.addHighlight(start, end-1, painter); // end-1로 공백 빼기

            if (iterate > 0) { // 개수가 남아있음
                lineIndex = ((address / 16) + 1) * (48 + 9);
                start = lineIndex + 8; // 다음 라인에서의 시작점 계산

                end = start;
                for (int i = 0; i < iterate; i++) {
                    end += 3; // 각 바이트는 2자리 16진수, 한 칸 공백 있으니까 크기가 3
                }
                highlighter.addHighlight(start, end-1, painter); // end-1로 공백 빼기
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
