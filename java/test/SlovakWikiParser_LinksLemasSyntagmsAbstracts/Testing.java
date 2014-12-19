package test;

import java.awt.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Testing {

	/**unit test - porovnanie so vzorovym suborom/
	 */
	public static Boolean unitTestContent(String output)
	{
		Boolean bResult = false;
		try
		{
			//get pattern file as string
			Path fileTypes = Paths.get("C:/Users/Domi/workspace/WikiParser/data/sample_output_parsed_sentences_and_links.xml");
			byte[] fileArray = Files.readAllBytes(fileTypes);
			String strPatternFile = new String(fileArray, "UTF-8");
			if(strPatternFile.equals(output))
				bResult=true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		return bResult;
	}
	
	public static Boolean unitTestStructure(String output)
	{
		Boolean bResult = false;
		try
		{
			Path fileTypes = Paths.get("C:/Users/Domi/workspace/WikiParser/data/sample_output_parsed_sentences_and_links.xml");
			byte[] fileArray = Files.readAllBytes(fileTypes);
			String strPatternFile = new String(fileArray, "UTF-8");
			if(strPatternFile.equals(output))
				bResult=true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		return bResult;
	}
	
	/**test parsed input file
	 * */
	private static Boolean testOneParsedInputFile(Path p)
	{
		Boolean bResult = false;
		try
		{
			byte[] byteContent = Files.readAllBytes(p);
			String strFileContent = new String(byteContent, "UTF-8");
			String[] lines = strFileContent.split("\\r\\n");
			if(lines[0].startsWith("TITLE:") && lines[1].startsWith("INFOBOX:") && lines[2].equals("") && !lines[3].equals(""))
				bResult=true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		return bResult;
	}
	
	public static Boolean testParsedInput(Path[] paths)
	{
		Boolean bResult = true;
		try
		{
			for(int i=0;i<paths.length;i++)
			{
				Boolean oneFile =testOneParsedInputFile(paths[i]);
				System.out.print(oneFile);
				System.out.println(" - " + Integer.toString(i));
				bResult = bResult && oneFile;
			}
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		return bResult;
	}
	
	/**test, if the text does not contain \< or \> - a.k.a. it was parsed out of XML file well
	 * */
	public static Boolean isTextClear(String text)
	{
		Boolean bResult = true;
		try
		{
			if(text.contains("<") || text.contains(">"))
				bResult=false;
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		return bResult;
	}
}
