package edu.harvard.hul.ois.fits.tools.htmlinfo;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.DoctypeTag;
import org.htmlparser.tags.FrameSetTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ObjectTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.jdom.Document;
import org.jdom.Element;

import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.exceptions.FitsToolException;

/**
 * 
 * @author Stefan Schindler (schind85@gmail.com)
 */
public class HtmlParser  
{
	//private static final Logger LOG = LoggerFactory.getLogger(HtmlParser.class);
	public final static String xslt = Fits.FITS_HOME + "xml/htmlinfo/htmlinfo_to_fits.xslt";
	
	private Map<String, Long> tags = new HashMap<String, Long>();
	private Map<String, Long> objects = new HashMap<String, Long>();
	private long absoluteLinks = 0;
	private long relativeLinks = 0;
	private long frameSetCount = 0;
	private String htmlVersion = "";
	private String mimeType = "";
	private String name = "";
	private String encoding = ""; 
	
	// indicates, if doctype was set with !doctype tag
	private boolean doctypeSet = false;
	
	// indicates, if a tag was found (--> html file)
	private boolean foundTag = false;
	
	public Map<String, Long> getTags() {
		return tags;
	}
	
	public Map<String, Long> getObjects() {
		return objects;
	}

	public static final String PARSER_VERSION = "0.7";
	
	public HtmlParser(String filename)
	{
		try {
			// remove HtmlParser feedback with 2nd argument
			Parser parser = new Parser(filename, Parser.DEVNULL);			

			// parse all nodes			
			for (NodeIterator i = parser.elements (); i.hasMoreNodes (); )
				if(processNode (i.nextNode ()) == false)
					break;
			
		// no doctype set through !doctype tag
			if(this.doctypeSet == false)
				setTypeManually();
			
			setEncoding(parser.getEncoding());
			
		} catch (ParserException e) {
			e.printStackTrace();
		}
	}	
		
	private boolean processNode (Node node)
	{
		if (node instanceof TextNode)
		{			
			// downcast to TextNode
			// TextNode text = (TextNode)node;
	      //System.out.println ("TEXT NODE: " + text.getText ());
		}		
		else if (node instanceof TagNode)
		{			
			foundTag = true;
			
	      // downcast to TagNode
	      TagNode tag = (TagNode)node;	      	     
	      
	      if(tag instanceof DoctypeTag)
	      {	      	
	      	setVersionAndType((DoctypeTag)tag);
	      }else		// TODO atm: don't increment !doctype tag, maybe change this?
	      {
	      	// count tagnames
		      incrementMapCount(tags, tag.getTagName(), 1);
	      }
	      
	      if(tag instanceof LinkTag)
	      {
	      	LinkTag lTag = (LinkTag)tag;
	      		      	
	      	if(checkLinkRef(lTag.extractLink()) == true)
	      		absoluteLinks++;
	      	else
	      		relativeLinks++;
	      }	      	
	      	      
	      if(tag instanceof ObjectTag)
	      {
	      	/*
	      	 * The type attribute lets the author define the MIME type of the data used in the object the file thats specified 
	      	 * in the data attribute. This is slightly different from the codetype attribute, which is used to specify 
	      	 * the MIME type of the object itself. If the server sends data with the appropriate MIME type, this attribute may be omitted.
	      	 */
	      	
	      	ObjectTag oTag = (ObjectTag)tag;
	      
	      	if(oTag.getObjectType() == null)
	      		incrementMapCount(objects, "undefined", 1);
	      	else
	      		incrementMapCount(objects, oTag.getObjectType(), 1);
	      }
	      
	      if(tag instanceof FrameSetTag)
	      {	      	
	      	frameSetCount++;
	      }
	      
	      // process recursively (nodes within nodes) via getChildren()
	      NodeList nl = tag.getChildren();
	      if (nl != null)
	      {
	      	try 
	      	{
					for (NodeIterator i = nl.elements (); i.hasMoreNodes() ; )						
						processNode (i.nextNode ());
				} catch (ParserException e) {					
					e.printStackTrace();
				}
	      }
		}
		return true;
	 }
	
	protected void incrementMapCount(Map<String, Long> map, String key, long increment) {
		// not safe -- removed
		// omit characters that would lead to xslt errors ('<', '>', '&', '?', @, ", ')
		// String tagName = key.toLowerCase().replaceAll("[\\?<>&@'\"]", "");
		
		// except the above, remove all non-letters and non-numbers
		String tagName = key.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
		// remove leading digits
		tagName = tagName.replaceAll("^[0-9]*", "");
		
		if(tagName.length() > 0)
		{
	       Long lw = (Long)map.get(tagName);
	       if(lw == null) {
	      	 map.put(tagName, new Long(increment));
	       } else {
	      	 map.put(tagName, lw.longValue() + increment);
	       }
		}
   }
	
