/**
 * @author Enoch Jung (github.com/enochjung)
 * @file Assembler.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import instruction.InstructionTable;

/**
 * SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 
 * 작성 중 유의 사항
 * 1) Assembler.java 파일의 기존 코드를 변경하지 말 것. 다른 소스 코드의 구조는 본인의 편의에 따라 변경해도 됨.
 * 2) 새로운 클래스, 새로운 필드, 새로운 메소드 선언은 허용됨. 단, 기존의 필드와 메소드를 삭제하거나 대체하는 것은 불가함.
 * 3) 예외 처리, 인터페이스, 상속 사용 또한 허용됨.
 * 4) 파일, 또는 콘솔창에 한글을 출력하지 말 것. (채점 상의 이유)
 * 
 * 제공하는 프로그램 구조의 개선점을 제안하고 싶은 학생은 보고서의 결론 뒷부분에 첨부 바람. 내용에 따라 가산점이 있을 수 있음.
 *
 * 필드와 메소드를 삭제 혹은 대체하지 않는 선에서 변경하여도 됨'의 의미는
 * 주어진 필드와 메소드를 적극적으로 사용하되, 이들의 역할을 바꾸지 말아 달라는 의미입니다.
 * 기준이 상당히 주관적이므로 적절히 판단해 주시길 바랍니다.
 *
 * 심볼 테이블 및 리터럴 테이블 출력에서 각 요소 출력 순서는 상관없습니다.
 * 로더가 같은 동작을 수행함을 보장한다면 오브젝트 코드의 출력 순서도 상관없습니다..만
 * 보장하지 않는 경우가 대부분이므로, 오브젝트 코드는 출력 예시에 맞추는 것을 권장합니다.
 *
 * - 주어진 java 파일을 참고하여 과제 코드를 구현할 것
 * - ControlSection.java 파일에서 각 control section에 해당하는 소스 코드가 독립적으로 컴파일됨.
 *   독립적인 컴파일을 보장하기 위하여, Assembler.java 에서 각 control section 소스 코드에 END directive를 추가하였음
 * - Exception Handling을 통해 SIC/XE 소스 코드의 문법적 오류를 탐지할 수 있도록 구현할 것.
 *   최소한 exception handling을 통해 정의되지 않은 심볼 사용 오류를 감지하여야 하며, 다른 오류 감지를 추가적으로 수행할 경우 가산점이 주어질 수 있음
 * - EXTREF에서 선언된 심볼 또한 심볼 테이블에서 출력할 것
 * - Assembler.java를 제외한 파일은 필드와 메소드를 삭제 혹은 대체하지 않는 선에서 변경하여도 됨.
 *   기존 작성된 코드는 가이드라인을 제공하기 위해 작성된 것이기에, 해당 형식을 따르는 것을 권장함.
 * - 제공된 코드의 개선점을 제안하고 싶은 경우, 보고서의 결론 뒷부분에 첨부 바람. 내용에 따라 가산점이 주어질 수 있음
 */
public class Assembler {
	public static void main(String[] args) {
		try {
			Assembler assembler = new Assembler("inst_table.txt");
			ArrayList<String> input = assembler.readInputFromFile("input.txt");
			// 현재 input 리스트의 각 요스는 소스코드 한 라인
			ArrayList<ArrayList<String>> dividedInput = assembler.divideInput(input);
			// divided SECTion 별로 저장
			// dividedInput.size(); - 딱히 쓸일이 없는거 같음

			ArrayList<ControlSection> controlSections = (ArrayList<ControlSection>) dividedInput.stream()
					.map(x -> assembler.pass1(x))
					.collect(Collectors.toList());
			// 패스 1 수행

			String symbolsString = controlSections.stream()
					.map(x -> x.getSymbolString())
					.collect(Collectors.joining("\n\n"));

			String literalsString = controlSections.stream()
					.map(x -> x.getLiteralString())
					.collect(Collectors.joining("\n\n"));

			assembler.writeStringToFile("output_symtab.txt", symbolsString);
			assembler.writeStringToFile("output_littab.txt", literalsString);
			// 심볼테이블, 리터럴테이블 출력

			ArrayList<ObjectCode> objectCodes = (ArrayList<ObjectCode>) controlSections.stream()
					.map(x -> assembler.pass2(x))
					.collect(Collectors.toList());
			// 패스 2 수행

			String objectCodesString = objectCodes.stream()
					.map(x -> x.toString())
					.collect(Collectors.joining("\n\n"));

			assembler.writeStringToFile("output_objectcode.txt", objectCodesString);
			// 오브젝트 코드 출력
		} catch (Exception e) {
			System.out.println("Error : " + e.getMessage());
		}

	}

	public Assembler(String instFile) throws FileNotFoundException, IOException {
		_instTable = new InstructionTable(instFile);
		// Assembler.java 생성하면서 기계어 명령어 테이블 목록 만든다
	}

	private ArrayList<ArrayList<String>> divideInput(ArrayList<String> input) {
		ArrayList<ArrayList<String>> divided = new ArrayList<ArrayList<String>>();
		String lastStr = input.get(input.size() - 1);

		ArrayList<String> tmpInput = new ArrayList<String>();
		for (String str : input) { // 한 라인씩 검사 수행
			if (str.contains("CSECT")) { // 라인에 CSECT가 있다면
				if (!tmpInput.isEmpty()) { // tmpInput이 비어있지 않은 경우
					tmpInput.add(lastStr); // END FIRST 삽입!! -> 나중에 심볼테이블에서 확인 가능
					divided.add(tmpInput);
					tmpInput = new ArrayList<String>();
					tmpInput.add(str);
				}
			} else {
				tmpInput.add(str);
			}
		}

		if (!tmpInput.isEmpty()) {
			divided.add(tmpInput);
		}

		return divided; // 섹션별로 독립적인 컴파일 위해 나누었음
	}

	private ArrayList<String> readInputFromFile(String inputFileName) throws FileNotFoundException, IOException {
		ArrayList<String> input = new ArrayList<String>();

		File file = new File(inputFileName);
		BufferedReader bufReader = new BufferedReader(new FileReader(file));

		String line = "";
		while ((line = bufReader.readLine()) != null) // 한 라인을 읽어서 null이 아니라면 line으로 읽어오기
			input.add(line); // input에 line을 추가

		bufReader.close();

		return input;
	}

	private void writeStringToFile(String fileName, String content) throws IOException {
		File file = new File(fileName);

		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(content);
		writer.close();
	}

	private ControlSection pass1(ArrayList<String> input) throws RuntimeException {
		return new ControlSection(_instTable, input); // inst_table , input을 받아서 pass1 수행
	}

	private ObjectCode pass2(ControlSection controlSection) throws RuntimeException {
		return controlSection.buildObjectCode(); // PASS2 수행
	}

	private InstructionTable _instTable;
}