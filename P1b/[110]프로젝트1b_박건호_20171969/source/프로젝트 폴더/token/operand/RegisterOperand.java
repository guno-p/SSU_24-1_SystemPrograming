/**
 * @author Enoch Jung (github.com/enochjung)
 * @file RegisterOperand.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package token.operand;

public class RegisterOperand extends Operand {
	public RegisterOperand(Register register) {
		_register = register;
	}

	/**
	 * 레지스터 operand 객체의 정보를 문자열로 반환한다. 디버그 용도로 사용된다.
	 */
	@Override
	public String toString() {
		String registerName = _register.getName();

		return "Register(" + registerName + ")";
	}

	public int getValue() {
		return _register.value;
	}

	private final Register _register;
}
