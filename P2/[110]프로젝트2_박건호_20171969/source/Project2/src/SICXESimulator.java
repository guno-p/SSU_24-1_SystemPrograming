import java.io.File;

public class SICXESimulator {
    private ResourceManager resourceManager;
    private ObjectCodeLoader objectCodeLoader;
    private InstructionExecutor instructionExecutor;
    private VisualSimulator visualSimulator;

    public SICXESimulator(VisualSimulator visualSimulator) {
        this.visualSimulator = visualSimulator;
        this.resourceManager = new ResourceManager();
        this.objectCodeLoader = new ObjectCodeLoader(resourceManager); // 인스턴스 공유
        // 로더에서 로드 끝내고 PC 를 첫 시작 address 로 설정하였음
        this.instructionExecutor = new InstructionExecutor(resourceManager); // 인스턴스 공유
    }

    public void loadObjectCode(File file) {
        objectCodeLoader.load(file);
        // 로더 작업을 지시 -> file open 버튼에서 호출 된다.
        visualSimulator.updateGUI(); // GUI 업데이트
    }

    public void executeNextInstruction() {
        instructionExecutor.executeNext();
        visualSimulator.updateGUI();
    }

    public void executeAllInstructions() {
        while (!resourceManager.isEndOfProgram()) {
            // END 검사 : RSUB 하여 COPY를 호출한 곳으로 돌아가는 것을 임의로 0x3000 으로 설정하였다.
            // (저장된 돌아갈 곳이 0x3000 : JSUB한 곳의 주소가 0x3000이라는게 아님)
            instructionExecutor.executeNext();
        }
        visualSimulator.updateGUI();
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }
}