/**
 * @author Enoch Jung (github.com/enochjung)
 * @file ControlSection.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import directive.Directive;
import instruction.*;
import literal.*;
import symbol.*;
import token.*;
import token.operand.*;
import numeric.Numeric;

public class ControlSection {
	/**
	 * pass1 작업을 수행한다. 기계어 목록 테이블을 통해 소스 코드를 토큰화하고, 심볼 테이블 및 리터럴 테이블을 초기화환다.
	 * 
	 * @param instTable 기계어 목록 테이블
	 * @param input     하나의 control section에 속하는 소스 코드. 마지막 줄은 END directive를 강제로
	 *                  추가하였음.
	 * @throws RuntimeException 소스 코드 컴파일 오류.
	 */
	public ControlSection(InstructionTable instTable, ArrayList<String> input) throws RuntimeException {
		List<StringToken> stringTokens = input.stream()
				.map(x -> new StringToken(x))
				.collect(Collectors.toList());
		// StringToken 의 List -> StringToken 인스턴스는... 레이블 오퍼레이터 오퍼랜드 코멘트 / nixpe 포함

		SymbolTable symTab = new SymbolTable(); // 심볼테이블 생성
		LiteralTable litTab = new LiteralTable(); // 리터럴테이블 생성

		ArrayList<Token> tokens = new ArrayList<Token>(); // 토큰 리스트 생성

		int locctr = 0;
		for (StringToken stringToken : stringTokens) { // 라인마다 수행
			if (stringToken.getOperator().isEmpty()) { // 연산자가 없는 경우
				boolean isLabelEmpty = stringToken.getLabel().isEmpty(); // true : 레이블 없음
				boolean isOperandEmpty = stringToken.getOperands().isEmpty(); // true : operand 없음
				if (!isLabelEmpty || !isOperandEmpty) // 연산자가 없는데, 레이블이나 피연산자가 있음
					throw new RuntimeException("missing operator\n\n" + stringToken.toString());
				continue;
			}
			String operator = stringToken.getOperator().get(); // 연산자

			Optional<Instruction> optInst = instTable.search(operator); // instTable에서 찾아보고
			// 있다 -> instruction | 없다 -> directive
			boolean isOperatorInstruction = optInst.isPresent();
			if (isOperatorInstruction) { // instruction인 경우 토큰 처리하고 리스트에 삽입
				Token token = handlePass1InstructionStep(optInst.get(), stringToken, locctr, symTab, litTab);
				locctr += token.getSize();
				tokens.add(token);
				// System.out.println(token.toString()); /** 디버깅 용도 */
			} else { // directive인 경우 토큰 처리하고 리스트에 삽입
				Token token = handlePass1DirectiveStep(stringToken, locctr, symTab, litTab);
				locctr += token.getSize();
				tokens.add(token);
				// System.out.println(token.toString()); /** 디버깅 용도 */
			}
		} // 라인마다 수행 끝

		_tokens = tokens;
		_symbolTable = symTab;
		_literalTable = litTab;
	}

	/**
	 * pass2 작업을 수행한다. pass1에서 초기화한 토큰 테이블, 심볼 테이블 및 리터럴 테이블을 통해 오브젝트 코드를 생성한다.
	 * 
	 * @return 해당 control section에 해당하는 오브젝트 코드 객체
	 * @throws RuntimeException 소스 코드 컴파일 오류.
	 */
	public ObjectCode buildObjectCode() throws RuntimeException {
		ObjectCode objCode = new ObjectCode();
		Optional<Symbol> optRepSymbol = _symbolTable.getRepSymbol();
		if (optRepSymbol.isEmpty())
			throw new RuntimeException("invalid operation");
		Symbol repSymbol = optRepSymbol.get();

		for (Token token : _tokens) {
			if (token instanceof InstructionToken) { // inst PASS2
				handlePass2InstructionStep(objCode, (InstructionToken) token, _symbolTable, _literalTable);
			} else if (token instanceof DirectiveToken) { // direc PASS2
				handlePass2DirectiveStep(objCode, (DirectiveToken) token, repSymbol, _symbolTable, _literalTable);
			} else
				throw new RuntimeException("invalid operation");
		}

		return objCode;
	}

	/**
	 * 심볼 테이블 객체의 정보를 문자열로 반환한다. Assembler.java에서 심볼 테이블 출력 용도로 사용한다.
	 * 
	 * @return 심볼 테이블의 정보를 담은 문자열
	 */
	public String getSymbolString() {
		return _symbolTable.toString();
	}

	/**
	 * 리터럴 테이블 객체의 정보를 문자열로 반환한다. Assembler.java에서 리터럴 테이블 출력 용도로 사용한다.
	 * 
	 * @return 리터럴 테이블의 정보를 담은 문자열
	 */
	public String getLiteralString() {
		return _literalTable.toString();
	}

	/**
	 * pass1에서 operator가 instruction에 해당하는 경우에 대해서 처리한다. label 및 operand에 출현한 심볼 및
	 * 리터럴을 심볼 테이블 및 리터럴 테이블에 추가하고, 문자열 형태로 파싱된 토큰을 InstructionToken으로 가공하여 반환한다.
	 * 
	 * @param inst   기계어 정보
	 * @param token  문자열로 파싱된 토큰
	 * @param locctr location counter 값
	 * @param symTab 심볼 테이블
	 * @param litTab 리터럴 테이블
	 * @return 가공된 InstructionToken 객체
	 * @throws RuntimeException 잘못된 명령어 사용 방식.
	 */
	private static InstructionToken handlePass1InstructionStep(Instruction inst, StringToken token, int locctr,
			SymbolTable symTab, LiteralTable litTab) throws RuntimeException {
		Instruction.Format format = inst.getFormat();
		Instruction.OperandType operandType = inst.getOperandType();

		int size = 0;
		ArrayList<Operand> operands = new ArrayList<>();
		boolean isN = token.isN();
		boolean isI = token.isI();
		boolean isX = token.isX();
		boolean isP = token.isP();
		boolean isE = token.isE();

		switch (format) {
			case TWO:
				// TODO: size = 2?;
				size = 2;
				break;

			case THREE_OR_FOUR:
				// TODO: size = 3 or 4?;
				if (token.isE()) size = 4;
				else size = 3;
				break;

			default:
				throw new UnsupportedOperationException("not fully support InstructionInfo.Format");
		}

		// TODO: label을 심볼 테이블에 추가하기.
		token.getLabel().ifPresent(label -> {
			symTab.put(label, locctr);
		});

		switch (operandType) {
			case NO_OPERAND:
				// TODO: operand가 없어야 하는 경우에 대해서 처리하기.
				// like \tJSUB\tCOMMENT
				isP = false;
				break;

			case MEMORY:
				// TODO: operand로 MEMORY 하나만 주어져야 하는 경우에 대해서 처리하기.
				String opd0 = token.getOperands().get(0);
				Operand.MemoryType memoryType = Operand.MemoryType.distinguish(opd0);
				switch (memoryType) {
					case NUMERIC:
						// TODO: operand로 상수 혹은 심볼이 주어지는 경우에 대해서 처리하기.
						// 심볼이고, 처음 나왔으면 삽입 | 처음 나오는게 아니면 주소 할당
						// 문자로 시작하면 심볼 |
						Numeric numericOperand;
						// 숫자 판별: 숫자로만 구성되어 있으면 true, 아니면 false
						if (opd0.matches("-?\\d+")) { // 정규 표현식을 사용하여 숫자인지 판단
							// 숫자 리터럴로 Numeric 객체 생성
							numericOperand = new Numeric(opd0);

						} else {
							// 심볼이면 심볼 테이블에서 검색 또는 새로 추가
							if (symTab.search(opd0).isPresent()) {
							}
							else {
								symTab.put(opd0);
							}
							Symbol operand_symbol = symTab.search(opd0).get();
							numericOperand = new Numeric(0, operand_symbol);
						}
						NumericOperand token_numericOperand = new NumericOperand(numericOperand);
						operands.add(token_numericOperand);
						break;

					case LITERAL:
						// TODO: operand로 리터럴이 주어지는 경우에 대해서 처리하기.
						// 리터럴테이블 삽입
						Optional<Literal> optionalLiteral = litTab.search(opd0);  // LiteralTable에서 리터럴 검색
						if (optionalLiteral.isPresent()) {
						}
						else {
							litTab.putLiteral(opd0);
						}
						Literal literaloperand = litTab.search(opd0).get();
						LiteralOperand token_literaloperand = new LiteralOperand(literaloperand);
						operands.add(token_literaloperand);
						break;

					default:
						throw new UnsupportedOperationException("not fully support Operand.MemoryType");
				}
				break;

			case REG:
				// TODO: operand로 REGISTER 하나만 주어져야 하는 경우에 대해서 처리하기.
				RegisterOperand registerOperand = new RegisterOperand(Operand.Register.stringToRegister(token.getOperands().get(0)));
				operands.add(registerOperand);
				break;

			case REG1_REG2:
				// TODO: operand로 REGISTER 두개가 주어져야 하는 경우에 대해서 처리하기.
				String regName1 = token.getOperands().get(0);
				String regName2 = token.getOperands().get(1);
				RegisterOperand regOperand1 = new RegisterOperand(Operand.Register.stringToRegister(regName1));
				RegisterOperand regOperand2 = new RegisterOperand(Operand.Register.stringToRegister(regName2));
				operands.add(regOperand1);
				operands.add(regOperand2);
				break;

			default:
				throw new UnsupportedOperationException("not fully support InstructionInfo.OperandType");
		}

		return new InstructionToken(token.toString(), locctr, size, inst, operands, isN, isI, isX, isP, isE);
	}

	/**
	 * pass1에서 operator가 directive에 해당하는 경우에 대해서 처리한다. label 및 operand에 출현한 심볼을 심볼
	 * 테이블에 추가하고, 주소가 지정되지 않은 리터럴을 리터럴 테이블에서 찾아 주소를 할당하고, 문자열 형태로 파싱된 토큰을
	 * DirectiveToken으로 가공하여 반환한다.
	 * 
	 * @param token  문자열로 파싱된 토큰
	 * @param locctr location counter 값
	 * @param symTab 심볼 테이블
	 * @param litTab 리터럴 테이블
	 * @return 가공된 DirectiveToken 객체
	 * @throws RuntimeException 잘못된 지시어 사용 방식.
	 */
	private static DirectiveToken handlePass1DirectiveStep(StringToken token, int locctr, SymbolTable symTab,
			LiteralTable litTab) throws RuntimeException {
		String operator = token.getOperator().get();

		Directive directive;
		try {
			directive = Directive.stringToDirective(operator);
		} catch (RuntimeException e) {
			throw new RuntimeException(e.getMessage() + "\n\n" + token.toString());
		} // illegal directive name

		int size = 0;
		ArrayList<Operand> operands = new ArrayList<>();

		switch (directive) {
			case START:
				// TODO: START인 경우에 대해서 pass1 처리하기.
				size = 0; // START does not occupy any space.
				symTab.putRep(token.getLabel().orElseThrow(() -> new RuntimeException("Label required for START")), locctr);
				NumericOperand numericOperand = new NumericOperand(new Numeric(token.getOperands().get(0)));
				operands.add(numericOperand);
				break;

			case CSECT:
				// TODO: CSECT인 경우에 대해서 pass1 처리하기.
				size = 0; // CSECT does not occupy any space.
				symTab.putRep(token.getLabel().orElseThrow(() -> new RuntimeException("Label required for CSECT")), locctr);
				break;

			case EXTDEF:
				// TODO: EXTDEF인 경우에 대해서 pass1 처리하기.
				// operand 전부 심볼에 때려박기
				for (String operand : token.getOperands()) {
					symTab.put(operand); // ADDRESS_NOT_ASSIGNED;
					Symbol operand_symbol = symTab.search(operand).get();
					NumericOperand token_numericOperand = new NumericOperand(new Numeric(0, operand_symbol));
					operands.add(token_numericOperand);
				}
				size = 0;
				break;

			case EXTREF:
				// TODO: EXTREF인 경우에 대해서 pass1 처리하기.
				for (String operand : token.getOperands()) {
					symTab.putRefer(operand);
					Symbol operand_symbol = symTab.search(operand).get();
					NumericOperand token_numericOperand = new NumericOperand(new Numeric(0, operand_symbol));
					operands.add(token_numericOperand);
				}
				size = 0;
				break;

			case BYTE:
				// TODO: BYTE인 경우에 대해서 pass1 처리하기.
				String byte_operand = token.getOperands().get(0);
				symTab.put(token.getLabel().orElseThrow(() -> new RuntimeException("Label required for BYTE")), locctr);
				operands.add(new NumericOperand(new Numeric(byte_operand)));
				if (byte_operand.startsWith("C'") && byte_operand.endsWith("'")) {
					size =  byte_operand.length() - 3;
				} else if (byte_operand.startsWith("X'") && byte_operand.endsWith("'")) {
					size = (byte_operand.length() - 3) / 2;
				} else if (byte_operand.matches("-?\\d+")) {
					size = 1;
				} else {
					throw new RuntimeException("Invalid format for BYTE operand: " + byte_operand);
				}
				break;

			case WORD:
				// TODO: WORD인 경우에 대해서 pass1 처리하기.
				String word_operand = token.getOperands().get(0);
				symTab.put(token.getLabel().orElseThrow(() -> new RuntimeException("Label required for WORD")), locctr);
				operands.add(new NumericOperand(new Numeric(word_operand, symTab, locctr)));
				size = 3;

				break;

			case RESB:
				// TODO: RESB인 경우에 대해서 pass1 처리하기.
				symTab.put(token.getLabel().orElseThrow(() -> new RuntimeException("Label required for RESB")), locctr);
				size = Integer.parseInt(token.getOperands().get(0));
				operands.add(new NumericOperand(new Numeric(token.getOperands().get(0))));
				break;

			case RESW:
				// TODO: RESW인 경우에 대해서 pass1 처리하기.
				symTab.put(token.getLabel().orElseThrow(() -> new RuntimeException("Label required for RESW")), locctr);
				size = 3 * Integer.parseInt(token.getOperands().get(0));
				operands.add(new NumericOperand(new Numeric(token.getOperands().get(0))));
				break;

			case LTORG:
				// TODO: LTORG인 경우에 대해서 pass1 처리하기.
				size = litTab.assignAddress(locctr);
				break;

			case EQU:
				// TODO: EQU인 경우에 대해서 pass1 처리하기.
				String equValue = token.getOperands().get(0); // "*" "BUFEND-BUFFER"
				// 레이블은 심볼테이블에 넣기
				symTab.put(token.getLabel().orElseThrow(() -> new RuntimeException("Label required for EQU")), equValue, locctr);
				operands.add(new NumericOperand(new Numeric(equValue, symTab, locctr)));
				size = 0;
				break;

			case END:
				// TODO: END인 경우에 대해서 pass1 처리하기.
				// 모든 섹션의 마지막에 \tEND\tFIRST삽입했음
				size = litTab.assignAddress(locctr);
				if (symTab.search(token.getOperands().get(0)).isPresent()) { // 만약 symTab에 FIRST 존재하면 삽입
					operands.add(new NumericOperand(new Numeric(0, symTab.search(token.getOperands().get(0)).get())));
				}
				// FIRST
				break;

			default:
				throw new UnsupportedOperationException("not fully support Directive");
		}

		return new DirectiveToken(token.toString(), locctr, size, directive, operands);
	}

	/**
	 * pass2에서 operator가 instruction인 경우에 대해서 오브젝트 코드에 정보를 추가한다.
	 * 
	 * @param objCode 오브젝트 코드 객체
	 * @param token   InstructionToken 객체
	 * @throws RuntimeException 잘못된 심볼 객체 변환 시도.
	 */
	private static void handlePass2InstructionStep(ObjectCode objCode, InstructionToken token, SymbolTable symTab, LiteralTable litTab) throws RuntimeException {
		Token.TextInfo textInfo = token.getTextInfo(symTab, litTab); // 그러면 지금 Token에 Text info가 있다
		// 그냥 symTab, litTab 넘겨서 offset 계산했음
		// 라인 정보랑 mod 정보 textInfo 에 담았음
		objCode.addText(textInfo.address, textInfo.code, textInfo.size);
		// Text 배열에 추가
		if (textInfo.mod.isEmpty())
			return; // mod 없으면 리턴
		Token.ModificationInfo modInfo = textInfo.mod.get();
		// mod 돌면서 추가
		for (String refer : modInfo.refers)
			objCode.addModification(refer, modInfo.address, modInfo.sizeHalfByte);
	}

	/**
	 * pass2에서 operator가 directive인 경우에 대해서 오브젝트 코드에 정보를 추가한다.
	 * 
	 * @param objCode      오브젝트 코드 객체
	 * @param token        DirectiveToken 객체
	 * @param repSymbol    control section 명칭 심볼
	 * @param literalTable 리터럴 테이블
	 * @throws RuntimeException 잘못된 지시어 사용 방식.
	 */
	private static void handlePass2DirectiveStep(ObjectCode objCode, DirectiveToken token, Symbol repSymbol,
												 SymbolTable symbolTable, LiteralTable literalTable) throws RuntimeException {
		// 그냥 귀찮아서 심볼테이블도 가져왔음
		// 어차피 SECT마다 독립적으로 심볼테이블 가짐
		Directive directive = token.getDirective();
		String sectionName = repSymbol.getName();

		ArrayList<Operand> operands;
		NumericOperand numOperand;
		Numeric num;
		int address;
		int size;

		switch (directive) {
			case START:
				numOperand = (NumericOperand) token.getOperands().get(0);
				objCode.setSectionName(sectionName);
				objCode.setStartAddress(numOperand.getNumeric().getInteger());
				break;

			case CSECT:
				objCode.setSectionName(sectionName);
				objCode.setStartAddress(0);
				break;

			case EXTDEF:
				// TODO: EXTDEF인 경우에 대해서 pass2 처리하기.
				// objCode.addDefineSymbol(?, ?);
				operands = token.getOperands();
				for (Operand operand : operands) {
					NumericOperand op = (NumericOperand) operand;
					objCode.addDefineSymbol(op.getNumeric().getName().orElseThrow(
									() -> new RuntimeException("Expected numeric operand with name for EXTDEF")),
							symbolTable.search(op.getNumeric().getName().get()).get().getAddress().get().getInteger());
				} // 피연산자에 대해 EXTDEF 심볼들을 오브젝트 인스턴스 D 에 넘기기
				break;

			case EXTREF:
				operands = token.getOperands();
				for (Operand operand : operands) {
					numOperand = (NumericOperand) operand;
					num = numOperand.getNumeric();
					String symbolName = num.getName().get();
					objCode.addReferSymbol(symbolName);
				} // EXTREF 심볼들을 오브젝트 인스턴스 R 에 넘기기
				break;

			case BYTE:
				// TODO: BYTE인 경우에 대해서 pass2 처리하기.
				// objCode.addText(?, ?, ?);
				numOperand = (NumericOperand) token.getOperands().get(0);
				objCode.addText(token.getAddress(), numOperand.getNumeric(), numOperand.getNumeric().getSize());
				// BYTE 의 정보 오브젝트 인스턴스에 T 에 넘기기
				break;

			case WORD:
				// TODO: WORD인 경우에 대해서 pass2 처리하기.
				ArrayList<String> refers = new ArrayList<>();
				for (int i = 0;  i < token.getOperands().size(); i++) {
					NumericOperand numericOperand = (NumericOperand) token.getOperands().get(i);
					for (int j = 0 ; j < numericOperand.getNumeric().relativeMap_numbers() ; j++) {
						String refer = numericOperand.getNumeric().getReferSymbolsString().get(j).toString();
						refers.add(refer);
						objCode.addModification(refer, token.getAddress(), token.getSize() * 2);
					} // WORD에 대해 수식이 등장한다면... modification 정보 오브젝트 인스턴스에 M 으로 넘기기
				}
				numOperand = (NumericOperand) token.getOperands().get(0);
				objCode.addText(token.getAddress(), numOperand.getNumeric(), 3); // WORD의 사이즈를 3을 가정 - 만약 크기 달라진다면...?

				break;

			case LTORG:
				// TODO: LTORG인 경우에 대해서 pass2 처리하기.
				// objCode.addText(?, ?, ?);
				for (Literal literal : literalTable.getAllLiterals()) {
					if (literal.getAddress().isPresent() && !literal.Is_Written()) {
						int litAddress = literal.getAddress().get();
						Numeric litValue = literal.getValue();
						int litSize = literal.getSize();

						objCode.addText(litAddress, litValue, litSize);
						literal.set_written_flag();
					} // 리터럴 중 아직 쓰이지 않은것을 T에 쓰고 written flag를 true로 설정
					  // true인 경우 - T에 쓰지 않음
				}
				break;

			case END:
				// TODO: END인 경우에 대해서 pass2 처리하기.
				for (Literal literal : literalTable.getAllLiterals()) { // LTORG 못한 리터럴 처리
					if (literal.getAddress().isPresent() && !literal.Is_Written()) {
						int litAddress = literal.getAddress().get();
						Numeric litValue = literal.getValue();
						int litSize = literal.getSize();

						objCode.addText(litAddress, litValue, litSize);
						literal.set_written_flag();
					} // 모든 코드의 끝 부분에서 LTORG와 동일작업 수행
				}
				if (!token.getOperands().isEmpty()) {
					NumericOperand END_operand_numeric = (NumericOperand) token.getOperands().get(0);
					String END_operand = END_operand_numeric.getNumeric().getName().get();
					objCode.setInitialPC(symbolTable.search(END_operand).get().getAddress().get().getInteger());
				} // 만약 END Directive의 피연산자가 PASS1에서 심볼테이블에 존재했을 경우...
				  // 현재 섹션에 END의 피연산자 심볼이 존재해서 토큰 피연산자로 넘겨받았음
				  // 따라서 오브젝트 인스턴스 E에 붙여질 InitialPC 를 해당 심볼의 주소로 설정
				objCode.setProgramLength(token.getAddress() + token.getSize());
				// 섹션의 총 사이즈 설정 크기를 계산

				break;

			case RESB:
				objCode.add_clear_Text();
			case RESW:
				objCode.add_clear_Text();
				// 줄바꿈에 해당하는 더미 Text 배열 생성
			case EQU:
				// 처리할 동작이 없음.
				break;

			default:
				throw new UnsupportedOperationException("not fully support Directive");
		}
	}

	private final List<Token> _tokens;
	private final SymbolTable _symbolTable;
	private final LiteralTable _literalTable;
}
