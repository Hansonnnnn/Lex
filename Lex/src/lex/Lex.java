package lex;

import java.util.Arrays;

public class Lex {
    /**
     * inputStream 输入字符串序列
     * tokens 每个单独的token序列
     * nextChar 从输入流读入的下一个字符
     * len 输入流字符序列的长度
     *
     * isp-inputStreamPointer 输入字符串序列的下标指针
     * tp-tokenArrayPointer
     */
    private char inputStream[], token[], keyword[];
    private String stateTable[][];
    private char nextChar;
    private int len;

    private int isp;
    private int tp;
    private int kwp;
    private int state;

    private String stateTableFilePath;

    private static String KEY_WORD_TABLE[] = {
           "abstract","boolean","break","byte","case","catch","char","class",
            "continue","default","do","double","else","extends","false","final",
            "finally","float","for","if","implements","import","instanceof","int",
            "interface","long","native","new","null","package","private","protected",
            "public","return","short","static","super","switch","synchronized","this",
            "throw","throws","transient","try","true","void","volatile","while"
    };

    private static String ARITHMETIC_OPERATOR_TABLE[] = {
            "+","-","*","/","%",">","<","="
    };
    private static String MARK_OPERATOR_TABLE[] = {
            "(",")","{","}","|","&","!",";",",",".","[","]","’","”","?",":","~","^","\"","'"
    };

    private static String TerminalState[] = {
            "1","2"
    };


    public Lex(String stateTableFilePath, String inputSequenceFilePath){
        keyword = new char[50];
        token = new char[50];
        isp = 0;
        tp = 0;
        kwp = 0;
        state = 0;
        len = inputStream == null ? 0 : inputStream.length;
        inputStream = IOHelper.getInputSequence(inputSequenceFilePath);
        this.stateTableFilePath = stateTableFilePath;
    }

    private void process(){
        len = inputStream.length;
        while(true){
            nextChar = getNextChar();
            //读到第一个不是空格和换行符的字符
            while (nextChar == ' ' || nextChar == '\n'){
                nextChar = getNextChar();
            }

            //1.判断是否为KEYWORD
            if ((nextChar>='a')&&(nextChar<='z')||(nextChar>='A')&&(nextChar<='Z')){
                int saveIsp = isp-1;
                while((nextChar>='a')&&(nextChar<='z')||(nextChar>='A')&&(nextChar<='Z')){
                    keyword[kwp++] = nextChar;
                            //输出keyword时，要将kwp置为0
                    if(isp>=len){break;}
                    nextChar = getNextChar();
                }
                if(isKeyWord(new String(keyword))){
                    printToken(new String(keyword), "KEYWORD");
                    makeKeywordArrEmpty();
                    isp--;
                }else{
                    makeKeywordArrEmpty();
                    isp = saveIsp;
                    if(isp >= len){break;}
                    nextChar = getNextChar();
                    //不是KEYWORD就可能是正则表达式,再用一个while来读
                    int lastState = state;
                    String s;
                    while(((nextChar>='a')&&(nextChar<='z'))||((nextChar>='A')&&(nextChar<='Z'))||((nextChar>='0' && nextChar<='9'))){
                        state = lookUpTable(state, nextChar);
                        token[tp++] = nextChar;
                        if(state == -1 && isTerminal(lastState)){
                            s = new String(token);
                            printToken(s, "ID");
                            isp--;
                        }else if (state == -1 && !isTerminal(lastState)){
                            reportError();
                        }else if (state != -1 && isTerminal(state)){
                            //有下一个状态，看当前状态是不是终态，是，看有没有下一个字符，要是有，继续读；没有，吐一个字符，当前状态即为结束，字符串合法
                            if(isp >= len){
                                s = new String(token);
                                printToken(s, "ID");
                            }else{
                                nextChar = getNextChar();
                                //向前再看一个字符，如93#，如果不是数字，读到#，输出token序列
                                if(!((nextChar>='a')&&(nextChar<='z')||(nextChar>='A')&&(nextChar<='Z')||(nextChar>='0' && nextChar<='9'))){
                                    s = new String(token);
                                    printToken(s, "ID");
                                    isp--;
                                }else{
                                    isp--;
                                }
                            }

                        }
                        if(isp >= len){break;}
                        nextChar = getNextChar();
                    }
                    isp--;
                }

            }
            //2.判断是否为操作符
            // 不是KEYWORD, 也不是以数字开头
            else if (!(nextChar>='0' && nextChar<='9')){
                String s = String.valueOf(nextChar);
                if(isArithmeticOperator(s)){
                    printToken(s, "ARITHMETIC_OPERATOR");
                }else if (isMarkOperator(s)){
                    printToken(s, "MARK_OPERATOR");
                }else{
                    reportError();
                }
            }else{
                //不是以字符和运算符开头的输入---数字
                int lastState = state;
                String s;
                while((nextChar>='0' && nextChar<='9')){
                    state = lookUpTable(state, nextChar);
                    token[tp++] = nextChar;
                    if(state == -1 && isTerminal(lastState)){
                        s = new String(token);
                        printToken(s, "NUM");
                    }else if (state == -1 && !isTerminal(lastState)){
                        reportError();
                    }else if (state != -1 && isTerminal(state)){
                        //有下一个状态，看当前状态是不是终态，是，看有没有下一个字符，要是有，继续读；没有，吐一个字符，当前状态即为结束，字符串合法
                        if(isp >= len){
                            s = new String(token);
                            printToken(s, "NUM");
                        }else{
                            nextChar = getNextChar();
                            //向前再看一个字符，如93#，如果不是数字，读到#，输出token序列
                            if(!(nextChar>='0' && nextChar<='9')){
                                s = new String(token);
                                printToken(s, "NUM");
                                isp--;
                            }else{
                                isp--;
                            }
                        }

                    }
//                    else{
//                        //下一个有状态，当前状态不是终态, 就接着把读到的字符放入到缓冲区token中
////                        token[tp++] = nextChar;
//                    }
                    if(isp >= len){break;}
                    nextChar = getNextChar();
                }
                isp--;
            }
            if (isp >= len){
                break;
            }
        }
    }



