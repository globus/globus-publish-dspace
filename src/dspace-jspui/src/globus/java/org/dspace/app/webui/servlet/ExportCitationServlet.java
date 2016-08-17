/**
 * This file is a modified version of a DSpace file.
 * All modifications are subject to the following copyright and license.
 * 
 * Copyright 2016 University of Chicago. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.content.Item;
import org.dspace.core.LogManager;
import org.dspace.globus.Globus;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.content.DCValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
/**
 * Servlet that exports a citation
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class ExportCitationServlet extends DSpaceServlet
{
    private static final int BYTES_DOWNLOAD = 1024;
    private static Logger log = Logger.getLogger(ExportCitationServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "export citation", ""));
        int id = UIUtil.getIntParameter(request, "id");
        String format = request.getParameter("format").trim();

        // get collection
    	Item item = Item.find(context, id);
    	if (item == null){
    		return;
    	}

    	// Make sure the user can read this item.
    	AuthorizeManager.authorizeAction(context, item, Constants.READ);

        String identifier = item.getUri();

    	String title = "";
    	String year = "";
    	ArrayList<String> authors = new ArrayList<String>();

    	DCValue[] titleValue = item.getMetadata("dc","title", null, Item.ANY);
    	if (titleValue.length != 0)	{
    		title = titleValue[0].value;
    	} else	{
    		title = "Item " + item.getHandle();
    	}
    	DCValue[] authorsValue = item.getMetadata("dc", "contributor", "author", Item.ANY);
    	if (authorsValue.length != 0) {
    		for (DCValue a: authorsValue){
    			authors.add(a.value);
    		}
    	}
    	DCValue[] dateValue = item.getMetadata("dc", "date", "issued", Item.ANY);
    	if (dateValue.length != 0)	{
    		year = dateValue[0].value;
    		if (year.indexOf("-") > -1){
    			year = year.substring(0, year.indexOf("-"));
    		}
    	}
    	String citation = "";
    	String ext = ".txt";
    	if (format.equalsIgnoreCase("bibtex")){
	      		citation = createBibtex(title, authors, year, identifier);
	    		ext = ".bib";
    	} else if (format.equalsIgnoreCase("ris")){
	    		citation = createRIS(title, authors, year, identifier);
	    		ext = ".ris";
    	} else {
	    		citation = createPlain(title, authors, year, identifier);
    	}
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment;filename=globus-" + id + ext);

        InputStream input = new ByteArrayInputStream(citation.getBytes("UTF8"));

        int read = 0;
        byte[] bytes = new byte[BYTES_DOWNLOAD];
        OutputStream os = response.getOutputStream();

        while ((read = input.read(bytes)) != -1) {
            os.write(bytes, 0, read);
        }
        os.flush();
        os.close();
    }

    private String createBibtex(String title, ArrayList<String> authors, String year, String identifier){
    	String authorString = "";
    	String bibkey = "";
    	for (String a : authors){
    		if (bibkey.equals("")){
    			if (a.indexOf(",") > -1){
    				bibkey = a.substring(0, a.indexOf(","));
    			} else {
    				bibkey = a;
    			}
    		}
    		authorString += a + " and ";
    	}
    	if (authorString.length() > 3){
    		authorString= authorString.substring(0, authorString.length()-5);
    	}
    	return "@misc{" + bibkey + ":" + year +  ",\n" +
    			"  author = {" + authorString + "},\n" +
    			"  title = {" + title + "},\n" +
    			"  year = {" + year + "},\n" +
    			"  howpublished= {" + identifier + "}\n" +
    			"} ";

    }
    private String createPlain(String title, ArrayList<String> authors, String year, String identifier){
    	String authorString = "";
    	for (String a : authors){
    		authorString += a + "; ";
    	}
    	if (authorString.length() > 2){
    		authorString= authorString.substring(0, authorString.length()-2) + ", ";
    	}
    	return authorString + "\"" + title + ",\" " + year  + ", " + identifier + ".";
    }
    private String createRIS(String title, ArrayList<String> authors, String year, String identifier){
    	String ris = "TY  - DBASE\n";
    	ris += "T1  - Particulate matter contributions from agricultural tilling operations in an irrigated desert region\n";
    	for (String a : authors){
    		ris += "AU  - " + a + "\n";
    	}
    	ris += "PY  - " + year + "\n";
    	ris += "UR  - " + identifier + "\n";
    	return ris;
    }


}
