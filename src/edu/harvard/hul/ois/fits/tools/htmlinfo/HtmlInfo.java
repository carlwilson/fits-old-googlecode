package edu.harvard.hul.ois.fits.tools.htmlinfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;



import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsToolException;
import edu.harvard.hul.ois.fits.tools.ToolBase;
import edu.harvard.hul.ois.fits.tools.ToolInfo;
import edu.harvard.hul.ois.fits.tools.ToolOutput;

public class HtmlInfo extends ToolBase
{
	private boolean enabled = true;
	public final static String xslt = Fits.FITS_HOME + "xml/htmlinfo/htmlinfo_to_fits.xslt";
	public final static boolean showParsingTime = false;
	private HtmlParser parser;
		
	public static float avgParsingTime;
	public static float avgXmlCreationTime; 
	public static long objectCount;
	
	public static float avgParsingTimeValid;
	public static float avgXmlCreationTimeValid; 
	public static long objectCountValid;

	public HtmlInfo() throws FitsToolException 
	{
		info = new ToolInfo("HtmlInfo", HtmlParser.PARSER_VERSION, null);		
		//info = new ToolInfo("HtmlInfo", "0.1", null);
	}

	@Override
	public ToolOutput extractInfo(File file) throws FitsToolException 
	{								 
		long startTimeParsing = 0;
		long startTimeGenerating = 0;
		float actualTimeParsing = 0.0f;
		float actualTimeGenerating = 0.0f;
		
		if(showParsingTime)
			startTimeParsing = System.nanoTime();
		
		parser = new HtmlParser(file.getAbsolutePath());
		
		if(showParsingTime)		
		{			
			long estimatedTimeParsing = System.nanoTime() - startTimeParsing;
			// increase object count
			objectCount++;
			actualTimeParsing = (estimatedTimeParsing / 1000000f);
			avgParsingTime = ((objectCount - 1) * avgParsingTime + actualTimeParsing) / objectCount;
		}
				
//		System.out.println("Parsing time: " + (estimatedTime / 1000000f) + "ms");
		if(showParsingTime)
			startTimeGenerating = System.nanoTime();
		
      Document rawXml = parser.createXml();
      
      if(showParsingTime)
      {
	      long estimatedTimeGenerating = System.nanoTime() - startTimeGenerating;
	      actualTimeGenerating = (estimatedTimeGenerating / 1000000f);
			avgXmlCreationTime = ((objectCount - 1) * avgXmlCreationTime + actualTimeGenerating) / objectCount;
      }
      if(showParsingTime)
      {
			try {
				List errors = XPath.selectNodes(rawXml, "//htmlInfo/error");
				if(errors.size() == 0)
				{
					objectCountValid++;
					avgParsingTimeValid = ((objectCount - 1) * avgParsingTimeValid + actualTimeParsing) / objectCountValid;
					avgXmlCreationTimeValid = ((objectCount - 1) * avgXmlCreationTimeValid + actualTimeGenerating) / objectCountValid;
				}
			} catch (JDOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      }
		
		if(showParsingTime)
		{
			System.out.println("objects: " + objectCount);
			System.out.println("avg parsing time: " + avgParsingTime + " ms");
			System.out.println("avg xml creation time: " + avgXmlCreationTime + " ms");
			System.out.println("valid objects: " + objectCountValid);
			System.out.println("v.o. avg parsing time: " + avgParsingTimeValid + " ms");
			System.out.println("v.o. avg xml creation time: " + avgXmlCreationTimeValid + " ms");
		}		
	/*	try {
			new XMLOutputter().output(rawXml, System.out);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		Document fitsXml = transform(xslt, rawXml);
	/*	try {
			new XMLOutputter().output(fitsXml, System.out);
		} catch (IOException e) { 
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return new ToolOutput(this, fitsXml, rawXml);
	}
	
	private Document createXml() throws FitsToolException
	{		
      // xml root
      Element root = new Element("htmlInfo");

      // xml tags element
      Element tags = new Element("tags");		
		root.addContent(tags);
						
		Map<String, Long> tagMap = parser.getTags();
		
		for(String tagStr : tagMap.keySet())
		{
			Element tag = new Element("tag");
			Element tagName = new Element("name");
			Element tagCount = new Element("occurrences");
			
			// set tag name and number of occurrences in file
			tagName.setText(tagStr);
			tagCount.setText(tagMap.get(tagStr).toString());
			
			tag.addContent(tagName);
			tag.addContent(tagCount);
			
			tags.addContent(tag);
		}
		
		return new Document(root);
	}

	@Override
	public boolean isEnabled() 
	{
		return this.enabled;
	}

	@Override
	public void setEnabled(boolean value) 
	{
		this.enabled = value; 
	}
}
