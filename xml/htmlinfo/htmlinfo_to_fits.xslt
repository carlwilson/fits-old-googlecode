<?xml version="1.0" ?>
<xsl:stylesheet version="1.0" 
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:template match="/">
		<xsl:if test="not(htmlInfo/error)">
			<fits xmlns="http://hul.harvard.edu/ois/xml/ns/fits/fits_output"> 
				<identification>
		  			<identity>
		  				<xsl:attribute name="format"><xsl:value-of select="htmlInfo/name" /></xsl:attribute>
		  				<xsl:attribute name="mimetype"><xsl:value-of select="htmlInfo/mimetype"/></xsl:attribute>		  												
				  		<version>
				  			<xsl:value-of select="htmlInfo/version" />
				  		</version>
		  			</identity>
		  		</identification>
				<metadata>
					<text>
						<xsl:for-each select="htmlInfo/tags/tag">
							<xsl:element name="{concat(name, 'TagOccurences')}">								
								<xsl:value-of select="occurences" />								
							</xsl:element>							
						</xsl:for-each>						
						<xsl:for-each select="htmlInfo/objects/object">
							<xsl:element name="{concat(type, 'ObjectOccurences')}">
								<xsl:value-of select="occurences" />
							</xsl:element>
						</xsl:for-each>
						<absoluteLinks><xsl:value-of select="htmlInfo/linkCount/absolute" /></absoluteLinks>
						<relativeLinks><xsl:value-of select="htmlInfo/linkCount/relative" /></relativeLinks>						
					</text>
				</metadata>
		    </fits>
	    </xsl:if>
  </xsl:template>
  
</xsl:stylesheet>