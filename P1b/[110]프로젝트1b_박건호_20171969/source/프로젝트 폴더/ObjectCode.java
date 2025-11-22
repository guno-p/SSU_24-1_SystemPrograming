/**
 * @author Enoch Jung (github.com/enochjung)
 * @file ObjectCode.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

import java.util.Optional;
import java.util.ArrayList;

import numeric.Numeric;

public class ObjectCode {
	public ObjectCode() {
		_sectionName = Optional.empty();
		_startAddress = Optional.empty();
		_programLength = Optional.empty();
		_initialPC = Optional.empty();

		_defines = new ArrayList<Define>();
		_refers = new ArrayList<String>();
		_texts = new ArrayList<Text>();
		_mods = new ArrayList<Modification>();
	}

	/**
	 * ObjectCode 객체를 String으로 변환한다. Assembler.java에서 오브젝트 코드를 출력하는 데에 사용된다.
	 */
	@Override
	public String toString() {
		if (_sectionName.isEmpty() || _startAddress.isEmpty() || _programLength.isEmpty())
			throw new RuntimeException("illegal operation");

		String sectionName = _sectionName.get();
		int startAddress = _startAddress.get();
		int programLength = _programLength.get();

		String header = String.format("H%-6s%06X%06X\n", sectionName, startAddress, programLength);

		// TODO: 오브젝트 코드 문자열 생성하기.
		String define = "D";
		// 이어붙이기
		define += _defines.stream().map(d -> String.format("%-6s%06X", d.symbolName, d.address))
				.reduce("", (acc, x) -> acc + x);
		define += "\n";
		// 마지막에 줄바꿈 붙이기
		if (_defines.isEmpty()) define = "";

		String refer = "R";
		refer += _refers.stream().map(r -> String.format("%-6s", r))
				.reduce("", (acc, x) -> acc + x);
		refer += "\n";
		if (_refers.isEmpty()) refer = "";

		// Text는 최대 70B -> 앞에 9 빼고 30B -> 총 60글자로...
		// 첫 녀석은 "T%06X%02X%s", t.address, t.value.length() / 2, t.value 다 넣기
		// 그 뒤로 오는 녀석들은 t.value만 넣어가면서 크기 계산
		// 만약 크기가 꽉 찼으면 줄바꾸기
		// 첫 녀석은 "T%06X%02X%s", t.address, t.value.length() / 2, t.value 다 넣기
		// 이런식으로 진행...
		final int MAX_CHARS = 60; // 최대 글자 수 (30바이트)
		String text = "";
		int currentLineStartAddress = 0;
		String currentLineValues = "";
		int currentLength = 0;

		for (Text t : _texts) {
			if (!currentLineValues.isEmpty() && t.line_clear) {
				text += String.format("T%06X%02X%s\n",
						currentLineStartAddress,
						currentLength / 2,
						currentLineValues);
				currentLineValues = ""; // 내용 초기화
				currentLength = 0;
				continue;
			}

			if (currentLineValues.isEmpty()) {
				// 새로운 라인을 시작할 때 시작 주소 설정
				currentLineStartAddress = t.address;
			}

			// 현재 값 추가 전에 길이를 확인
			if (currentLength + t.value.length() > MAX_CHARS) {
				// 현재 줄을 종료하고 새로운 줄을 시작
				text += String.format("T%06X%02X%s\n",
						currentLineStartAddress,
						currentLength / 2,
						currentLineValues);
				currentLineValues = ""; // 내용 초기화
				currentLineStartAddress = t.address; // 새로운 시작 주소 업데이트
				currentLength = 0;
			}

			// 현재 줄에 값을 추가
			currentLineValues += t.value;
			currentLength += t.value.length();
		}

		// 마지막 라인 처리
		if (!currentLineValues.isEmpty()) {
			text += String.format("T%06X%02X%s\n",
					currentLineStartAddress,
					currentLength / 2,
					currentLineValues);
		}

		String modification = _mods.stream()
				.map(m -> String.format("M%06X%02X%s", m.address, m.sizeHalfByte, m.symbolNameWithSign))
				.reduce("", (acc, x) -> acc + x + "\n");

		String end = "E" + _initialPC
				.map(x -> String.format("%06X", x))
				.orElse("");

		return header + define + refer + text + modification + end;
	}

	public void setSectionName(String sectionName) {
		_sectionName = Optional.of(sectionName);
	}

	public void setStartAddress(int address) {
		_startAddress = Optional.of(address);
	}

	public void setProgramLength(int length) {
		_programLength = Optional.of(length);
	}

	public void addDefineSymbol(String symbolName, int address) {
		_defines.add(new Define(symbolName, address));
	}

	public void addReferSymbol(String symbolName) {
		_refers.add(symbolName);
	}

	public void addText(int address, Numeric context, int size) {
		String value = context.getValue(size * 2);
		_texts.add(new Text(address, value));
	}

	public void add_clear_Text() {
		Text clear_Text = new Text(0, "");
		clear_Text.setLine_clear();
		_texts.add(clear_Text);
	}

	public void addModification(String symbolNameWithSign, int address, int sizeHalfByte) {
		_mods.add(new Modification(address, sizeHalfByte, symbolNameWithSign));
	}

	public void setInitialPC(int address) {
		_initialPC = Optional.of(address);
	}

	class Define {
		Define(String symbolName, int address) {
			this.symbolName = symbolName;
			this.address = address;
		}

		String symbolName;
		int address;
	}

	class Text {
		Text(int address, String value) {
			this.address = address;
			this.value = value;
			this.line_clear = false;
		}
		void setLine_clear (){ // 개행 필요하면 true 설정
			line_clear = true;
		}

		int address;
		String value;
		boolean line_clear;
	}

	class Modification {
		Modification(int address, int sizeHalfByte, String symbolNameWithSign) {
			this.address = address; // 위치
			this.sizeHalfByte = sizeHalfByte; // 변경할 개수
			this.symbolNameWithSign = symbolNameWithSign; // ex) +RDREC -BUFFER
		}

		int address;
		int sizeHalfByte;
		String symbolNameWithSign;
	}

	private Optional<String> _sectionName;
	private Optional<Integer> _startAddress;
	private Optional<Integer> _programLength;
	private Optional<Integer> _initialPC;

	private ArrayList<Define> _defines;
	private ArrayList<String> _refers;
	private ArrayList<Text> _texts; // Text Array
	private ArrayList<Modification> _mods;
}
