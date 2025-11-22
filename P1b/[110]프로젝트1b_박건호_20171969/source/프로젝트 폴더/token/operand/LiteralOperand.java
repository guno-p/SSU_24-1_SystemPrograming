/**
 * @author Enoch Jung (github.com/enochjung)
 * @file LiteralOperand.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package token.operand;

import java.util.Optional;

import literal.Literal;

public class LiteralOperand extends Operand {
	public LiteralOperand(Literal literal) {
		_literal = literal;
	}

	/**
	 * 리터럴 operand 객체의 정보를 문자열로 반환한다. 디버그 용도로 사용된다.
	 */
	@Override
	public String toString() {
		String literalName = _literal.getLiteral();

		return "Literal(" + literalName + ")";
	}

	public Optional<Integer> getAddress() {
		return _literal.getAddress();
	}

	private final Literal _literal;
}