    private char getNextChar(){

        return inputStream[isp++];
    }
    private void makeEmpty(){
        for (int i = 0; i < 50;i++){
            token[i] = ' ';
        }
        tp = 0;
    }
    private void makeKeywordArrEmpty(){
        for (int i = 0; i < 50;i++){
            keyword[i] = ' ';
        }
        kwp = 0;
    }

    private boolean isKeyWord(String str){
        return Arrays.asList(KEY_WORD_TABLE).contains(str.trim());
    }

    private boolean isArithmeticOperator(String ch){
        return Arrays.asList(ARITHMETIC_OPERATOR_TABLE).contains(ch.trim());
    }

    private boolean isMarkOperator(String ch){
        return Arrays.asList(MARK_OPERATOR_TABLE).contains(ch.trim());
    }

    private boolean isTerminal(int state){
        return Arrays.asList(TerminalState).contains(String.valueOf(state));
    }



    private void printToken(String tokenStr, String type){
        System.out.println("<" + type + ", " + tokenStr.trim() + ">");
        makeEmpty();
    }

    private void reportError(){
        System.out.println("Input is invalidate");
    }

    private int lookUpTable(int state, char ch){
        stateTable = IOHelper.getStateData(stateTableFilePath);
        if(stateTable != null){
            if(((ch>='a')&&(ch<='z'))||((ch>='A')&&(ch<='Z'))){
                return Integer.parseInt(stateTable[state][1]);
            }else if ((ch>='0' && ch<='9')){
                return Integer.parseInt(stateTable[state][2]);
            }else{
                return -1;
            }
        }else{
            return -1;
        }

    }
    public static void main(String[] args){
//        System.out.println(String.valueOf(']'));
//        System.out.println(Arrays.asList(MARK_OPERATOR_TABLE).contains(String.valueOf(']')));
//        char[] a = {'a','j',' '};
//        System.out.println(new String(a).trim());
        Lex lex = new Lex("Lex/src/lex/MinDFAStateTable.txt","Lex/src/lex/TestData.txt");
        lex.stateTable = IOHelper.getStateData(lex.stateTableFilePath);
        lex.process();
//        lex.process("990()".toCharArray());
//        System.out.println(IOHelper.getInputSequence("/Users/xiezhenyu/IdeaProjects/Lex/Lex/src/lex/TestData.txt"));
//        for (int i = 0; i < lex.stateTable.length;i++){
//            for (int j = 0;j < lex.stateTable[0].length;j++){
//                System.out.print(lex.stateTable[i][j] + " ");
//            }
//            System.out.println();
//        }
    }
}