	/**
	 * 
	 * @param link
	 * @return if the link is absolute (true) or relative (false)
	 */
	private boolean checkLinkRef(String link)
	{
		/*
		 * relative links look like this:
		 * file://localhost/aktuelles
		 * ./pages/page1
		 * 
		 * absolute links look like this:
		 * http://www.tuwien.ac.at/
		 */
		if (link.matches("http://.*"))
			return true;
		return false;
	}
	
	public Document createXml() throws FitsToolException
	{
		// no xml file
		if(foundTag == false)
			return new Document(new Element("htmlInfo").addContent(new Element("error").setText("No XML File")));
		// no html file		
		if(!this.getTags().containsKey("html"))
			return new Document(new Element("htmlInfo").addContent(new Element("error").setText("No HTML File")));
      // xml root
      Element root = new Element("htmlInfo");
      // version element
      Element version = new Element("version");
      version.setText(htmlVersion);
      root.addContent(version);
      // mimetype element
      Element mime = new Element("mimetype");
      mime.setText(mimeType);
      root.addContent(mime);
      
      // encoding element
      Element encoding = new Element("encoding");
      encoding.setText(this.encoding);
      root.addContent(encoding);
      
      // name element
      root.addContent(new Element("name").setText(name));

      // xml tags element
      if(this.getTags().size() > 0)
      {
	      Element tags = new Element("tags");		
			root.addContent(tags);
							
			Map<String, Long> tagMap = this.getTags();
			
			for(String tagStr : tagMap.keySet())
			{
				Element tag = new Element("tag");
				Element tagName = new Element("name");
				Element tagCount = new Element("occurences");
				
				// set tag name and number of occurrences in file
				tagName.setText(tagStr);
				tagCount.setText(tagMap.get(tagStr).toString());
				
				tag.addContent(tagName);
				tag.addContent(tagCount);
				
				tags.addContent(tag);
			}
      }
		
		// xml objects element
		if(this.getObjects().size() > 0)
		{
			Element objects = new Element("objects");
			root.addContent(objects);
			Map<String, Long> objectMap = this.getObjects();
			
			for(String objStr : objectMap.keySet())
			{
				Element obj = new Element("object");
				Element tagType = new Element("type");
				Element tagCount = new Element("occurences");
				
				// set tag name and number of occurrences in file
				tagType.setText(objStr);
				tagCount.setText(objectMap.get(objStr).toString());
				
				obj.addContent(tagType);
				obj.addContent(tagCount);
				
				objects.addContent(obj);
			}
		}
		
		// link count
		Element linkCount = new Element("linkCount");
		root.addContent(linkCount);
		linkCount.addContent(new Element("absolute").setText(""+this.absoluteLinks));
		linkCount.addContent(new Element("relative").setText(""+this.relativeLinks));
				
		return new Document(root);
	}
	
	/**
	 * Method tries to set version an type of the current html file by using
	 * common doctype definitions
	 * 
	 * @param tag the DoctypeTag
	 */
	private void setVersionAndType(DoctypeTag tag)
	{		
		// html5		
		if(tag.getText().equalsIgnoreCase("!doctype html"))
		{
			this.mimeType = "text/html";
			this.name = "Hypertext Markup Language";
			this.htmlVersion = "5";
		}else
		{
			Pattern pat = Pattern.compile("DTD\\s+([A-Za-z]*)\\s+([A-Za-z]*\\s+)?([0-9]*\\.[0-9]*)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pat.matcher(tag.getText());
			if(matcher.find())
			{
				if(matcher.group(1).equalsIgnoreCase("html"))
				{
					this.mimeType = "text/html";
					this.name = "Hypertext Markup Language";
				}
				else if(matcher.group(1).equalsIgnoreCase("xhtml"))
				{
					this.mimeType = "application/xhtml+xml";
					this.name = "Extensible Hypertext Markup Language";
				}
				else
				{
					this.mimeType = "text/html";
					this.name = "Hypertext Markup Language";
				}
				this.htmlVersion = matcher.group(3);
			}else
			{
				this.mimeType = "text/html";
				this.htmlVersion = "0";
			}
		}
		this.doctypeSet = true;
	}
	
	private void setEncoding(String encoding)
	{
		this.encoding = encoding;
	}
	
	private void setTypeManually()
	{
		// html tag found -> html file
		if(this.getTags().containsKey("html"))
		{
			this.mimeType = "text/html";
			this.name = "Hypertext Markup Language";
		}
	}
}