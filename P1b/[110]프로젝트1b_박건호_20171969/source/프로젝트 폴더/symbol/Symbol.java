/**
 * @author Enoch Jung (github.com/enochjung)
 * @file Symbol.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package symbol;

import java.util.Optional;

import numeric.Numeric;

public class Symbol {
	/**
	 * 문자열이 심볼 문자열 형태인지 판별한다.
	 * 
	 * @param symbol 판별할 문자열
	 * @return 심볼 문자열로 사용 가능한 형태인지 여부
	 */
	public static boolean isSymbol(String symbol) {
		String symbolRegex = "^[a-zA-Z][a-zA-Z0-9]*$";
		return symbol.matches(symbolRegex) && symbol.length() <= 6;
	} // 심볼문자열로 사용 가능하다면 true ex) Xyz123 - true / 123abc false

	/**
	 * 심볼 명칭을 반환한다.
	 * 
	 * @return 심볼 명칭
	 */
	public String getName() {
		return _name;
	}

	/**
	 * 상대 주소로 표현될 수 없는 심볼인지 (START,CSECT,EXTREF로 생성된 심볼인지) 판별한다.
	 * 
	 * @return 상대 주소로 표현될 수 없는지 여부
	 */
	public boolean isBaseSymbol() {
		return _state == State.REP_SECTION || _state == State.EXTERNAL;
	}

	/**
	 * EXTREF로 생성된 심볼인지 판별한다.
	 * 
	 * @return EXTREF로 생성된 심볼인지 여부
	 */
	public boolean isReferSymbol() {
		return _state == State.EXTERNAL;
	}

	/**
	 * 심볼의 정보를 문자열로 반환한다. 디버그 용도로 사용한다.
	 */
	@Override
	public String toString() {
		String name = _name;
		String address = _address
				.map(x -> x.toString())
				.map(x -> x.replace("+" + name, ""))
				.map(x -> x.replace("+", " \t+ "))
				.orElse("(not assigned)");
		String section;
		if (isBaseSymbol() || isReferSymbol() || _section == null) {
			section = ""; // 섹션 대표심볼, 상대주소 아닌 것들은 ""
		} else {
			section = _section;
		}
		String formatted = String.format("%-12s%-12s%s", name, _state == State.EXTERNAL ? "REF" : address,
				section); // ex) COPY
		return formatted;
	}

	public Optional<Numeric> getAddress() {
		return _address;
	}

	/**
	 * 대표 심볼 객체를 초기화한다.
	 * 
	 * @param name    심볼 명칭
	 * @param address 절대 주소
	 * @return 대표 심볼 객체
	 * @throws RuntimeException 부적절한 심볼 명칭
	 */
	static Symbol createRepSymbol(String name, int address) throws RuntimeException {
		// TODO: symbol 객체의 address 할당하기.
		if (!isSymbol(name)) {
			throw new RuntimeException("Illegal symbol name: " + name);
		} // 대표심볼 할당을 진행한다. State REP_SECTION
		Numeric numeric = new Numeric(String.valueOf(address));
		Optional<Numeric> optionalNumeric = Optional.of(numeric);
		Symbol symbol = new Symbol(name, optionalNumeric, State.REP_SECTION);
		return symbol;
	}

	/**
	 * 외부 심볼 객체를 초기화한다.
	 *
	 * @param name 심볼 명칭
	 * @return 외부 심볼 객체
	 * @throws RuntimeException 부적절한 심볼 명칭
	 */
	static Symbol createExternalSymbol(String name) throws RuntimeException {
		// TODO: 외부 심볼 객체 생성하기.
		// Symbol symbol = new Symbol(?, ?, ?);
		// and something more...?
		if (!isSymbol(name)) {
			throw new RuntimeException("Illegal symbol name: " + name);
		} // state EXTERNAL
		Optional<Numeric> optionalNumeric = Optional.empty();
		Symbol symbol = new Symbol(name, optionalNumeric, State.EXTERNAL);
		return symbol;
	}

	/**
	 * 주소값이 주어진 일반 심볼 객체를 초기화한다.
	 * 
	 * @param name    심볼 명칭
	 * @param address 주소값
	 * @return 일반 심볼 객체
	 * @throws RuntimeException 부적절한 심볼 명칭
	 */
	static Symbol createAddressAssignedSymbol(String name, Numeric address) throws RuntimeException {
		if (!isSymbol(name)) {
			throw new RuntimeException("Illegal symbol name: " + name);
		}
		Symbol symbol = new Symbol(name, Optional.of(address), State.ADDRESS_ASSIGNED);
		return symbol;
	} // 심볼을 새로 인식할때, 레이블에 나온 심볼은 locctr통해 주소계산 바로 가능

	/**
	 * 주소값이 주어지지 않은 일반 심볼 객체를 초기화한다.
	 * 
	 * @param name 심볼 명칭
	 * @return 일반 심볼 객체
	 * @throws RuntimeException 부적절한 심볼 명칭
	 */
	static Symbol createAddressNotAssignedSymbol(String name) throws RuntimeException {
		if (!isSymbol(name)) {
			throw new RuntimeException("Illegal symbol name: " + name);
		}
		Symbol symbol = new Symbol(name, Optional.empty(), State.ADDRESS_NOT_ASSIGNED);
		return symbol;
	}

	/**
	 * 주소값이 주어지지 않은 일반 심볼 객체의 주소를 설정한다.
	 * 
	 * @param address 주소값
	 * @throws RuntimeException 주소값이 주어지지 않은 일반 심볼이 아님
	 */
	void assign(Numeric address) throws RuntimeException {
		// TODO: 주소값 설정하기.
		if (_state != State.ADDRESS_NOT_ASSIGNED) {
			throw new RuntimeException("Address can only be assigned to symbols with not assigned state.");
		} // 주소가 설정 안되어있던 심볼에 대해 주소 할당
		_address = Optional.of(address);
		_state = State.ADDRESS_ASSIGNED;
	}
	void assign_section(String _repsect) throws RuntimeException {
		_section = _repsect;
	} // 상대주소를 명시적으로 넣는 함수 ex) COPY

	public boolean state_ADDRESS_NOT_ASSIGNED() {
		if (_state == State.ADDRESS_NOT_ASSIGNED) return  true;
		else return false;
	} // 예외처리 위한 > 주소 할당 안되어있다면 true

	private Symbol(String name, Optional<Numeric> address, State state) throws RuntimeException {
		if (!isSymbol(name))
			throw new RuntimeException("illegal symbol name");
		_name = name;
		_address = address;
		_state = state;
	}

	/**
	 * 심볼의 상태값
	 * 
	 * <ul>
	 * <li><code>State.REP_SECTION</code>: control section의 명칭으로 선언한 대표 심볼
	 * <li><code>State.EXTERNAL</code>: EXTREF로 선언한 외부 심볼
	 * <li><code>State.ADDRESS_ASSIGNED</code>: label에서 등장하여 주소값이 결정된 일반 심볼
	 * <li><code>State.ADDRESS_NOT_ASSIGNED</code>: operand에서 등장하였으나, 아직 label에서는
	 * 등장하지 않아 주소값이 결정되지 않은 일반 심볼
	 * </ul>
	 */
	private enum State {
		/**
		 * control section의 명칭으로 선언한 대표 심볼
		 */
		REP_SECTION,

		/**
		 * EXTREF로 선언한 외부 심볼
		 */
		EXTERNAL,

		/**
		 * label에서 등장하여 주소값이 결정된 일반 심볼
		 */
		ADDRESS_ASSIGNED,

		/**
		 * operand에서 등장하였으나, 아직 label에서는 등장하지 않아 주소값이 결정되지 않은 일반 심볼
		 */
		ADDRESS_NOT_ASSIGNED;
	}

	private final String _name;
	private Optional<Numeric> _address;
	private State _state;
	private String _section;
	// 상대주소 명시 String 추가
}
