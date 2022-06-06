<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE sharedpackages PUBLIC "-//MMBase/DTD sharedpackages config 1.0//EN" "https://www.mmbase.org/dtd/sharedpackages_1_0.dtd"><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-1.0" prefix="mm" %><mm:cloud><mm:import externid="user">guest</mm:import><mm:import externid="password">guest</mm:import><mm:import externid="method">http</mm:import><mm:import externid="host"></mm:import><mm:import externid="callbackurl">none</mm:import>
<sharedpackages>
	<mm:nodelistfunction set="mmpm" name="getRemoteSharedPackages" referids="user,password,method,host,callbackurl">
	<package name="<mm:field name="name" />" type="<mm:field name="type" />" maintainer="<mm:field name="maintainer" />" version="<mm:field name="version" />" creation-date="6 Apr 2003 10:30" >
		<path><mm:field name="path" /></path>
		<description><mm:field name="description" /></description>
		<license type="<mm:field name="licensetype" />" version="<mm:field name="licenseversion" />" name="<mm:field name="licensename" />" />
		<releasenotes><mm:field name="releasenotes" /></releasenotes>
		<installationnotes><mm:field name="installationnotes" />
		</installationnotes>
		<screenshots>
<mm:field name="screenshots" />
		</screenshots>
		<starturls>
<mm:field name="starturls" />
		</starturls>
		<initiators>
<mm:field name="initiators" />
		</initiators>
		<supporters>
<mm:field name="supporters" />
		</supporters>
		<developers>
<mm:field name="developers" />
		</developers>
		<contacts>
<mm:field name="contacts" />
		</contacts>
		<mm:field name="type">
		<mm:compare value="bundle/basic">
		<mm:remove referid="tmpid" />
		<mm:remove referid="tmpversion" />
		<mm:remove referid="tmpprovider" />
		<mm:import id="tmpid"><mm:field name="id" /></mm:import>
		<mm:import id="tmpversion"><mm:field name="version" /></mm:import>
		<mm:import id="tmpprovider"><mm:field name="provider" /></mm:import>
		<includedpackages>
		<mm:functioncontainer>
		  <mm:param name="id" value="$tmpid" />
		  <mm:param name="version" value="$tmpversion" />
		  <mm:param name="provider" value="$tmpprovider" />
		  <mm:nodelistfunction set="mmpm" name="getBundleNeededPackages">
				<includedpackage name="<mm:field name="name" />" type="<mm:field name="type" />" maintainer="<mm:field name="maintainer" />" version="<mm:field name="version" />" creation-date="6 Apr 2003 10:30" packed="true">

		<description><mm:field name="description" /></description>
		<license type="<mm:field name="licensetype" />" version="<mm:field name="licenseversion" />" name="<mm:field name="licensename" />" />
		<releasenotes><mm:field name="releasenotes" /></releasenotes>
		<installationnotes><mm:field name="installationnotes" />
		</installationnotes>
		<initiators>
<mm:field name="initiators" />
		</initiators>
		<supporters>
<mm:field name="supporters" />
		</supporters>
		<developers>
<mm:field name="developers" />
		</developers>
		<contacts>
<mm:field name="contacts" />
		</contacts>
				</includedpackage>
		  </mm:nodelistfunction>
		</mm:functioncontainer>
		</includedpackages>
		</mm:compare>
		</mm:field>
	</package>
</mm:nodelistfunction></sharedpackages>
</mm:cloud>
