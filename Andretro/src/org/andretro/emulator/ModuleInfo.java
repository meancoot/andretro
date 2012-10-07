package org.andretro.emulator;

import android.content.res.*;
import android.os.*;

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
	public String[] extensions;
	
	public String fileName;
	public String dataPath;
	
	public ModuleInfo(final AssetManager aAssets, final File aFile)
	{
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try
        {
        	// Read XML
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
        	
        	// Quick check
        	if(null == name || null == shortName || null == libraryName || null == extensions)
        	{
        		throw new Exception("Not all elements present in xml");
        	}
        	
        	// Build Directories
        	dataPath = Environment.getExternalStorageDirectory().getPath() + "/andretro/" + libraryName;
        	new File(dataPath + "/Games").mkdirs();
        }
        catch(final Exception e)
        {
        	throw new RuntimeException(e);
        }
	}
	
	public String getDataName()
	{
		return libraryName;
	}
	
	public String getDataPath()
	{
		return dataPath;
	}
	
	public boolean isFileValid(File aFile)
	{
    	final String path = aFile.getAbsolutePath(); 
        final int dot = path.lastIndexOf(".");
        final String extension = (dot < 0) ? null : path.substring(dot + 1);

    	return (null == extension) ? false : (0 <= Arrays.binarySearch(extensions, extension));
	}
}
