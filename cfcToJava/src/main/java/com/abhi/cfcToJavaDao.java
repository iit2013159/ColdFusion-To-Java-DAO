package com.abhi;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cfcToJavaDao {
    public static final String[] keys= { "isDefined","is Not","isNumeric","FindNoCase","recordCount","len","trim","GT" ,"NEQ","EQ", "NOT", "AND" , "OR","Left"};
    public Stack<String> stringStack = new Stack<>();
    public cfcToJavaDao(String damaged) {
    }

    public static String converter(final Path abc){
        String data = "";
        
        String fileName = abc.toString();
        {
            try {
                data = new String(Files.readAllBytes(Paths.get(fileName)));
                String fileHead = extractFileName(fileName);
                getMetatData(data,fileHead);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }
    public static String extractFileName(String fileName){
        for(String token: fileName.split("//")){
            if(token.contains(".cfc")){
                return token.substring(0,token.indexOf(".cfc"));
            }
        }
        return "";
    }
    public static void getMetatData(String data,String fileHead){
        String[] functions  = data.split("<cffunction");
        boolean say = true;
        String key ="";
        for (String function : functions){
            //System.out.println(function);
            if(say && function.indexOf("<!--") > -1) {
                String comment = function.substring(function.indexOf("<!--"), function.indexOf("-->"))
                        .replace("<!--","#");
                comment = removeExtraSpace(comment);
                System.out.println("//" +comment);
                say = false;
            }
            
            if(function.indexOf("</cffunction>") != -1) {
                key = extractFunction(fileHead, key, function);
            }
        }
    }

    private static String extractFunction(String fileHead, String funcName, String function) {
        String fnComment ="";
        if(function.indexOf("<!--") > -1) {
            fnComment = function.substring(function.indexOf("<!--"), function.indexOf("-->"));
        }
        function = function.substring(0, function.indexOf("</cffunction>"));
        if(function.indexOf("name=\"") != -1){
            function = function.substring(7,function.length());
            int wInd = function.indexOf("\"");
            if(wInd != -1){
                funcName = function.substring(0,wInd);
            }
            //System.out.print("# FUNCTION NAME --> "+ key);
            //System.out.println(function);
            List<Pair<String,String>> argumentList = changefuncToArguments(function);
            convertArgumentsToJava(argumentList);
           String fnDesc = funcName.concat(" ( ") ;
            fnDesc = "Public ResultSet ".concat(fnDesc);
            for (Pair<String,String> pair : argumentList){
                fnDesc = fnDesc.concat(pair.getKey() +" " +pair.getValue() +" , ");
            }
            if(fnDesc.contains(", ")) {
                fnDesc = fnDesc.substring(0, fnDesc.lastIndexOf(", "));
            }
            fnDesc = fnDesc.concat(") { ");
            System.out.println();
            System.out.println(fnDesc);
            functionProcessing(fileHead, funcName, function, argumentList);
            System.out.println("}");
            fnComment = fnComment.replaceAll("<!--", "#");
            System.out.println("// "+removeExtraSpace(fnComment));
        }
        //break;
        return funcName;
    }

    private static void functionProcessing(String fileHead, String key, String function, List<Pair<String, String>> argumentList) {
        if(!checkForCases(function)) {
            List<String> queryList = changefuncToQuery(function, key, fileHead);
            System.out.println("");
            for(String indQuery : queryList){
                Pair<String,String> resPair = makeQueryPrepared(removeExtraSpace(indQuery),argumentList);

                String str = "String selectSQL = "+resPair.getKey()+";\n" +
                        "PreparedStatement preparedStatement = dbConnection.prepareStatement(selectSQL);";
                str = str.concat(resPair.getValue());
                str = str.concat("\nResultSet rs = preparedStatement.executeQuery(selectSQL );");
                str = str.concat("\nreturn rs;");
                System.out.println(str);
            }

        }else {
            complexMethod(function,argumentList);
        }
    }

    public static String changePrimitive(String argKey){
        switch (argKey.toUpperCase()){
            case "STRING":
                return "String";
            case "NUMERIC":
                return "int";
            default:
                return argKey;
        }
    }
    public static void convertArgumentsToJava(List<Pair<String, String>> argumentList) {
        int i = 0;
        while( i < argumentList.size()){
            Pair<String,String> argKey = argumentList.get(i);
            argKey = new Pair<String, String>(changePrimitive(argKey.getKey()),argKey.getValue());
            argumentList.add(i,argKey);
            argumentList.remove(i+1);
            i++;
        }
    }

    public static void complexMethod(String function, List<Pair<String, String>> argumentList) {
        Map<Integer,String> indexAr = new TreeMap<>();
        if(function.contains("<cfquery")) {
            indexAr.put(function.indexOf("<cfquery"), "<cfquery");
        }
        if(function.contains("<cfswitch")) {
            indexAr.put(function.indexOf("<cfswitch"), "<cfswitch");
        }
        if(function.contains("<cfif")) {
            indexAr.put(function.indexOf("<cfif"), "<cfif");
        }
        if(function.contains("<cftransaction")) {
            indexAr.put(function.indexOf("<cftransaction"), "<cftransaction");
        }
        Iterator<Integer> iterator = indexAr.keySet().iterator();
        while (iterator.hasNext()) {
            int option = iterator.next();
            String choice  = indexAr.get(option);
            switch (choice){
                case "<cfquery":
                    String qu1 = function.substring(option, function.indexOf("</cfquery>"));
                    qu1 = qu1.substring(qu1.indexOf(">") + 1, qu1.length());
                    System.out.println("// cfquery --> " + removeExtraSpace(qu1));
                    Pair<String,String> resPair = makeQueryPrepared(removeExtraSpace(qu1),argumentList);

                    String str = "String selectSQL = "+resPair.getKey()+";\n" +
                            "PreparedStatement preparedStatement = dbConnection.prepareStatement(selectSQL);";
                    str = str.concat(resPair.getValue());
                    str = str.concat("\nResultSet rs = preparedStatement.executeQuery(selectSQL );");
                    //str = str.concat("return rs;");
                    System.out.println(str);
                    break;
                case "<cfif":
                    qu1 = function.substring(option, function.indexOf("</cfif>"));
                    qu1 = qu1.substring(choice.length(), qu1.indexOf(">"));
                    System.out.println("// if statement" + removeExtraSpace(qu1));
                    String condition = evaluteIfCondition(qu1);
                    System.out.println("if(  "+condition+" ){");

                    break;
            }

        }

        System.out.println("its detailed");
    }

    /**
     * @param qu1
     * @param argumentList
     * @return Pair where key is resulting sl query and value is prepared statement for the  particular query.
     */
    public static Pair<String, String> makeQueryPrepared(String qu1, List<Pair<String, String>> argumentList) {
        /*"preparedStatement.set"+
                argumentList.get(i).getKey().substring(0,1).toUpperCase() +argumentList.get(i).getKey().substring(1)+
                "("+
                String.valueOf(i+1) +
                ","+
                argumentList.get(i).getValue()+");\n");*/
        Pair<String,String> resPrepared;
        Map<String,String> argMap = new LinkedHashMap();
        for (Pair<String,String> pair : argumentList){
            argMap.put(pair.getValue(),pair.getKey());
        }
        if(qu1.toUpperCase().contains("ARGUMENTS")) {
            int start = qu1.toUpperCase().indexOf("ARGUMENTS");

            String tempQ = "";
            String res = "";
            String prep ="";
            int count = 0;
            while (start > 8 && start <= qu1.toUpperCase().lastIndexOf("ARGUMENTS")) {
                tempQ = qu1.substring(start-8,qu1.length());
                if(qu1.contains("'") && qu1.indexOf("'") < start && qu1.indexOf("'") >= start -8 ) {
                    res = res.concat(qu1.substring(0, qu1.indexOf("'")));
                    start = tempQ.indexOf("'");
                    tempQ = tempQ.substring(start + 1, tempQ.length());
                    tempQ = tempQ.substring(0, tempQ.indexOf("'") + 1);
                    qu1 = qu1.replaceFirst(tempQ, "SomeGarbage");
                    qu1 = qu1.substring(
                            qu1.indexOf("SomeGarbage"), qu1.length()
                    );
                }else if (qu1.contains("#") && qu1.indexOf("#") < start && qu1.indexOf("#") >= start -8){
                    res = res.concat(qu1.substring(0, qu1.indexOf("#")));
                    start = tempQ.indexOf("#");
                    tempQ = tempQ.substring(start + 1, tempQ.length());
                    tempQ = tempQ.substring(0, tempQ.indexOf("#") + 1);
                    qu1 = qu1.replaceFirst(tempQ, "SomeGarbage");
                    qu1 = qu1.substring(
                            qu1.indexOf("SomeGarbage"), qu1.length()
                    );
                }
                /*else {
                    throw new IllformedLocaleException("fdjgdbf");
                }*/
                start = qu1.indexOf("ARGUMENTS");
                if(start == -1){
                    res = res.concat(qu1);
                }
                try {
                    prep = prep.concat("\npreparedStatement.set" +
                            argMap.get(extractArgName(tempQ)).substring(0, 1).toUpperCase() + argMap.get(extractArgName(tempQ)).substring(1) +
                            "( " +
                            String.valueOf(count + 1) +
                            "," +
                            extractArgName(tempQ) + ");");
                }catch (NullPointerException e){
                    System.out.println("tempQ "+ tempQ +"\n extracted is "+extractArgName(tempQ));
                    throw e;
                }
                count++;
                    /*start = pos + qu1.substring(
                            qu1.indexOf(tempQ)+tempQ.length(),qu1.length()
                    ).indexOf("ARGUMENTS");*/

            }
            resPrepared = new Pair(res.replaceAll("SomeGarbage","?"),prep);
            return resPrepared;
        }
        return new Pair<String,String>(qu1,"");
    }
    public static String extractArgName(String cfParam){
        String key = getKey(cfParam);
        //System.out.println("key is " + key);
        return cfParam.replaceAll("[#'()]","").split("[\\., ]")[1];
    }
    public static String replaceFirst(String currString, CharSequence target, CharSequence replacement) {
        return Pattern.compile(target.toString(), Pattern.LITERAL).matcher(
                currString).replaceFirst(Matcher.quoteReplacement(replacement.toString()));
    }
    public static String evaluteIfCondition(String string) {
        /*
         * KeyWord Mentioned are
         * GT ,EQ, NEQ, NOT, AND , OR
         * isDefined
         * is Not
         * isNumeric
         * FindNoCase
         * recordCount
         * len
         * trim
         * */

        String[] tokens = string.split(" ");
        String res ="";
        for (String token : tokens){
            String key = getKey(token);
            res = res.concat(evalKey(key,token));
        }
        return res;
    }

    public static String getKey(String token) {
        return Arrays.asList(keys).stream()
                .filter(
                        i->
                                Arrays.stream(token.split("[.()]"))
                                        .filter(j -> j.toUpperCase().contains(i.toUpperCase()))
                                        .findAny()
                                        .isPresent()
                )
                .findFirst()
                .orElse("");
    }

    public static String evalKey(String key,String token) {
        String arg;

        String tempKey = key.toUpperCase();
        switch (tempKey){
            case "RECORDCOUNT":
                String[] args = token.split("\\.");
                arg = args[0];
                return "rowCount(".concat(arg).concat(") ");
            case "FINDNOCASE":
                return "";
            case "LEN":
                if(token.contains("(")){
                    if(token.indexOf("(") != token.lastIndexOf("(") && token.indexOf(")") != token.lastIndexOf(")")){
                        String finalArg = token.substring(token.indexOf("(")+1,token.indexOf(")")+1);
                        String anotherKey =  Arrays.asList(keys).stream().filter(i->finalArg.toUpperCase().contains(i.toUpperCase())).findFirst().orElse("");
                        return evalKey(anotherKey,finalArg).concat(".length() ");
                    }
                    arg = token.substring(token.indexOf("(")+1,token.lastIndexOf(")"));
                    return arg.concat(".length() ");
                }
            case "TRIM":
                if(token.contains("(")) {
                    if (token.indexOf("(") != token.lastIndexOf("(") && token.indexOf("(") == token.lastIndexOf(")")) {
                        String finalArg = token.substring(token.indexOf("(") + 1, token.lastIndexOf("("));
                        String anotherKey = Arrays.asList(keys).stream().filter(i ->finalArg.toUpperCase().contains(i.toUpperCase())).findFirst().orElse("");
                        if (anotherKey.isEmpty()) {
                            return token.split("\\(")[0].concat(evalKey(token.substring(token.indexOf("\\("), token.length()), key));
                        } else {
                            return evalKey(anotherKey, finalArg).concat(" ").concat(token.split("\\(")[1]);
                        }
                    }
                    arg = token.substring(token.indexOf("(") + 1, token.lastIndexOf(")"));
                    return arg.concat(".trim() ");
                }
            case "ISDEFINED":
                if(token.contains("(")) {
                    arg = token.substring(token.indexOf("(") + 1, token.indexOf(")"));
                    return arg.concat(" != NULL && !").concat(arg).concat(".isEmpty() ");
                }
            case "GT":
                return "> ";
            case "EQ":
                return " == ";
            case "NEQ":
                return " != ";
            case "NOT":
                return " !";
            case "IS NOT":
                return "";
            case "OR":
                return " || ";
            case "AND":
                return " && ";
            default:
                //System.out.println("This keyWord is not in dictionary");
                return token;

        }
    }
    public static boolean checkBraces(String token,String braces){
        if(token.lastIndexOf(braces) == token.indexOf(braces)){
            return true;
        }return false;
    }

    public static boolean checkForCases(String function) {
        if(function.contains("cfif") ||
                function.contains("cftransaction") ||
                function.contains("cfswitch")){
            return true;
        }return false;
    }

    public static List<Pair<String,String>> changefuncToArguments(String function) {
        List<Pair<String,String>> arguments = new ArrayList<>();
        String[] args = function.split("<cfargument");
        //System.out.print(" , ARGS -->");
        if(args.length == 1){
            //System.out.print("No args");
            return arguments;
        }
        String argName = "";
        String argType = "";
        for(String arg : args) {
            argName = getVal(arg,"name=\"");
            argType = getVal(arg,"type=\"");

            if(!argName.isEmpty() && !argName.isEmpty()) {
                arguments.add(new Pair<>(argType,argName));
                //System.out.print(argType + " " + argName + " ,");
            }
            //System.out.println(arg);
        }
        return arguments;
    }
    public static String getVal(String main,String find){
        if(main.contains(find)){
            main = main.substring(main.indexOf(find)+find.length(),main.length());
            if(main.contains("\"")){
                return main.substring(0,main.indexOf("\""));
            }
        }
        return "";
    }

    public static List<String> changefuncToQuery(String function, String key, String fileHead){

        Map<String,List<Pair<String,String>>> queryList = new HashMap<>();
        List<String> queryArrList = new ArrayList<>();
        String[] queries = function.split("<cfquery");
        if(queries.length == 1){
            System.out.println("no queries");
            return queryArrList;
        }
        for(String query : queries){
            String datasource = "";
            String queryName = "";
            datasource = getVal(query,"datasource=\"");
            queryName = getVal(query,"name=\"");

            if(!queryName.isEmpty() && query.contains(queryName) && !datasource.isEmpty()) {
                query = getQueryString(query, queryName);
                /*System.out.println(
                        fileHead + "." +
                                key + "." +
                                datasource.replaceAll("#","")+ "." +
                                queryName+ " =" + query
                );*/
                queryArrList.add(query);
            }
            if(queryList.containsKey(datasource)){
                queryList.get(datasource).add(new Pair<String,String>(queryName,query));
            }else{
                List<Pair<String, String>> ar = new ArrayList<Pair<String, String>>();
                ar.add(new Pair<>(queryName,query));
                queryList.put(datasource,ar);
            }
        }
        return queryArrList;
    }

    public static String getQueryString(String query, String queryName) {
        //query = query.substring(query.indexOf(queryName)+queryName.length(),query.length());
        if(query.indexOf("</cfquery>") > -1){
            query = query.substring(query.indexOf(queryName)+queryName.length()+2,query.indexOf("</cfquery>"));
            query = removeExtraSpace(query);
        }
        return query;
    }

    public static String removeExtraSpace(String str){
        str = str.replaceAll("\r"," ")
                .replaceAll("\n"," ")
                .replaceAll("\t"," ");
       int i = 0;
        while (i < str.length() -1){
            int st = i;
            if(str.charAt(i+1) == str.charAt(i) && str.charAt(i)==' ') {
                while (i < str.length() - 1 && str.charAt(i + 1) == str.charAt(i) && str.charAt(i) == ' ') {
                    i++;
                }
                str = str.replace(str.substring(st, i+1), " ");
                i = i - (i - st);
            }
            i++;
        }
        return str;
    }


}
