package com.abhi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class testCheck {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String a ="Update sample from dknk '#ARGUMENT.asbdsb#' and sert = '#Left(#ARGUMENT.bfdbbjbddfhd)' sbdjbih hdfhfdf nddfbb 'Left(ARGUMENT.uigusxxf)' kjfbdj dfhjigbkd fdhdfkjj 'Left(ARGUMENT.uigusxxf#)#'" ;
		System.out.println(a);
		String res ="";
		int start = a.indexOf("ARGUMENT");
		String temp = "";
		if(start != -1) {
			while(start != -1 && start <= a.lastIndexOf("ARGUMENT")) {				
				temp = a.substring(start-8);
				int pos = temp.indexOf("'")+1;
				res = res.concat(a.substring(0,start-8 + pos-1));
				temp = temp.substring(pos);
				temp = temp.substring(0, temp.indexOf("'")+1);
				a = replaceFirstModi(a,temp, "someGarbage");
				a = a.substring(a.lastIndexOf("someGarbage"));
				
				System.out.println(res);
				System.out.println("string --> " + a);
				System.out.println("Start -----> " + start);
				start = a.indexOf("ARGUMENT");
			}
			System.out.println(res.replaceAll("someGarbage", "?"));
		}
	}
	public static String replaceFirstModi(String input,String replaceable,String replacement) {
		return Pattern.compile(replaceable.toString(), Pattern.LITERAL).matcher(
                input).replaceFirst(Matcher.quoteReplacement(replacement.toString()));
    
	}

}
