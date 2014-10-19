package Parser;
import java.util.*;

/* 		OO PARSER AND BYTE-CODE GENERATOR FOR TINY PL

Grammar for TinyPL (using EBNF notation) is as follows:

 program ->  decls stmts end
 decls   ->  int idlist ;
 idlist  ->  id { , id } 
 stmts   ->  stmt [ stmts ]
 cmpdstmt->  '{' stmts '}'
 stmt    ->  assign | cond | loop
 assign  ->  id = expr ;
 cond    ->  if '(' rexp ')' cmpdstmt [ else cmpdstmt ]
 loop    ->  while '(' rexp ')' cmpdstmt  
 rexp    ->  expr (< | > | =) expr
 expr    ->  term   [ (+ | -) expr ]
 term    ->  factor [ (* | /) term ]
 factor  ->  int_lit | id | '(' expr ')'

Lexical:   id is a single character; 
	      int_lit is an unsigned integer;
		 equality operator is =, not ==

Sample Program: Factorial

int n, i, f;
n = 4;
i = 1;
f = 1;
while (i < n) {
  i = i + 1;
  f= f * i;
}
end

   Sample Program:  GCD

int x, y;
x = 121;
y = 132;
while (x != y) {
  if (x > y) 
       { x = x - y; }
  else { y = y - x; }
}
end

 */

public class Parser {
	static Program p;
	public static void main(String[] args)  {
		System.out.println("Enter program and terminate with 'end'!\n");
		p = new Program();
		Code.output();
	}
}

class Program {
	Decls decls;
	Stmts stmts;
	Program()
	{
		int token = Lexer.lex();
		if(token == 17){
			 decls = new Decls();
		}
		token = Lexer.lex();
		if(token!=20)
		{	
			 stmts = new Stmts();
		}
		Code.lhm.put(Code.counter,": return");
		System.out.println("Parsing ends");
	}
}

class Decls {
	Idlist idlist;
	Decls(){
		if(Lexer.nextToken == 17)
		{
			idlist =new Idlist();
		}
	}
}

class Idlist {
	public static LinkedHashMap<Character,Integer> idlist_lhm = new LinkedHashMap<Character,Integer>();	
	Idlist()
	{
		int count=0;
		do{
			Lexer.lex();
			idlist_lhm.put(Lexer.ident,count++);  
		}
		while(Lexer.lex()!=0);
	}
}

class Stmt {
	Loop loop;
	Cond cond;
	Assign assign;
	
	 Stmt(){

		int current_token = Lexer.nextToken;
		
		if(current_token == 19){
			 loop = new Loop();
			}
		else if (current_token == 16){
			 cond =new Cond();
			}
		else if(current_token==14){
			 assign = new Assign();
			}
	}
} 

class Stmts {
	Stmt stmt;
	Stmts stmts;
	Stmts(){
		int token= Lexer.nextToken;
		if(token != 20)
		{	
			stmt = new Stmt();

			if(Lexer.nextToken==20)
				return;
			
			int next_token = Lexer.lex();

			if(next_token == 13)
			{
				return;
			}
			//TODO: check order of the following two if's
			
			if(next_token != 20 && next_token !=0)
			{
				stmts = new Stmts();		
			}

			if(next_token==19)
				return;
		}
	}
}

class Assign {
	Expr expr;
	 Assign()
	{
		int current_token = Lexer.nextToken;
		char ident=Lexer.ident;

		Code.store_at = Lexer.ident;
		int next_token = Lexer.lex();
		
		if(next_token != 7)
		{
			System.out.println("Assignment syntax did not match, please check ur logic");
			System.out.println("Token is "+next_token+"-->"+Lexer.ident);
			System.exit(0);
		}
		
		expr= new Expr();
		
		if(Lexer.nextToken==0){
			Code.llist.add(Code.counter+": istore_"+Idlist.idlist_lhm.get(ident)+"\n");
			Code.lhm.put(Code.counter,": istore_"+Idlist.idlist_lhm.get(ident)+"\n");
			Code.counter++;	
		}
	}
}

class Cond {
	Rexpr rexpr;
	Cmpdstmt cmpdstmt1;
	Cmpdstmt cmpdstmt2;
	Cond(){

		 rexpr = new Rexpr();

		Code.if_stack.add(Code.counter);
	
		Code.counter+=3;
		
		if(Lexer.lex()==12)			// '{'
		{
			cmpdstmt1 = new Cmpdstmt();			
		}	

		int next_token =Lexer.lex();
		
		if(next_token == 18)		// 'else'
		{			
			Code.lhm.put(Code.counter,": goto "+ "GT"+"\n");
			Code.else_stack.push(Code.counter);
			
			Code.counter+=3;
			
			int update_at = Code.if_stack.pop();
			String new_value = Code.lhm.get(update_at).replace("###", Integer.toString(Code.counter));

			Code.lhm.put(update_at, new_value);

			if(Lexer.lex()==12)		// '{'
				{
					cmpdstmt2 =new Cmpdstmt();

					int update_else = Code.else_stack.pop();
					String new_value_else = Code.lhm.get(update_else).replace("GT", Integer.toString(Code.counter));
					Code.lhm.put(update_else, new_value_else);

				}
		}
		else // if no else part, update the if accordingly
		{
			int update_at = Code.if_stack.pop();
			String new_value = Code.lhm.get(update_at).replace("###", Integer.toString(Code.counter));
			Code.lhm.put(update_at, new_value);
			
			if(next_token == 13)
			{
				update_at = Code.if_stack.peek();
				 new_value = Code.lhm.get(update_at).replace("###", Integer.toString(Code.counter));
				Code.lhm.put(update_at, new_value);
			}
		}		
	}
}

