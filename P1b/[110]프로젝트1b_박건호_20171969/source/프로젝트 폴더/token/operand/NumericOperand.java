/**
 * @author Enoch Jung (github.com/enochjung)
 * @file NumericOperand.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package token.operand;

import numeric.Numeric;

public class NumericOperand extends Operand {
	public NumericOperand(Numeric numeric) {
		_numeric = numeric;
	}

	/**
	 * 수치값 operand 객체의 정보를 문자열로 반환한다. 디버그 용도로 사용된다.
	 */
	@Override
	public String toString() {
		return "Numeric(" + _numeric.toString() + ")";
	}

	public Numeric getNumeric() {
		return _numeric;
	}

	private final Numeric _numeric;
}
