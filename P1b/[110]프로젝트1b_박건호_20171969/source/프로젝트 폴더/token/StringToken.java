/**
 * @author Enoch Jung (github.com/enochjung)
 * @file StringToken.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package token;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import java.util.Arrays;
import instruction.*;

public class StringToken {
	/**
	 * 소스 코드 한 줄에 해당하는 토큰을 초기화한다.
	 * 
	 * @param input 소스 코드 한 줄에 해당하는 문자열
	 * @throws RuntimeException 잘못된 형식의 소스 코드 파싱 시도.
	 */
	public StringToken(String input) throws RuntimeException {
		// TODO: 소스 코드를 파싱하여 토큰을 초기화하기.

		/*
		 * 1. input의 첫 문자가 .이라면 주석으로 처리
		 * 2. 첫 문자가 \t가 아니라면 레이블로 설정
		 * 3. 첫 문자가 \t이라면 오퍼레이터부터 설정
		 *
		 * 4. 오퍼레이터 설정이 끝나면 피연산자를 체크하는데
		 *    여기서 \tOPERAND\tCOMMENT 의 경우와
		 *         \t\tCOMMENT의 경우와
		 *         아무것도 없는 경우가 존재
		 * 	각각에 대해 제대로 파싱 진행해야함
		 */

		_label = Optional.empty();
		_operator = Optional.empty();
		_operands = new ArrayList<>();
		_comment = Optional.empty();

		_nBit = true;
		_iBit = true;
		_xBit = false;
		_pBit = true;
		_eBit = false;

		// 1. .으로 시작하면 주석 처리
		if (input.startsWith(".")) {
			_comment = Optional.of(input.substring(1).trim());
			return;
		}

		int pos = 0; // 현재 위치 0 에서 시작
		int length = input.length(); // 입력 문자열의 길이

		// 2. 레이블 추출
		if (Character.isLetter(input.charAt(pos))) { // 첫 문자가 문자인 경우
			int start = pos;
			while (pos < length && input.charAt(pos) != '\t') pos++;
			_label = Optional.of(input.substring(start, pos));
			pos++; // 탭을 넘어서 연산자를 가리키도록 함
		}
		else {
			pos++; // 첫 문자가 \t인 경우 건너 뜀
		}

		// 3. 연산자 추출
		if (pos < length) {
			int start = pos;
			while (pos < length && input.charAt(pos) != '\t') pos++;
			String operator = input.substring(start, pos);
			if (operator.startsWith("+")) {
				_eBit = true;
				_pBit = false;
				operator = operator.substring(1);
			}
			_operator = Optional.of(operator);
			pos++;
		}

		// 4. 피연산자 추출
		if (pos < length && input.charAt(pos) != '\t') { // 다음 문자가 탭이 아니라면 피연산자가 존재
			int start = pos;
			while (pos < length && input.charAt(pos) != '\t') pos++;
			String operands = input.substring(start, pos);
			// _operands.addAll(Arrays.asList(operands.split(",")));

			String[] operandArray = operands.split(",");
			ArrayList<String> processedOperands = new ArrayList<>();

			// 피연산자 처리 필요함
			// 만약 첫 피연산자의 첫 문자가 @이라면 _nBit true, _iBit false
			// 만약 첫 피연산자의 첫 문자가 #이라면 _nBit false, _iBit false
			// 그리고 substring(1)

			// 만약 필요한 피연산자 수보다 많은 경우 "X"인지 확인
			// X라면 _xBit true
			// 그리고 해당 피연산자 삭제
			// 이 작업을 위해서는 _operator를 instTable에서 search해서 피연산자 형식을 봐야 할 듯

			for (int i = 0; i < operandArray.length; i++) { // 하나씩 돌면서 확인
				String operand = operandArray[i].trim(); // 혹시모르니 공백 제거 (BUFFER, X) 이런식으로 쓸수도?
				if (i == 0) {
					if (operand.charAt(0) == '@') {
						_nBit = true;
						_iBit = false;
						operand = operand.substring(1); // '@' 제거
					}
					else if (operand.charAt(0) == '#') {
						_nBit = false;
						_iBit = true;
						_pBit = false;
						operand = operand.substring(1); // '#' 제거
					}
				}

				processedOperands.add(operand);
			}

			// _xBit 검사 및 처리
			// (no label)    <CLEAR>    (no operand)X    <CLEAR LOOP COUNTER>
			// 예시 처리는 그냥 X나오면 _xBit true로 처리? 아마?
			// 조건 1. Operand 존재 2. 마지막 Operand가 X 3. Operand는 2개 이상 (CLEAR X의 경우)
			// 그런데 COMR A,X 는???
			// 더 정확한 처리를 위해서는 instTable에서 검색 필요...
			if (!processedOperands.isEmpty()
					&& processedOperands.get(processedOperands.size() - 1).equals("X")
					&& processedOperands.size() > 1) {
				_xBit = true; // 인덱싱 비트 설정
				processedOperands.remove(processedOperands.size() - 1); // 'X' 제거
			}

			_operands.addAll(processedOperands); // 처리된 피연산자들을 최종 목록에 추가
			pos++;
		}

		// 5. 주석 처리
		if (pos < length) { // 남은 문자열은 모두 주석으로 처리
			_comment = Optional.of(input.substring(pos).trim());
		}

		// System.out.println(this.toString()); /** 디버깅 용도 */
	}

	/**
	 * label 문자열을 반환한다.
	 * 
	 * @return label 문자열. 없으면 empty <code>Optional</code>.
	 */
	public Optional<String> getLabel() {
		return _label;
	}

	/**
	 * operator 문자열을 반환한다.
	 * 
	 * @return operator 문자열. 없으면 empty <code>Optional</code>.
	 */
	public Optional<String> getOperator() {
		return _operator;
	}

	/**
	 * operand 문자열 배열을 반환한다.
	 * 
	 * @return operand 문자열 배열
	 */
	public ArrayList<String> getOperands() {
		return _operands;
	}

	/**
	 * comment 문자열을 반환한다.
	 * 
	 * @return comment 문자열. 없으면 empty <code>Optional</code>.
	 */
	public Optional<String> getComment() {
		return _comment;
	}

	/**
	 * 토큰의 iNdirect bit가 1인지 여부를 반환한다.
	 * 
	 * @return N bit가 1인지 여부
	 */
	public boolean isN() {
		return _nBit;
	}

	/**
	 * 토큰의 Immediate bit가 1인지 여부를 반환한다.
	 * 
	 * @return I bit가 1인지 여부
	 */
	public boolean isI() {
		return _iBit;
	}

	/**
	 * 토큰의 indeX bit가 1인지 여부를 반환한다.
	 * 
	 * @return X bit가 1인지 여부
	 */
	public boolean isX() {
		return _xBit;
	}

	/**
	 * 토큰의 Pc relative bit가 1인지 여부를 반환한다.
	 * 
	 * @return P bit가 1인지 여부
	 */
	public boolean isP() {
		return _pBit;
	}

	/**
	 * 토큰의 Extra bit가 1인지 여부를 반환한다.
	 * 
	 * @return E bit가 1인지 여부
	 */
	public boolean isE() {
		return _eBit;
	}

	/**
	 * StringToken 객체의 정보를 문자열로 반환한다. 디버그 용도로 사용한다.
	 */
	@Override
	public String toString() {
		String label = _label.map(x -> "<" + x + ">").orElse("(no label)");
		String operator = (isE() ? "+" : "") + _operator.map(x -> "<" + x + ">").orElse("(no operator)");
		String operand = (isN() && !isI() ? "@" : "") + (isI() && !isN() ? "#" : "")
				+ (_operands.isEmpty() ? "(no operand)"
						: "<" + _operands.stream().collect(Collectors.joining("/")) + ">")
				+ (isX() ? (_operands.isEmpty() ? "X" : "/X") : "");
		String comment = _comment.map(x -> "<" + x + ">").orElse("(no comment)");

		String formatted = String.format("%-12s\t%-12s\t%-18s\t%s", label, operator, operand, comment);
		return formatted;
	}

	private Optional<String> _label;
	private Optional<String> _operator;
	private ArrayList<String> _operands;
	private Optional<String> _comment;

	private boolean _nBit;
	private boolean _iBit;
	private boolean _xBit;
	// private boolean _bBit; /** base relative는 구현하지 않음 */
	private boolean _pBit;
	private boolean _eBit;
}
