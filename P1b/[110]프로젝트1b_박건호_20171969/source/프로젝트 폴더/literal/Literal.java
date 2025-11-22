/**
 * @author Enoch Jung (github.com/enochjung)
 * @file Literal.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package literal;

import java.util.Optional;
import numeric.Numeric;

public class Literal {
	/**
	 * 리터럴 객체를 초기화한다. 주소는 할당하지 않는다.
	 * 
	 * @param literal 리터럴 문자열
	 * @throws RuntimeException 잘못된 리터럴 문자열 포맷
	 */
	Literal(String literal) throws RuntimeException {
		// TODO: 리터럴 객체 초기화하기.
		_literal = literal;
		_address = Optional.empty();
		_value = new Numeric(literal.substring(1));
		written_flag = false;
	}

	/**
	 * 리터럴 String을 반환한다.
	 * 
	 * @return 리터럴 String
	 */
	public String getLiteral() {
		return _literal;
	}

	public Numeric getValue() {
		return _value;
	}

	/**
	 * 리터럴의 주소를 반환한다.
	 * 
	 * @return 리터럴의 주소. 주소가 지정되지 않은 경우 empty <code>Optional</code>
	 */
	public Optional<Integer> getAddress() {
		return _address;
	}

	/**
	 * 리터럴의 수치값을 저장하기 위해 필요한 크기를 반환한다.
	 * 
	 * @return 수치값 크기
	 */
	public int getSize() {
		return _value.getSize();
	}

	/**
	 * 리터럴 객체의 정보를 문자열로 반환한다. 리터럴 테이블 출력 용도로 사용한다.
	 */
	@Override
	public String toString() {
		String literal = _literal;
		String address = _address
				.map(x -> String.format("%X", x))
				.map(x -> x.replaceAll("[+].*$", ""))
				.orElse("(not assigned)");
		String formatted = String.format("%-12s%s", literal, address);
		return formatted;
	}

	/**
	 * 리터럴의 주소를 지정한다.
	 * 
	 * @param address 리터럴의 주소
	 */
	void assignAddress(int address) {
		_address = Optional.of(address);
	}
	public void set_written_flag() { written_flag = true; }
	public boolean Is_Written() { return written_flag; }

	private final String _literal;

	/** 리터럴 주소. 주소가 지정되지 않은 경우 empty <code>Optional</code> */
	private Optional<Integer> _address;
	private final Numeric _value;
	private boolean written_flag;
	// 리터럴을 PASS2에서 다룰때, 이 플래그를 통해 중복 처리 되지 않도록 한다.
}
