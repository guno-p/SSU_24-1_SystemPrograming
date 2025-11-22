/*
*   parser program by
*   author Enoch Jung(github.com / enochjung)
* 
*   assembler program by
*   Park KunHo 20171969
*/

#ifndef __MY_ASSEMBLER_PARSING_H__
#define __MY_ASSEMBLER_PARSING_H__

#define MAX_INST_TABLE_LENGTH 256
#define MAX_INPUT_LINES 5000
#define MAX_TABLE_LENGTH 5000
#define MAX_OPERAND_PER_INST 3
#define MAX_OBJECT_CODE_STRING 74
#define MAX_OBJECT_CODE_LENGTH 5000
#define MAX_CONTROL_SECTION_NUM 10

// 새로운 define
#define MAX_MODIFICATIONS 100
#define MAX_COMMENT_SIZE 100

#define ERR_FILE_IO_FAIL -100
#define ERR_ALLOCATION_FAIL -200
#define ERR_ARRAY_OVERFLOW -1000
#define ERR_ILLEGAL_OPERATOR -10200
#define ERR_ILLEGAL_OPERAND_FORMAT -10300

// 새로운 error define
#define ERR_NAME_LENGTH_OVER -20000
#define ERR_DUPLICATE_SYMBOL -30000
#define ERR_INVALID_REGISTER_NAME -40000
#define ERR_INVALID_EXPRESSION -50000
#define ERR_INVALID_LITERAL_FORMAT -55000
#define ERR_SYMBOL_NOT_FOUND -60000
#define ERR_OBJECTCODE_LIMIT_EXCEEDED -70000
#define ERR_MISSING_END_DIRECTIVE -80000
#define ERR_MISSING_LABEL -90000
#define ERR_MISSING_OPERAND -95000

/**
 * @brief 한 개의 SIC/XE instruction을 저장하는 구조체
 *
 * @details
 * 기계어 목록 파일(inst_table.txt)에 명시된 SIC/XE instruction 하나를
 * 저장하는 구조체. 라인별로 하나의 instruction을 저장하고 있는 instruction 목록
 * 파일로부터 정보를 받아와서 생성한다.
 */
typedef struct _inst {
    char str[10];     /** instruction 이름 */
    unsigned char op; /** instruction의 opcode */
    int format;       /** instruction의 format */
    int ops;          /** instruction이 가지는 operator 구분 */
} inst;

/**
 * @brief 소스코드 한 줄을 분해하여 저장하는 구조체
 *
 * @details
 * 원할한 assem을 위해 소스코드 한 줄을 label, operator, operand, comment로
 * 파싱한 후 이를 저장하는 구조체. 필드의 `operator`는 renaming을 허용한다.
 */
typedef struct _token {
    char *label;    /** label을 가리키는 포인터 */
    char *operatr; /** operator를 가리키는 포인터 */
    char *operand[MAX_OPERAND_PER_INST]; /** operand들을
                                            가리키는 포인터 배열 */
    char *comment; /** comment를 가리키는 포인터 */

    char nixbpe;   /** 특수 bit 정보 */
    int LC; /*Location Counter*/

} token;

/**
 * @brief 하나의 심볼에 대한 정보를 저장하는 구조체
 *
 * @details
 * SIC/XE 소스코드에서 얻은 심볼을 저장하는 구조체이다. 기존에 정의된 `name` 및
 * `addr`는 필수로 사용해야 한다. 필드가 더 필요한 경우 구조체 내에 필드를
 * 추가하는 것을 허용한다.
 */
typedef struct _symbol {
    char name[10]; /** 심볼의 이름 */
    int addr;      /** 심볼의 주소 */

    char SECT_NAME[10]; /* 소속 섹션 이름 */
    int section_length; /* 섹션의 크기 - 첫 레이블만... */
    int flag_add_address; /* 1이면 +1 출력*/

} symbol;

/**
 * @brief 하나의 리터럴에 대한 정보를 저장하는 구조체
 *
 * @details
 * SIC/XE 소스코드에서 얻은 리터럴을 저장하는 구조체이다. 기존에 정의된 literal
 * 및 addr는 필수로 사용하고, field가 더 필요한 경우 구조체 내에 field를
 * 추가하는 것을 허용한다. addr 필드는 리터럴의 값을 저장하는 것이 아닌 리터럴의
 * 주소를 저장하는 필드임을 유의하라.
 */
typedef struct _literal {
    char literal[20]; /** 리터럴의 표현식 */
    int addr;         /** 리터럴의 주소 */

    char SECT_NAME[10]; /* 소속 섹션 이름 */

} literal;

