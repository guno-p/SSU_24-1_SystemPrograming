/**
 * @author Enoch Jung (github.com/enochjung)
 * @file Directive.java
 * @date 2024-05-05
 * @version 1.0.0
 *
 * @brief 조교가 구현한 SIC/XE 어셈블러 코드 구조 샘플
 */

package directive;

/**
 * 지시어 종류.
 * 
 * <ul>
 * <li><code>Directive.START</code>
 * <li><code>Directive.CSECT</code>
 * <li><code>Directive.EXTDEF</code>
 * <li><code>Directive.EXTREF</code>
 * <li><code>Directive.BYTE</code>
 * <li><code>Directive.WORD</code>
 * <li><code>Directive.RESB</code>
 * <li><code>Directive.RESW</code>
 * <li><code>Directive.LTORG</code>
 * <li><code>Directive.EQU</code>
 * <li><code>Directive.END</code>
 * </ul>
 */
public enum Directive {
	START,
	CSECT,
	EXTDEF,
	EXTREF,
	BYTE,
	WORD,
	RESB,
	RESW,
	LTORG,
	EQU,
	END;

	/**
	 * 지시어 이름을 반환한다.
	 * 
	 * @return 지시어 이름 문자열
	 */
	public String getName() {
		return this.name();
	}

	/**
	 * 문자열을 <code>Directive</code>로 변환한다.
	 * 
	 * @param str 변환할 문자열
	 * @return <code>Directive</code>
	 * @throws RuntimeException 지시어가 아닌 문자열이 주어짐
	 */
	public static Directive stringToDirective(String str) throws RuntimeException {
		if ("START".equals(str))
			return Directive.START;
		if ("CSECT".equals(str))
			return Directive.CSECT;
		if ("EXTDEF".equals(str))
			return Directive.EXTDEF;
		if ("EXTREF".equals(str))
			return Directive.EXTREF;
		if ("BYTE".equals(str))
			return Directive.BYTE;
		if ("WORD".equals(str))
			return Directive.WORD;
		if ("RESB".equals(str))
			return Directive.RESB;
		if ("RESW".equals(str))
			return Directive.RESW;
		if ("LTORG".equals(str))
			return Directive.LTORG;
		if ("EQU".equals(str))
			return Directive.EQU;
		if ("END".equals(str))
			return Directive.END;
		throw new RuntimeException("illegal directive name (" + str + ")");
	}
}
