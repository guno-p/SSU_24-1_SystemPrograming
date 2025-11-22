/**
 * @author Enoch Jung (github.com/enochjung)
 * @file Numeric.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

import symbol.Symbol;
import symbol.SymbolTable;

public class Numeric {
	/**
	 * 두 수치값을 더한 값을 가지는 수치값 객체를 반환한다.
	 * 
	 * @param lhs 수치값 객체 1
	 * @param rhs 수치값 객체 2
	 * @return 더해진 값을 가지는 수치값 객체
	 */
	public static Numeric add(Numeric lhs, Numeric rhs) {
		// TODO: 두 객체를 더한 객체를 반환하기.
		BigInteger resultValue = lhs._value.add(rhs._value);
		HashMap<Symbol, Integer> resultMap = new HashMap<>(lhs._relativeMap);

		rhs._relativeMap.forEach((key, value) ->
				resultMap.merge(key, value, Integer::sum));

		return new Numeric(resultValue, resultMap);
	}

	/**
	 * 두 수치값을 뺀 값을 가지는 수치값 객체를 반환한다.
	 * 
	 * @param lhs 수치값 객체 1
	 * @param rhs 수치값 객체 2
	 * @return 수치값 객체 1에서 수치값 객체 2를 뺀 값을 가지는 수치값 객체
	 */
	public static Numeric subtract(Numeric lhs, Numeric rhs) {
		// TODO: 두 객체를 뺀 객체를 반환하기.
		BigInteger resultValue = lhs._value.subtract(rhs._value);
		HashMap<Symbol, Integer> resultMap = new HashMap<>(lhs._relativeMap);

		rhs._relativeMap.forEach((key, value) ->
				resultMap.merge(key, -value, Integer::sum));

		return new Numeric(resultValue, resultMap);
	}

	/**
	 * 상수 문자열을 파싱하여, 해당 상수 값으로 수치값 객체를 초기화한다.
	 * 
	 * @param constant 상수 문자열
	 * @throws RuntimeException 잘못된 상수 문자열 포맷
	 */
	public Numeric(String constant) throws RuntimeException {
		Numeric numeric = evaluateConstant(constant);

		_value = numeric._value;
		_relativeMap = numeric._relativeMap;
	} // 상수값을 파싱 - 리터럴값이나 간단한 숫자

	/**
	 * 수치값 객체를 초기화한다. 수치는 <code>symbol.address + value</code>를 가진다.
	 * 
	 * @param value  절대값
	 * @param symbol 상대값에서의 주소 심볼
	 */
	public Numeric(int value, Symbol symbol) {
		HashMap<Symbol, Integer> map = new HashMap<Symbol, Integer>();
		map.put(symbol, 1);

		_value = new BigInteger(Integer.toString(value));
		_relativeMap = map;
	} //

	/**
	 * 수식을 계산하여, 해당 값으로 수치값 객체를 초기화한다.
	 * 
	 * @param formula     수식 문자열
	 * @param symbolTable 심볼 테이블
	 * @param locctr      location counter 값
	 * @throws RuntimeException 잘못된 수식 포맷
	 */
	public Numeric(String formula, SymbolTable symbolTable, int locctr) throws RuntimeException {
		Numeric numeric = evaluateFormula(formula, symbolTable, locctr);

		_value = numeric._value;
		_relativeMap = numeric._relativeMap;
	} // 수식에 대한 계산

	/**
	 * 수치값에 단일 심볼이 포함된 경우, 해당 심볼의 명칭을 반환한다.
	 * 
	 * @return 심볼의 명칭. 수치값에 심볼이 없거나 하나보다 많은 경우 empty <code>Optional</end>.
	 */
	public Optional<String> getName() {
		if (_relativeMap.size() != 1)
			return Optional.empty();
		String name = _relativeMap.keySet().stream()
				.map(x -> x.getName())
				.collect(Collectors.joining(""));
		return Optional.of(name);
	}

	/**
	 * 절대값을 반환한다. 절대값이 integer 범위를 초과하는 경우 하위 32-bit 값만 반환한다.
	 * 
	 * @return 절대값
	 */
	public int getInteger() {
		return _value.intValue();
	}

	/**
	 * 절대값을 16진수 형태로 하여 길이가 length인 문자열로 반환한다. 절대값 문자열보다 length가 큰 경우 문자열 앞에
	 * "0"을 추가하며, 그렇지 않은 경우 하위 length 길이만큼의 문자열만 반환한다.
	 * 
	 * @param length 반환할 문자열 길이
	 * @return 절대값을 16진수로 표현한 문자열
	 */
	public String getValue(int length) {
		// TODO: 16진수 문자열 반환하기.

		// ex. _value가 0x10FF이고 length가 6이라면 "0010FF"를 반환.
		// ex. _value가 0xC0DE이고 length가 2이라면 "DE"를 반환.
		// Q. _value가 -0x003이고 length가 2이라면?? "03"을 반환.
		// `_value`의 절대값을 16진수 문자열로 변환하고, 대문자로 변경
		String hexString = _value.abs().toString(16).toUpperCase();

		// 필요한 경우 앞에 '0'을 추가하여 길이를 `length`로 조정
		while (hexString.length() < length) {
			hexString = "0" + hexString;
		}

		// 길이가 너무 길 경우, 뒷부분을 잘라내어 `length` 길이로 만듦
		if (hexString.length() > length) {
			hexString = hexString.substring(hexString.length() - length);
		}

		// 음수인 경우 2의 보수를 계산해야 하나, 여기서는 간단하게 처리
		if (_value.signum() < 0) {
			// BigInteger를 사용하여 2의 보수를 계산
			BigInteger mask = BigInteger.ONE.shiftLeft(length * 4).subtract(BigInteger.ONE); // 예: length 2 -> 0xFF
			BigInteger negValue = mask.subtract(_value.abs().subtract(BigInteger.ONE));
			hexString = negValue.toString(16).toUpperCase();
			// 최종 문자열 길이 조정
			if (hexString.length() > length) {
				hexString = hexString.substring(hexString.length() - length);
			}
		}

		return hexString;
	}

	/**
	 * 절대값을 저장하기 위해 필요한 크기를 반환한다.
	 * 
	 * @return 절대값 크기
	 */
	public int getSize() {
		return (_value.toString(16).length() + 1) / 2;
	}

	/**
	 * 수치값이 절대값으로만 이루어졌는지 여부를 반환한다.
	 * 
	 * @return 절대값으로만 이루어졌는지 여부
	 */
	public boolean isAbsolute() {
		return _relativeMap.size() == 0;
	}

	public int relativeMap_numbers() {
		return _relativeMap.size();
	}

	/**
	 * 수치값이 단일 심볼의 상대값으로 이루어졌는지 여부를 반환한다.
	 * 
	 * @return 단일 심볼의 상대값으로 이루어졌는지 여부
	 */
	public boolean isRelative() {
		if (_relativeMap.size() != 1)
			return false;

		boolean isOnlyOneMultiplier = _relativeMap.values().stream()
				.map(x -> x == 1 ? 1 : 2)
				.reduce(0, (acc, x) -> acc + x) == 1;
		return isOnlyOneMultiplier == true;
	}

	/**
	 * 상대값에 외부 심볼이 포함된 경우, 해당 심볼 명칭에 부호를 붙인 문자열 리스트를 반환한다.
	 * 
	 * @return 상대값에 포함된 심볼 중 외부 심볼 명칭에 부호를 붙인 문자열 리스트
	 */
	public ArrayList<String> getReferSymbolsString() {
		// TODO: 외부 심볼 문자열 리스트를 반환하기.

		// ex. _relativeMap에 (RDREC, 1)이 들어있다면 ["+RDREC"]를 반환하기.
		// ex. _relativeMap에 (BUFEND, 1), (BUFFER, -1)이 들어있다면 ["+BUFEND", "-BUFFER"]를
		// 반환하기.
		ArrayList<String> result = new ArrayList<>();
		_relativeMap.forEach((symbol, multiplier) -> {
			String sign = multiplier > 0 ? "+" : "-";
			result.add(sign + symbol.getName() + (Math.abs(multiplier) == 1 ? "" : "*" + Math.abs(multiplier)));
		}); // 절댓값이 1이 아니라면 그만큼 반복했다고 명시
		return result;
	}

	/**
	 * 현재 수치값의 상대값을 평탄화한 수치값 객체를 반환한다.
	 * 
	 * <pre>
	 * EQU   A    COPY<br>EQU   B    A+1<br>EQU   C    B+2<br>
	 * </pre>
	 * 
	 * 위 예시에서 <code>B.flat()</code>은 <code>COPY+1</code>를 반환하며,
	 * <code>C.flat()</code>은 <code>COPY+3</code>를 반환한다.
	 * 기존 <code>B</code> 및 <code>C</code>는 변하지 않는다 (이전 상태인
	 * <code>A+1</code> 및 <code>B+2</code>를 유지한다).
	 * 
	 * @return 평탄화된 수치값 객체
	 * @throws RuntimeException 상대값의 심볼 중 주소값이 할당되지 않은 심볼이 존재
	 */
	public Numeric flat() throws RuntimeException {
		// TODO: 평탄화된 수치값 객체를 생성하고, 결과를 반환하기.
		BigInteger flatValue = new BigInteger(_value.toByteArray());
		HashMap<Symbol, Integer> flatMap = new HashMap<>();

		for (Map.Entry<Symbol, Integer> entry : _relativeMap.entrySet()) {
			Symbol symbol = entry.getKey();
			int multiplier = entry.getValue();

			Numeric symbolNumeric = symbol.getAddress()
					.orElseThrow(() -> new RuntimeException("Undefined symbol address"));
			flatValue = flatValue.add(symbolNumeric._value.multiply(BigInteger.valueOf(multiplier)));

			symbolNumeric._relativeMap.forEach((sym, mult) ->
					flatMap.merge(sym, mult * multiplier, Integer::sum));
		}

		return new Numeric(flatValue, flatMap);
	}

	/**
	 * 수치값 객체의 정보를 문자열로 반환한다. 디버그 용도로 사용한다.
	 */
	@Override
	public String toString() {
		String value = String.format("0x%s", _value.toString(16).toUpperCase());
		String relative = _relativeMap.entrySet().stream()
				.map(x -> {
					String symbolName = x.getKey().getName();
					int multiplier = x.getValue();

					String sign = multiplier > 0 ? "+" : "-";
					String mulStr = Math.abs(multiplier) == 1 ? "" : "*" + multiplier;

					return sign + symbolName + mulStr;
				})
				.collect(Collectors.joining(""));
		return value.equals("") ? relative.substring(1) : value + relative;
	}

	/**
	 * 심볼을 상대값 맵에 N번 추가한다.
	 * 
	 * @param relativeMap 상대값 맵
	 * @param symbol      추가할 심볼
	 * @param times       추가할 횟수. 음수일 경우 해당 횟수만큼 제거.
	 */
	private static void addRelative(HashMap<Symbol, Integer> relativeMap, Symbol symbol, int times) {
		// TODO: 심볼을 relativeMap에 추가하기.
		relativeMap.merge(symbol, times, Integer::sum);
	}

	/**
	 * 상수 문자열을 파싱하고, 해당 상수를 절대값으로 가지는 수치값 객체를 반환한다. 파싱할 수 있는 상수는 캐릭터
	 * 문자열(ex. <code>C'EOF'</code>), 16진수 숫자(ex. <code>X'F1'</code>), 10진수 숫자(ex.
	 * <code>3</code>)이다.
	 * 
	 * @param constant 상수 문자열
	 * @return 수치값 객체
	 * @throws RuntimeException 잘못된 상수 문자열 포맷
	 */
	private static Numeric evaluateConstant(String constant) throws RuntimeException {
		// TODO: 상수 문자열을 파싱하여 수치값 객체를 생성 및 반환하기.

		// ex. 상수 문자열이 "3"인 경우 Numeric{_value:3, _relativeMap:[]}을 반환하기.
		// ex. 상수 문자열이 "C'EOF'"인 경우 Numeric{_value:0x454F46, _relativeMap:[]}을 반환하기.
		// ex. 상수 문자열이 "X'F1'"인 경우 Numeric{_value:0xF1, _relativeMap:[]}을 반환하기.

		BigInteger value = BigInteger.ZERO;

		if (constant.startsWith("C'") && constant.endsWith("'")) {
			// C'EOF'의 경우 처리
			String characters = constant.substring(2, constant.length() - 1); // "EOF"
			for (char ch : characters.toCharArray()) {
				value = value.shiftLeft(8).add(BigInteger.valueOf(ch)); // 1B : 8bit shift
			}
		} else if (constant.startsWith("X'") && constant.endsWith("'")) {
			// X'F1'의 경우 처리
			String hex = constant.substring(2, constant.length() - 1); // "F1"
			value = new BigInteger(hex, 16);
		} else if (constant.matches("\\d+")) {
			// "3"의 경우 처리
			value = new BigInteger(constant);
		} else {
			throw new RuntimeException("Invalid constant format");
		}

		return new Numeric(value, new HashMap<>());
	}

	/**
	 * 수식 내에서 피연산자에 해당하는 문자열을 파싱하고, 해당 피연산자의 수치를 수치값 객체로 반환한다. 파싱할 수 있는 피연산자는 괄호(ex.
	 * <code>(123+456)</code>), 특수 기호(<code>*</code>), 심볼(ex.<code>BUFEND</code>)이다.
	 * 
	 * @param operand     피연산자 문자열
	 * @param symbolTable 심볼 테이블
	 * @param locctr      location counter 값
	 * @return 수치값 객체
	 * @throws RuntimeException 잘못된 피연산자 문자열 포맷
	 */
	private static Numeric evaluateOperand(String operand, SymbolTable symbolTable, int locctr)
			throws RuntimeException {
		// TODO: 피연산자 수식을 계산하여 수치값 객체를 생성 및 반환하기.
		if (operand.equals("*")) {
			// 특수 기호 '*'는 현재의 locctr을 사용
			return new Numeric(String.valueOf(locctr));
		} else if (Character.isDigit(operand.charAt(0)) || operand.startsWith("-")) {
			// 숫자 또는 음수로 시작하는 경우 숫자로 처리
			return new Numeric(operand);
		} else {
			// 심볼을 찾아 해당 주소 또는 상대 위치를 반환
			Optional<Symbol> sym = symbolTable.search(operand);
			if (sym.isPresent()) {
				return new Numeric(0, sym.get());
			} else {
				throw new RuntimeException("Undefined symbol: " + operand);
			}
		}

		// ex. 피연산자 문자열이 "(BUFFER-100)"인 경우 Numeric{_value:-100,
		// _relativeMap:[(BUFFER,+1)]}을 반환하기.
		// ex. 피연산자 문자열이 "BUFEND"인 경우 Numeric{_value:0, _relativeMap:[(BUFEND),+1)]}을
		// 반환하기.
		// Q. 피연산자 문자열이 "*"인 경우는?? -> _value:locctr, _relativeMap:null

		// Numeric something = null;
	}

	/**
	 * 수식 문자열을 파싱하고, 해당 수식을 계산하여 수치값 객체로 반환한다. 수식에는 피연산자로 괄호, 특수 기호(<code>*</code>),
	 * 심볼, 상수가 포함될 수 있으며, 이항연산자로 +, -가 포함될 수 있으며, 단항연산자로 -가 포함될 수 있다.
	 * 
	 * @param formula     수식 문자열
	 * @param symbolTable 심볼 테이블
	 * @param locctr      location counter 값
	 * @return 수치값 객체
	 * @throws RuntimeException 잘못된 수식 포맷
	 */
	private static Numeric evaluateFormula(String formula, SymbolTable symbolTable, int locctr)
			throws RuntimeException {
		// TODO: 수식을 계산하여 수치값 객체를 생성 및 반환하기.
		// Q. evaluateOperand 함수와 차이점은?

		BigInteger value = BigInteger.ZERO;
		HashMap<Symbol, Integer> relativeMap = new HashMap<>();

		if (formula.equals("*")) {
			// 특수 기호 '*'는 현재의 locctr을 사용
			return new Numeric(String.valueOf(locctr));
		}

		String[] parts = formula.split("-");
		if (parts.length == 0) {
			// - 안쓰임
		}
		Optional<Symbol> firstSymbol = symbolTable.search(parts[0]);
		Optional<Symbol> secondSymbol = symbolTable.search(parts[1]);
		if (firstSymbol.get().getAddress().isPresent() && secondSymbol.get().getAddress().isPresent()) {
			value = firstSymbol.get().getAddress().get()._value.subtract(
					secondSymbol.get().getAddress().get()._value);

			return new Numeric(value, relativeMap);
		} else {
			relativeMap.put(firstSymbol.get(), 1); // BUFEND에 +1
			relativeMap.put(secondSymbol.get(), -1); // BUFFER에 -1
			return new Numeric(value, relativeMap);
		}
	}

	/**
	 * 피연산자로 시작하는 수식 문자열에서 피연산자의 길이를 반환한다.
	 * 
	 * @param formula 피연산자로 시작하는 수식 문자열
	 * @return 피연산자의 길이
	 * @throws RuntimeException 잘못된 수식 포맷
	 */
	private static int getOperandLength(String formula) throws RuntimeException {
		// TODO: 함수를 구현하고, evaluateOperand 혹은 evaluateFormula 함수에서 사용하기.

		int plusIndex = formula.indexOf('+');
		int minusIndex = formula.indexOf('-');

		if (plusIndex == -1 && minusIndex == -1) {
			return formula.length(); // 더 이상 연산자가 없는 경우?
		} else if (plusIndex == -1) {
			return minusIndex;
		} else if (minusIndex == -1) {
			return plusIndex;
		} else {
			return Math.min(plusIndex, minusIndex);
		}
	}

	private Numeric(BigInteger value, HashMap<Symbol, Integer> relativeMap) {
		_value = value;
		_relativeMap = relativeMap;
	}

	/** C'EOFFFFFFFFFFFFFFFFFFFFF'와 같은 입력에서도 동작하도록 하기 위하여 BigInteger를 사용함 */
	private final BigInteger _value;

	private final HashMap<Symbol, Integer> _relativeMap;
}
