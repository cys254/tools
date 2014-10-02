package com.cisco.oss.foundation.tools.simulator.rest.container;

import java.util.regex.Pattern;

public class Test {

	public static void main(String[] args) {
		
		String patternStr = "dana";
		Pattern compile = Pattern.compile(patternStr);
		
		System.out.println(compile.pattern());
		
		patternStr = "";
		compile = Pattern.compile(patternStr);
		String str = compile.pattern();
		
		
		System.out.println("matches?" + compile.matcher("").matches());
		if (str == null) {
			System.out.println("1 null");
		} else {
			System.out.println("1 not null");
		}
		
		patternStr = null;
		compile = Pattern.compile(patternStr);
		str = compile.pattern();
		
		if (str == null){
			System.out.println("2 null");
		}
	}
}
