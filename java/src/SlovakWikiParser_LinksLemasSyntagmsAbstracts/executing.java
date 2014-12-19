package wiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.*;

import javax.naming.spi.DirectoryManager;

import test.Testing;


public class executing {

	/**counting variables for statistics*/
	static int allLiks;
	static int abstractsFound;
	static int abstractsNotFound;
	static int typesFound;
	static int typesNotFound;
	
	/**wiki pages temp files - list of filepaths*/
	static List<Path> strPagesWiki;
	/**hashtable - pairs - title of wiki page (key), path to file, where page ist stored (value)*/
	static Hashtable<String,String> titleWikiPages;
	/**file with abstracts from sk dbpedia*/
	static String strAbstractsFile;
	
	public static void main(String[] args) {
		try
		{
			//init variables for statistics
			allLiks=0;
			abstractsFound=0;
			abstractsNotFound=0;
			typesFound=0;
			typesNotFound=0;
			
			//init paths
			String abstractsFilePath = "";
			String pagesFilePath="";
			if(args.length>0)
			{
				pagesFilePath = args[0];
				abstractsFilePath = args[1];
			}
			else 
			{
				pagesFilePath ="data/skwiki-latest-pages-articles.xml";
				abstractsFilePath="data/short_abstracts_sk.ttl";
			}
			
			//byte array to load file contents
			byte[] fileArray;
			
			//open file with abstracts
			Path fileTypes = Paths.get(abstractsFilePath);
			fileArray = Files.readAllBytes(fileTypes);
			strAbstractsFile = new String(fileArray, "UTF-8");
			
			//open file with articles
			//Path fileWiki = Paths.get("C:/Users/Domi/workspace/WikiParser/data/sample_skwiki-latest-pages-articles.xml");
			//Path fileWiki = Paths.get("C:/Users/Domi/workspace/WikiParser/data/skwiki-latest-pages-articles.xml");
			//strPagesWiki = inputPreprocessing("C:/Users/Domi/workspace/WikiParser/data/sample_skwiki-latest-pages-articles.xml");
			strPagesWiki = inputPreprocessing(pagesFilePath);
			//System.out.println(Boolean.toString(Testing.testParsedInput(strPagesWiki.toArray(new Path[strPagesWiki.size()]))));
			
			System.out.println("processing start");
			long start = (new Date()).getTime();

			for(int fileId=0;fileId<strPagesWiki.size();fileId++)
			{
				fileArray = Files.readAllBytes(strPagesWiki.get(fileId));
				//one wiki page loaded
				String currentPage = new String(fileArray, "UTF-8");
				//list of input sentences
				String[] currentPageLines = currentPage.split("\\r\\n");
				
				List<Sentence> parsedSentences = new ArrayList<Sentence>();
				
				//sentences start from id=3
				for(int sentId=3;sentId<currentPageLines.length;sentId++)
				{
					String s = currentPageLines[sentId];
					//for each sentence will be done following
					Sentence oneSent1 = new Sentence();
					oneSent1.setFullSent(s);
					oneSent1.setLinks(getLinks(s));	
					allLiks+=oneSent1.getLinks().size();
					if(oneSent1.getLinks().size()>0)
						parsedSentences.add(oneSent1);
				}

				if(parsedSentences.size()>0)
				{
					String output = getOutput(parsedSentences);
					Path file = Paths.get(strPagesWiki.get(fileId).toString().replace("input","output"));
					//Path file = Paths.get("C:/Users/Domi/workspace/WikiParser/data/output1.xml");
					byte[] buf = output.getBytes("UTF-8");
					Files.write(file, buf);
				}
			}
			
			long end = (new Date()).getTime();
			System.out.println("processing end");
			
			double spanMS = end-start;
			double minutes = spanMS/60000;
			double seconds = (minutes - Math.round(minutes)) * 60;
			System.out.print(Math.round(minutes));
			System.out.print(" min ");
			System.out.print(seconds);
			System.out.println(" seconds processing");
			
		//System.out.println("Unit test results: ");
		//System.out.println("Structure test - " + Boolean.toString(Testing.unitTestStructure(output)));
		//System.out.println("Content test - " + Boolean.toString(Testing.unitTestContent(output)));
		
		//write statistics
			String stats = "all links: " + Integer.toString(allLiks)+"\r\n";
			stats += "typesFound: " + Integer.toString(typesFound)+"\r\n";
			stats += "types not found: " + Integer.toString(typesNotFound)+"\r\n";
			stats += "abstracts found: " + Integer.toString(abstractsFound)+"\r\n";
			stats += "abstracts not found: " + Integer.toString(abstractsNotFound)+"\r\n\r\n";
			stats += "Duration " + Long.toString(Math.round(minutes)) + " minutes " + Double.toString(seconds) + " seconds\r\n";
			
			saveAsFile(stats, "data/statistics.txt");
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * gets all links marked as [[..]] from string sentence
	 * returns List<String>
	 * @throws IOException 
	 * */
	public static List<Link> getLinks(String sentence) throws IOException
	{
		List<Link> result = new ArrayList<Link>();
		
		//pattern to match link
		Pattern p = Pattern.compile("\\[{2}[\\w\\s\\|\\p{L}\\(\\)]+\\]{2}\\w*");
		Matcher m = p.matcher(sentence);

		List<String> strLinks = new ArrayList<String>();
		while (m.find()) {
			String strFound = m.group();
			//ignore image links
			if(!strFound.startsWith("[[Súbor:"))
			{
				strLinks.add(strFound);
				result.add(new Link(strFound));
			}
		}
		
		for(Link l:result)
		{
			String linkFilePath = getLinkFile(l);
			if(linkFilePath!=null && linkFilePath!="")
			{
				byte[] pom = Files.readAllBytes(Paths.get(linkFilePath));
				String linkFileContent = new String(pom,"UTF-8");
				
				if(l.getType()==null || l.getType()=="")
					getLinkTypeFromInfobox(l, linkFileContent);
				getLinkAbstract(l, linkFileContent);
				l.setSyntax("Atr");
				
				//statistics
				if(l.getType()=="")
					typesNotFound++;
				else
					typesFound++;
				
				if(l.getArticleAbstract()=="")
					abstractsNotFound++;
				else
					abstractsFound++;
			}
		}
		
		return result;
	}
	
	/**gets a temp file of a link (by its lemma)*/
	private static String getLinkFile(Link l)
	{
		return titleWikiPages.get(l.getLemma());
	}
	
	
	/**finds infobox in linkFileContent and sets a type from it
	 * */
	public static void getLinkTypeFromInfobox(Link l, String linkFileContent)
	{
		String linkType = "";
		linkType = linkFileContent.split("\\r\\n")[1];
		linkType = linkType.replace("INFOBOX: ", "").trim();
		l.setType(linkType);
	}
	
	/**finds abstract in page text content
	 * */
	public static void getLinkAbstract(Link l, String linkFileContent)
	{

		String linkAbstract = "";
		
		//pattern to match db link in Types file
		Pattern p = Pattern.compile("<{1}" + l.getDbLnk() + ">{1}.*\\.");
		Matcher m = p.matcher(strAbstractsFile);

		while (m.find()) {
			String strLineFound = m.group();
			String[] parts = strLineFound.split(" <http://www.w3.org/2000/01/rdf-schema#comment> ");
			if(parts.length==2)
			{
				//abstract is the 2nd item of splitted line
				linkAbstract = parts[1];
			}
		}
		l.setArticleAbstract(linkAbstract);;
	}	
		
		
	public static String getOutput(List<Sentence> sentences)
	{
		String strResult = "";
		strResult+="<output>";
		
		for(int i=0;i<sentences.size();i++)
		{
			Sentence currentSent=sentences.get(i);
			strResult+="<sentence id=\"" + Integer.toString(i) + "\">";
			strResult+="<allSent>" + currentSent.getFullSent() + "</allSent>";
			
			strResult+="<links>";
			
			for(Link l : currentSent.getLinks())
			{
				strResult+="<link>";
				
				strResult+="<linkLabel>" + l.getLabel() + "</linkLabel>";
				strResult+="<lemma>" + l.getLemma() + "</lemma>";
				strResult+="<wklnk>" + l.getWLnk() + "</wklnk>";
				strResult+="<dblnk>" + l.getDbLnk() + "</dblnk>";
				strResult+="<abstract>" + l.getArticleAbstract() + "</abstract>";
				strResult+="<type>" + l.getType() + "</type>";
				strResult+="<vc>" + "Atr" + "</vc>";
				
				strResult+="</link>";
			}
			
			strResult+="</links>";
			strResult+="</sentence>";
		}
		
		strResult+="</output>";
		
		return strResult;
	}
	
	/**
	 * divides one big file into many temp files - one file per each wiki page
	 * */
	private static List<Path> inputPreprocessing(String fileName)
	{
		List<Path> resultList=new ArrayList<Path>();
		titleWikiPages = new Hashtable<String, String>();
		try
		{
			Date d = new Date();
			long start = d.getTime();
			
			//check if directories exists
			String parsingDir = "data/output";
			File directory = new File(parsingDir);
			if(!(directory.exists()))
			{
				directory.mkdir();
			}
			
			//check if directory exists
			parsingDir = "data/input";
			directory = new File(parsingDir);
			if(!(directory.exists()))
			{
				directory.mkdir();
			}
					
			//parse input file
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line="";
			//read file line by line
			//sorting by pages
			//one wiki page
			String page = "";
			String pageTitle = ""; 
			String pageInfobox="";
			String pageText="";
			boolean pageFound =false;
			int pagesCount = 0;
			while ((line = br.readLine()) != null && pagesCount<1000) 
			{
			   line=line.trim();
			   if(line.contains("<page>"))
			   {
				   pageFound = true;
				   //page+=line + "\r\n";
			   }
			   else if(pageFound && line.contains("</page>"))
			   {
				   if(pageText.replace("\r\n", "")!="")
				   {
					   page+= "TITLE: " + pageTitle + "\r\n";
					   page+= "INFOBOX: " + pageInfobox + "\r\n\r\n";
					   page+= pageText;
					   //page+=line;
					   
					   String fullFilePath=parsingDir+ "/pg_" + Integer.toString(resultList.size() + 1) + ".txt";
					   if(!(new File(fullFilePath)).exists())
					   {
						   saveAsFile(page, fullFilePath);
					   }
					   resultList.add(Paths.get(fullFilePath));
					   titleWikiPages.put(pageTitle, fullFilePath);
				   }
				   page="";
				   pageTitle="";
				   pageInfobox="";
				   pageFound = false;
				   pagesCount++;
			   }
			   //get the title
			   else if(pageFound && line.contains("<title>"))
			   {
				   pageTitle = line.replace("<title>", "").replace("</title>", "");
				   //we do not want to use configuration pages such as MediaWiki or Upload log
				   if(pageTitle.contains(":"))
				   {
					   pageTitle="";
					   page="";
					   pageText="";
					   pageFound=false;
				   }
			   }
			   else if(pageFound && line.contains("{{Infobox"))
			   {
				   pageInfobox = line.split("\\{\\{Infobox")[1];
				   pageInfobox = pageInfobox.trim();
			   }
			   else if(pageFound)
			   {
				   if(line.contains("==Externé odkazy=="))
						break;
				
				   else if(line.length()>0 && line!="\r\n" 
						   && !line.startsWith("==") 
						   && !line.startsWith("{") 
						   && !line.startsWith("}") 
						   && !line.startsWith("|") 
						   && !line.startsWith("&lt;") 
						   && !line.startsWith("#")
						   && !line.startsWith("*")
						   && !line.startsWith("<")
						   && !line.startsWith("[[Kategória:")
						   && !line.startsWith("Image:")
						   && !line.startsWith(":")
						   && !line.startsWith("!")
						   && !line.startsWith(";"))
					{
					   
					   if(!line.endsWith("\r\n"))
						   pageText+=line + "\r\n";
					   else
						   pageText+=line;
					}
				}
				   
			}
			br.close();
			
			long end = (new Date()).getTime();
			long span = end-start;
			
			System.out.print(span);
			System.out.println(" milisec - preprocessing");
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		return resultList;
	}
	
	/**save String content as file specified by filePath
	 * @throws IOException 
	 * */
	private static void saveAsFile(String content, String filePath) throws IOException
	{
		Path file = Paths.get(filePath);
		byte[] buf = content.getBytes("UTF-8");
		Files.write(file, buf);
	}
}