class Loop {
	Rexpr rexpr;
	Cmpdstmt cmpdstmt;
	Loop()
	{
		
		int goto_marker = Code.counter;
		rexpr =new Rexpr();

		Code.while_stack.add(Code.counter);
	
		Code.counter+=3;
			
		if(Lexer.lex()==12) 	// '{'
		{
			 cmpdstmt = new Cmpdstmt();

			Code.lhm.put(Code.counter,": goto "+goto_marker+"\n");
			Code.counter+=3;

			int update_at = Code.while_stack.pop();
			String new_value = Code.lhm.get(update_at).replace("###", Integer.toString(Code.counter));

			Code.lhm.put(update_at, new_value);
		}	
	}	
}

class Cmpdstmt {
	Stmts stmts;
	Cmpdstmt()
	{
		Lexer.lex();
		stmts = new Stmts();
	}
}

class Rexpr {
	Expr expr1;
	Expr expr2;
	Rexpr(){
		//operator1
		Lexer.lex();
		expr1 = new Expr();

		//operand  
		String inequality = null;
		switch (Lexer.nextToken) 
		{
		case Token.LESSER_OP: // <
			inequality ="if_cmpge";
			break;
		case Token.GREATER_OP: // >
			inequality ="if_cmple";
			break;
		case Token.NOT_EQ: // '!='
			inequality ="if_cmpeq";
			break;
		case Token.ASSIGN_OP: // '='
			inequality ="if_cmpne";
			break;
		default:
			break;
		}

		//opeartor2
		expr2= new Expr();

		//display instruction
		Code.lhm.put(Code.counter ,": " +inequality+" "+"###\n");
	}
}

class Expr {
	Term term;
	Expr expr;
	Expr() 
	{	
		term= new Term();

		int next_token=Lexer.nextToken;

		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) 
		{
			 expr = new Expr();

			if(next_token == Token.ADD_OP){ 
				//display add instruction
				Code.llist.add(Code.counter + ": iadd\n");
				Code.lhm.put(Code.counter , ": iadd\n");
				Code.counter++;
			}
			else{
				//display subtract instruction
				Code.llist.add(Code.counter + ": isub\n");
				Code.lhm.put(Code.counter , ": isub\n");
				Code.counter ++;
			}
		}
	}
}

class Term {  
	Factor factor;
	Term term;
	Term() {

		 factor= new Factor();

		char operator =Lexer.nextChar;

		int next_token=Lexer.lex();

		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP)
		{
			 term = new Term();
		
			//TODO: correct loop to if(term == 15)
			if(next_token == Token.MULT_OP){ 
			//Display multiply intruction
				Code.llist.add(Code.counter + ": imul\n");
				Code.lhm.put(Code.counter, ": imul\n");
				Code.counter++;
			}
			else{
			//Display divide intruction
				Code.llist.add(Code.counter + ": idiv\n");
				Code.lhm.put(Code.counter , ": idiv\n");
				Code.counter ++;
			}
		}	
	}
}

class Factor {  
	Expr expr;
	Factor()
	{
		int i = Lexer.lex();
		
		switch (i) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;

			if (i > 6  &&  i < 128){
				Code.llist.add(Code.counter+": bipush "+ i +"\n");
				Code.lhm.put(Code.counter, ": bipush "+ i +"\n");
				Code.counter+=2;
			}
			else if(i > 127){
					Code.llist.add(Code.counter+": sipush "+ i +"\n");
					Code.lhm.put(Code.counter,": sipush "+ i +"\n");
					Code.counter+=3;
				}
			else{	
				Code.llist.add(Code.counter+": iconst_"+ i +"\n");
				Code.lhm.put(Code.counter,": iconst_"+ i +"\n");
				Code.counter++;
			}
			break;	
		case Token.ID:
			Code.llist.add(Code.counter+": iload_"+Idlist.idlist_lhm.get(Lexer.ident)+"\n");
			Code.lhm.put(Code.counter,": iload_"+Idlist.idlist_lhm.get(Lexer.ident)+"\n");
			Code.counter++;			
			break;
		case Token.LEFT_PAREN: // '('
			expr = new Expr();
			break;
		default:
			break;
		}
	}
}

class Code {
	static int counter=0;
	static char store_at;
	static Stack<Integer> while_stack = new Stack<Integer>();
	static Stack<Integer> if_stack = new Stack<Integer>();
	static Stack<Integer> else_stack = new Stack<Integer>();
	static LinkedList<String> llist =new LinkedList<String>();
	static LinkedHashMap<Integer,String> lhm = new LinkedHashMap<Integer, String>();
	public static void output()
	{
		System.out.println("\n");
		for (Map.Entry<Integer, String> entry : lhm.entrySet()){
			System.out.print(entry.getKey()+entry.getValue());
		}
	}	
}