/**
 * @brief 오브젝트 코드 전체에 대한 정보를 담는 구조체
 *
 * @details
 * 오브젝트 코드 전체에 대한 정보를 담는 구조체이다. Header Record, Define
 * Record, Modification Record 등에 대한 정보를 모두 포함하고 있어야 한다. 이
 * 구조체 하나만으로 object code를 충분히 작성할 수 있도록 구조체를 직접
 * 정의해야 한다.
 */
typedef struct _object_code {
    int linenumber; // 라인의 갯수
    char* obj_line[MAX_OBJECT_CODE_LENGTH]; // 각 라인은 하나의 objcode 라인

} object_code;

typedef struct _ModificationRecord {
    // assem2 에서 쓰이는 Modification Record 저장 구조체
    char label[10];  // 변경할 심볼의 이름
    int position;    // 변경할 위치
    int length;      // 변경할 길이
    char operation;  // 수행 연산 + / -
} Modification_Record;


int init_inst_table(inst *inst_table[], int *inst_table_length,
                    const char *inst_table_dir);
int init_input(char *input[], int *input_length, const char *input_dir);
int assem_pass1(const inst *inst_table[], int inst_table_length,
                const char *input[], int input_length, token *tokens[],
                int *tokens_length, symbol *symbol_table[],
                int *symbol_table_length, literal *literal_table[],
                int *literal_table_length);
int token_parsing(const char *input, token *tok, const inst *inst_table[],
                  int inst_table_length);
int search_opcode(const char *str, const inst *inst_table[],
                  int inst_table_length);
int make_opcode_output(const char *output_dir, const token *tokens[],
                       int tokens_length, const inst *inst_table[],
                       int inst_table_length);
int assem_pass2(const token *tokens[], int tokens_length,
                const inst *inst_table[], int inst_table_length,
                const symbol *symbol_table[], int symbol_table_length,
                const literal *literal_table[], int literal_table_length,
                object_code *obj_code);
int make_symbol_table_output(const char *symbol_table_dir,
                             const symbol *symbol_table[],
                             int symbol_table_length);
int make_literal_table_output(const char *literal_table_dir,
                              const literal *literal_table[],
                              int literal_table_length);
int make_objectcode_output(const char *objectcode_dir,
                           const object_code *obj_code);


static int add_inst_to_table(inst* inst_table[], int* inst_table_length, const char* buffer);
static int token_operand_parsing(const char* operand_input, int operand_input_length, char* operand[]);
static int write_opcode_output(FILE* fp, const token* tok, const inst* inst_table[], int inst_table_length);

// Modification Record 관리 함수
void add_modification_record(Modification_Record* modifications, int* modification_count, int address, int length, const char* symbolName);
void sub_modification_record(Modification_Record* modifications, int* modification_count, int address, int length, const char* symbolName);

// Symbol / Literal Table 관리 함수
static int search_SYMTAB(const char* str, symbol* symbol_table[], int symbol_table_length);
static int search_SYMTAB_CUR_section(const char* str, symbol* symbol_table[], int symbol_table_length, const char* CUR_section);
static int insert_SYMTAB(const char* str, symbol* symbol_table[], int* symbol_table_length, int LC, const char* CUR_SECT, int SECT_flag);
static int insert_SECLEN(const char* str, symbol* symbol_table[], int symbol_table_length, int Section_length);
static int search_LITTAB(const char* str, literal* literal_table[], int literal_table_length);
static int insert_LITTAB(const char* str, literal* literal_table[], int* literal_table_length);
static int ORG_LITTAB(const char* str, literal* literal_table[], int literal_table_length, int* LC);

// 유틸리티 함수
static void safe_strcat(char* dest, const char* src, size_t buf_size);
static int generate_obj_code_line(char* buffer, const char* type, const token* tok, const symbol* symbol_table[], int symbol_table_length, object_code* obj_code);
static int generate_obj_code_line_end(char* buffer, const char* type, const token* tok, const symbol* symbol_table[], int symbol_table_length, object_code* obj_code, char* END_operand);
static int generate_obj_code_line_text(char* buffer, const char* type, const token* tok, const symbol* symbol_table[], int symbol_table_length, object_code* obj_code, int first_text_LC);
static int write_obj_code(char* buffer, object_code* obj_code);
static int get_register_code(const char* operand);
static int insert_buffer_object_code(char* buffer, int object_code, int num_bytes);

// assem2 리터럴 할당 함수
int process_literals(const char* section, literal* literal_table[], int literal_table_length, char* buffer, const token* tok, const symbol* symbol_table[], int symbol_table_length, object_code* obj_code, unsigned int* first_text_LC);

#endif
