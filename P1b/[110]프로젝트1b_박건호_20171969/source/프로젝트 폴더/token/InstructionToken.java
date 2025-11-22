/**
 * @author Enoch Jung (github.com/enochjung)
 * @file InstructionToken.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Optional;

import instruction.Instruction;
import literal.Literal;
import literal.LiteralTable;
import numeric.Numeric;
import symbol.SymbolTable;
import token.operand.*;

public class InstructionToken extends Token {
	public InstructionToken(String tokenString, int address, int size, Instruction inst, ArrayList<Operand> operands,
			boolean nBit, boolean iBit, boolean xBit, boolean pBit, boolean eBit) {
		super(tokenString, address, size);
		_inst = inst;
		_operands = operands;
		_nBit = nBit;
		_iBit = iBit;
		_xBit = xBit;
		_pBit = pBit;
		_eBit = eBit;
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
	 * 토큰의 Base relative bit가 1인지 여부를 반환한다.
	 * 
	 * @return B bit가 1인지 여부
	 */
	public boolean isB() {
		return false;
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
	 * InstructionToken 객체의 정보를 문자열로 반환한다. 디버그 용도로 사용한다.
	 */
	@Override
	public String toString() {
		String instName = _inst.getName();
		String operands = _operands.isEmpty() ? "(empty)"
				: (_operands.stream()
						.map(x -> x.toString())
						.collect(Collectors.joining("/")));
		String nixbpe = String.format("0b%d%d%d%d%d%d", _nBit ? 1 : 0, _iBit ? 1 : 0, _xBit ? 1 : 0, 0, _pBit ? 1 : 0,
				_eBit ? 1 : 0);
		return "InstructionToken{name:" + instName + ", operands:" + operands + ", nixbpe:" + nixbpe + "}";
	}

	/**
	 * object code에 관한 정보를 반환한다.
	 * 
	 * @return 텍스트 레코드 정보가 담긴 객체
	 * @throws RuntimeException 잘못된 심볼 객체 변환 시도.
	 */
	public TextInfo getTextInfo(SymbolTable symTab, LiteralTable litTab) throws RuntimeException {
		int address = this.getAddress();
		Numeric code;
		int size = this.getSize();
		Optional<ModificationInfo> modInfo = Optional.empty();

		// TODO: pass2 과정 중, 오브젝트 코드 생성을 위한 정보를 TextInfo 객체에 담아서 반환하기.
		int opcode = _inst.getOpcode() & 0xFF;
		int operandCode = 0;

		if (size == 2) { // 2형식
			operandCode += (opcode << 8);
			if (_inst.getOperandType() == Instruction.OperandType.REG) {
				RegisterOperand registerOperand = (RegisterOperand) _operands.get(0);
				operandCode += registerOperand.getValue() << 4;
			} else if (_inst.getOperandType() == Instruction.OperandType.REG1_REG2) {
				RegisterOperand registerOperand1 = (RegisterOperand) _operands.get(0);
				RegisterOperand registerOperand2 = (RegisterOperand) _operands.get(1);
				operandCode += registerOperand1.getValue() << 4;
				operandCode += registerOperand2.getValue();
			}
			// no operand-> 아무 행동 안한다
		} else if (size == 3) { // 3형식
			int PC = address + size;
			// PC값 계산
			int displacement;
			operandCode += (opcode << 16);
			if (_nBit) { operandCode += 32 << 12; }
			if (_iBit) { operandCode += 16 << 12; }
			if (_xBit) { operandCode += 8 << 12; }
			if (_pBit) { operandCode += 2 << 12; }
			if (_eBit) { operandCode += 1 << 12; }

			if (_inst.getOperandType() == Instruction.OperandType.NO_OPERAND) {
			} else if (_inst.getOperandType() == Instruction.OperandType.MEMORY) {
				String opd0 = _operands.get(0).toString().substring(0,7);
				switch (opd0) {
					case "Numeric":
						NumericOperand numericOperand = (NumericOperand) _operands.get(0);
						if (numericOperand.getNumeric().isAbsolute()) {
							displacement = numericOperand.getNumeric().getInteger();
							operandCode += displacement;
						} else {
							String Target_symbol = numericOperand.getNumeric().getReferSymbolsString().get(0).toString().substring(1);
							int Target_address = symTab.search(Target_symbol).get().getAddress().get().getInteger();
							displacement = Target_address - PC;
							operandCode += displacement;
							if (displacement < 0) { // !!! 만약 계산 결과가 음수 -> TA가 PC보다 작은 값이었다
								operandCode += 4096; // 만약 계산 했는데 음수이면 자릿수 하나 빌려온것이다
							} // 그래서 결과에 0x1000 추가해준다
						}
						break;
					case "Literal":
						LiteralOperand literalOperand = (LiteralOperand) _operands.get(0);
						displacement = literalOperand.getAddress().get() - PC;
						operandCode += displacement;
						if (displacement < 0) { // !!! 만약 계산 결과가 음수 -> TA가 PC보다 작은 값이었다
							operandCode += 4096; // 만약 계산 했는데 음수이면 자릿수 하나 빌려온것이다
						} // 그래서 결과에 0x1000 추가해준다
						break;
				}
			}
		} else if (size == 4) {
			// 4형식
			int PC = address + size;
			operandCode += (opcode << 24);
			if (_nBit) { operandCode += 32 << 20; }
			if (_iBit) { operandCode += 16 << 20; }
			if (_xBit) { operandCode += 8 << 20; }
			if (_pBit) { operandCode += 2 << 20; }
			if (_eBit) { operandCode += 1 << 20; }
			// modification 장보 추가
			ArrayList<String> refers = new ArrayList<>();
			for (int i = 0;  i < _operands.size(); i++) { // 예제에선 굳이 반복할 필요 없지만.. 그레도?
				NumericOperand numericOperand = (NumericOperand) _operands.get(i);
				String refer = numericOperand.getNumeric().getReferSymbolsString().get(0).toString();
				refers.add(refer);
				String check = refer.substring(1);
				if (symTab.search(check).get().state_ADDRESS_NOT_ASSIGNED()) {
					throw new RuntimeException("PASS2 Error : missing symbol definition : " + check + "\n");
				} // 에러 처리 Ex) WAAAA
			}
			modInfo = Optional.of(new ModificationInfo(refers, address + 1, 5));
		} else {
			throw new RuntimeException("wrong size instruction\n");
		}
		code = new Numeric(Integer.toString(operandCode));

		TextInfo textInfo = new TextInfo(address, code, size, modInfo);

		return textInfo;
	}

	private Instruction _inst;

	private ArrayList<Operand> _operands;

	private boolean _nBit;
	private boolean _iBit;
	private boolean _xBit;
	// private boolean _bBit; /** base relative는 구현하지 않음 */
	private boolean _pBit;
	private boolean _eBit;
}
