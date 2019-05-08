package com.abhi;
import java.nio.file.Paths;

public class daoCreater {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String fileLocation ="C:\\Users\\nomad\\eclipse-workspace\\carrom\\resource\\test.cfc";
		cfcToJavaDao.converter(Paths.get(fileLocation));

	}

}
