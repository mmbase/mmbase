<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="item">
		<xsl:apply-templates select="section"/>
	</xsl:template>
</xsl:stylesheet>
