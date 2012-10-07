package org.andretro.emulator;

import android.content.res.*;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class ModuleInfo
{
	public String name;
	public String shortName;
	public String libraryName;
	public String fileName;
	public String[] extensions;
	
	public ModuleInfo(final AssetManager aAssets, final File aFile)
	{
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try
        {
        	final SAXParser parser = factory.newSAXParser();
        	final InputStream file = aAssets.open(aFile.getName() + ".xml");

        	parser.parse(file, new DefaultHandler()
        	{
        		@Override public void startElement(String aURI, String aName, String aQualifiedName, Attributes aAttributes) throws SAXException
        		{
        			if("system".equals(aName))
        			{
        				name = aAttributes.getValue("", "fullname");
        				shortName = aAttributes.getValue("", "shortname");
        			}
        			else if("module".equals(aName))
        			{
        				libraryName = aAttributes.getValue("", "libraryname");
        				extensions = aAttributes.getValue("", "extensions").split("\\|");
        				Arrays.sort(extensions);
        			}
        		}
        	});
        }
        catch(final Exception e)
        {
        }

        fileName = aFile.getName();
        name = (null == name) ? aFile.getName() : name;
        shortName = (null == shortName) ? "UNK" : shortName;
        extensions = (null == extensions) ? new String[0] : extensions;
        libraryName = (null == libraryName) ? fileName : libraryName;
	}
	
	public String getDataName()
	{
		return fileName;
	}
	
	public boolean isFileValid(File aFile)
	{
    	final String path = aFile.getAbsolutePath(); 
        final int dot = path.lastIndexOf(".");
        final String extension = (dot < 0) ? null : path.substring(dot + 1);

        if(0 == extensions.length || aFile.isDirectory())
        {
        	return true;
        }
        else
        {
        	return (null == extension) ? false : (0 <= Arrays.binarySearch(extensions, extension));
        }
	}
}
