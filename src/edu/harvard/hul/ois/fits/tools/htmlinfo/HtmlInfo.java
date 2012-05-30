package edu.harvard.hul.ois.fits.tools.htmlinfo;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsToolException;
import edu.harvard.hul.ois.fits.tools.ToolBase;
import edu.harvard.hul.ois.fits.tools.ToolInfo;
import edu.harvard.hul.ois.fits.tools.ToolOutput;

/**
 * 
 * @author Stefan Schindler (schind85@gmail.com)
 */
public class HtmlInfo extends ToolBase
{
	private boolean enabled = true;
	public final static String xslt = Fits.FITS_HOME + "xml/htmlinfo/htmlinfo_to_fits.xslt";
	// flag that indicates, if debug information is to be shown
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
				
		if(showParsingTime)
			startTimeGenerating = System.nanoTime();
		
		// create xml from htmlparser
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
		// uncomment the following lines to show debug output from the raw xml  
	/*	try {
			new XMLOutputter().output(rawXml, System.out);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		Document fitsXml = transform(xslt, rawXml);
		// uncomment the following lines to show debug output from the fits xml 
	/*	try {
			new XMLOutputter().output(fitsXml, System.out);
		} catch (IOException e) { 
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		return new ToolOutput(this, fitsXml, rawXml);
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
