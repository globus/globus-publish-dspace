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
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.MetadataExposure;
import org.dspace.app.util.Util;
import org.dspace.app.webui.util.StyleSelection;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.browse.BrowseException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;
import org.dspace.globus.GlobusWebAppIntegration;

/**
 * <P>
 * JSP tag for displaying an item.
 * </P>
 * <P>
 * The fields that are displayed can be configured in <code>dspace.cfg</code>
 * using the <code>webui.itemdisplay.(style)</code> property. The form is
 * </P>
 *
 * <PRE>
 *
 * &lt;schema prefix&gt;.&lt;element&gt;[.&lt;qualifier&gt;|.*][(date)|(link)], ...
 *
 * </PRE>
 *
 * <P>
 * For example:
 * </P>
 *
 * <PRE>
 *
 * dc.title = Dublin Core element 'title' (unqualified)
 * dc.title.alternative = DC element 'title', qualifier 'alternative'
 * dc.title.* = All fields with Dublin Core element 'title' (any or no qualifier)
 * dc.identifier.uri(link) = DC identifier.uri, render as a link
 * dc.date.issued(date) = DC date.issued, render as a date
 * dc.identifier.doi(doi) = DC identifier.doi, render as link to https://doi.org
 * dc.identifier.hdl(handle) = DC identifier.hanlde, render as link to http://hdl.handle.net
 * dc.relation.isPartOf(resolver) = DC relation.isPartOf, render as link to the base url of the resolver
 *                                  according to the specified urn in the metadata value (doi:xxxx, hdl:xxxxx,
 *                                  urn:issn:xxxx, etc.)
 *
 * </PRE>
 *
 * <P>
 * When using "resolver" in webui.itemdisplay to render identifiers as resolvable
 * links, the base URL is taken from <code>webui.resolver.<n>.baseurl</code>
 * where <code>webui.resolver.<n>.urn</code> matches the urn specified in the metadata value.
 * The value is appended to the "baseurl" as is, so the baseurl need to end with slash almost in any case.
 * If no urn is specified in the value it will be displayed as simple text.
 *
 * <PRE>
 *
 * webui.resolver.1.urn = doi
 * webui.resolver.1.baseurl = https://doi.org/
 * webui.resolver.2.urn = hdl
 * webui.resolver.2.baseurl = http://hdl.handle.net/
 *
 * </PRE>
 *
 * For the doi and hdl urn defaults values are provided, respectively https://doi.org/ and
 * http://hdl.handle.net/ are used.<br>
 *
 * If a metadata value with style: "doi", "handle" or "resolver" matches a URL
 * already, it is simply rendered as a link with no other manipulation.
 * </P>
 *
 * <PRE>
 *
 * <P>
 * If an item has no value for a particular field, it won't be displayed. The
 * name of the field for display will be drawn from the current UI dictionary,
 * using the key:
 * </P>
 *
 * <PRE>
 *
 * &quot;metadata.&lt;style.&gt;.&lt;field&gt;&quot;
 *
 * e.g. &quot;metadata.thesis.dc.title&quot; &quot;metadata.thesis.dc.contributor.*&quot;
 * &quot;metadata.thesis.dc.date.issued&quot;
 *
 *
 * if this key is not found will be used the more general one
 *
 * &quot;metadata.&lt;field&gt;&quot;
 *
 * e.g. &quot;metadata.dc.title&quot; &quot;metadata.dc.contributor.*&quot;
 * &quot;metadata.dc.date.issued&quot;
 *
 * </PRE>
 *
 * <P>
 * You need to specify which strategy use for select the style for an item.
 * </P>
 *
 * <PRE>
 *
 * plugin.single.org.dspace.app.webui.util.StyleSelection = \
 *                      org.dspace.app.webui.util.CollectionStyleSelection
 *                      #org.dspace.app.webui.util.MetadataStyleSelection
 *
 * </PRE>
 *
 * <P>
 * With the Collection strategy you can also specify which collections use which
 * views.
 * </P>
 *
 * <PRE>
 *
 * webui.itemdisplay.&lt;style&gt;.collections = &lt;collection handle&gt;, ...
 *
 * </PRE>
 *
 * <P>
 * FIXME: This should be more database-driven
 * </P>
 *
 * <PRE>
 *
 * webui.itemdisplay.thesis.collections = 123456789/24, 123456789/35
 *
 * </PRE>
 *
 * <P>
 * With the Metadata strategy you MUST specify which metadata use as name of the
 * style.
 * </P>
 *
 * <PRE>
 *
 * webui.itemdisplay.metadata-style = schema.element[.qualifier|.*]
 *
 * e.g. &quot;dc.type&quot;
 *
 * </PRE>
 *
 * @author Robert Tansley
 * @version $Revision$
 */
public class ItemTag extends TagSupport
{
    private static final String HANDLE_DEFAULT_BASEURL = "http://hdl.handle.net/";

    private static final String DOI_DEFAULT_BASEURL = "https://doi.org/";

    /** Item to display */
    private transient Item item;

    /** Collections this item appears in */
    private transient Collection[] collections;

    /** The style to use - "default" or "full" */
    private String style;

    /** Should we have a link to the dataset */
    private boolean provideLinkToDataset = true;

    /** Whether to show preview thumbs on the item page */
    private boolean showThumbs;

    /** Default DC fields to display, in absence of configuration */
    private static String defaultFields = "dc.title, dc.title.alternative, dc.contributor.*, dc.subject, dc.date.issued(date), dc.publisher, dc.identifier.citation, dc.relation.ispartofseries, dc.description.abstract, dc.description, dc.identifier.govdoc, dc.identifier.uri(link), dc.identifier.isbn, dc.identifier.issn, dc.identifier.ismn, dc.identifier";

    /** log4j logger */
    private static Logger log = Logger.getLogger(ItemTag.class);

    private StyleSelection styleSelection = (StyleSelection) PluginManager.getSinglePlugin(StyleSelection.class);

    /** Hashmap of linked metadata to browse, from dspace.cfg */
    private static Map<String,String> linkedMetadata;

    /** Hashmap of urn base url resolver, from dspace.cfg */
    private static Map<String,String> urn2baseurl;

    /** regex pattern to capture the style of a field, ie <code>schema.element.qualifier(style)</code> */
    private Pattern fieldStylePatter = Pattern.compile(".*\\((.*)\\)");

    private static final long serialVersionUID = -3841266490729417240L;

    static {
        int i;

        linkedMetadata = new HashMap<String, String>();
        String linkMetadata;

        i = 1;
        do {
            linkMetadata = ConfigurationManager.getProperty("webui.browse.link."+i);
            if (linkMetadata != null) {
                String[] linkedMetadataSplit = linkMetadata.split(":");
                String indexName = linkedMetadataSplit[0].trim();
                String metadataName = linkedMetadataSplit[1].trim();
                linkedMetadata.put(indexName, metadataName);
            }

            i++;
        } while (linkMetadata != null);

        urn2baseurl = new HashMap<String, String>();

        String urn;
        i = 1;
        do {
            urn = ConfigurationManager.getProperty("webui.resolver."+i+".urn");
            if (urn != null) {
                String baseurl = ConfigurationManager.getProperty("webui.resolver."+i+".baseurl");
                if (baseurl != null){
                    urn2baseurl.put(urn, baseurl);
                } else {
                    log.warn("Wrong webui.resolver configuration, you need to specify both webui.resolver.<n>.urn and webui.resolver.<n>.baseurl: missing baseurl for n = "+i);
                }
            }

            i++;
        } while (urn != null);

        // Set sensible default if no config is found for doi & handle
        if (!urn2baseurl.containsKey("doi")){
            urn2baseurl.put("doi",DOI_DEFAULT_BASEURL);
        }

        if (!urn2baseurl.containsKey("hdl")){
            urn2baseurl.put("hdl",HANDLE_DEFAULT_BASEURL);
        }
    }

    public ItemTag()
    {
        super();
        getThumbSettings();
    }

    public int doStartTag() throws JspException
    {
        try
        {
            if (style == null || style.equals(""))
            {
                style = styleSelection.getStyleForItem(item);
            }

            if (style.equals("full"))
            {
                renderFull();
            }
            else
            {
                render();
            }
        }
        catch (SQLException sqle)
        {
            throw new JspException(sqle);
        }
        catch (IOException ie)
        {
            throw new JspException(ie);
        }
        catch (DCInputsReaderException ex)
        {
            throw new JspException(ex);
        }


        return SKIP_BODY;
    }

    /**
     * Get the item this tag should display
     *
     * @return the item
     */
    public Item getItem()
    {
        return item;
    }

    /**
     * Set the item this tag should display
     *
     * @param itemIn
     *            the item to display
     */
    public void setItem(Item itemIn)
    {
        item = itemIn;
    }

    /**
     * Get the collections this item is in
     *
     * @return the collections
     */
    public Collection[] getCollections()
    {
        return (Collection[]) ArrayUtils.clone(collections);
    }

    /**
     * Set the collections this item is in
     *
     * @param collectionsIn
     *            the collections
     */
    public void setCollections(Collection[] collectionsIn)
    {
        collections = (Collection[]) ArrayUtils.clone(collectionsIn);
    }

    /**
     * Get the style this tag should display
     *
     * @return the style
     */
    public String getStyle()
    {
        return style;
    }

    /**
     * Set the style this tag should display
     *
     * @param styleIn
     *            the Style to display
     */
    public void setStyle(String styleIn)
    {
        style = styleIn;
    }


    public void setLinkToDataset(boolean value)
    {
        this.provideLinkToDataset = value;
    }

    public void release()
    {
        style = "default";
        item = null;
        collections = null;
    }

    /**
     * Render an item in the given style
     */
    private void render() throws IOException, SQLException, DCInputsReaderException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        Context context = UIUtil.obtainContext(request);
        Locale sessionLocale = UIUtil.getSessionLocale(request);
        String configLine = styleSelection.getConfigurationForStyle(style);

        if (configLine == null)
        {
            configLine = defaultFields;
        }

        out.println("<table class=\"table itemDisplayTable\">");

        /*
         * Break down the configuration into fields and display them
         *
         * FIXME?: it may be more efficient to do some processing once, perhaps
         * to a more efficient intermediate class, but then it would become more
         * difficult to reload the configuration "on the fly".
         */
        StringTokenizer st = new StringTokenizer(configLine, ",");

        while (st.hasMoreTokens())
        {
        	String field = st.nextToken().trim();
            boolean isDate = false;
            boolean isLink = false;
            boolean isResolver = false;
            boolean isNoBreakLine = false;
            boolean isDisplay = false;

            String style = null;
            Matcher fieldStyleMatcher = fieldStylePatter.matcher(field);
            if (fieldStyleMatcher.matches()){
                style = fieldStyleMatcher.group(1);
            }

            String browseIndex;
            try
            {
                browseIndex = getBrowseField(field);
            }
            catch (BrowseException e)
            {
                log.error(e);
                browseIndex = null;
            }

            // Find out if the field should rendered with a particular style

            if (style != null)
            {
                isDate = style.contains("date");
                isLink = style.contains("link");
				isNoBreakLine = style.contains("nobreakline");
				isDisplay = style.equals("inputform");
                isResolver = style.contains("resolver") || urn2baseurl.keySet().contains(style);
                field = field.replaceAll("\\("+style+"\\)", "");
            }

            // Get the separate schema + element + qualifier

            String[] eq = field.split("\\.");
            String schema = eq[0];
            String element = eq[1];
            String qualifier = null;
            if (eq.length > 2 && eq[2].equals("*"))
            {
                qualifier = Item.ANY;
            }
            else if (eq.length > 2)
            {
                qualifier = eq[2];
            }

            // check for hidden field, even if it's configured..
            if (MetadataExposure.isHidden(context, schema, element, qualifier))
            {
                continue;
            }

            // FIXME: Still need to fix for metadata language?
            DCValue[] values = item.getMetadata(schema, element, qualifier, Item.ANY);

            if (values.length > 0)
            {
                out.print("<tr><td class=\"metadataFieldLabel\">");

                String label = null;
                try
                {
                    label = I18nUtil.getMessage("metadata."
                            + ("default".equals(this.style) ? "" : this.style + ".") + field,
                            context);
                }
                catch (MissingResourceException e)
                {
                    // if there is not a specific translation for the style we
                    // use the default one
                    label = LocaleSupport.getLocalizedMessage(pageContext,
                            "metadata." + field);
                }

                out.print(label);
                out.print(":&nbsp;</td><td class=\"metadataFieldValue\">");

                //If the values are in controlled vocabulary and the display value should be shown
                if (isDisplay){
                    List<String> displayValues = new ArrayList<String>();


                    displayValues = Util.getControlledVocabulariesDisplayValueLocalized(item, values, schema, element, qualifier, sessionLocale);

                        if (displayValues != null && !displayValues.isEmpty())
                        {
                            for (int d = 0; d < displayValues.size(); d++)
                            {
                                out.print(displayValues.get(d));
                                if (d<displayValues.size()-1)  out.print(" <br/>");

                            }
                        }
                    out.print("</td>");
                    continue;
                 }
                for (int j = 0; j < values.length; j++)
                {
                    if (values[j] != null && values[j].value != null)
                    {
                        if (j > 0)
                        {
                            if (isNoBreakLine)
                            {
                                String separator = ConfigurationManager
                                        .getProperty("webui.itemdisplay.nobreakline.separator");
                                if (separator == null)
                                {
                                    separator = ";&nbsp;";
                                }
                                out.print(separator);
                            }
                            else
                            {
                                out.print("<br />");
                            }
                        }

                        if (isLink)
                        {
                            out.print("<a href=\"" + values[j].value + "\">"
                                    + Utils.addEntities(values[j].value) + "</a>");
                        }
                        else if (isDate)
                        {
                            DCDate dd = new DCDate(values[j].value);

                            // Parse the date
                            out.print(UIUtil.displayDate(dd, false, false, (HttpServletRequest)pageContext.getRequest()));
                        }
                        else if (isResolver)
                        {
                            String value = values[j].value;
                            if (value.startsWith("http://")
                                    || value.startsWith("https://")
                                    || value.startsWith("ftp://")
                                    || value.startsWith("ftps://"))
                            {
                                // Already a URL, print as if it was a regular link
                                out.print("<a href=\"" + value + "\">"
                                        + Utils.addEntities(value) + "</a>");
                            }
                            else
                            {
                                String foundUrn = null;
                                if (!style.equals("resolver"))
                                {
                                    foundUrn = style;
                                }
                                else
                                {
                                    for (String checkUrn : urn2baseurl.keySet())
                                    {
                                        if (value.startsWith(checkUrn))
                                        {
                                            foundUrn = checkUrn;
                                        }
                                    }
                                }

                                if (foundUrn != null)
                                {

                                    if (value.startsWith(foundUrn + ":"))
                                    {
                                        value = value.substring(foundUrn.length()+1);
                                    }

                                    String url = urn2baseurl.get(foundUrn);
                                    out.print("<a href=\"" + url
                                            + value + "\">"
                                            + Utils.addEntities(values[j].value)
                                            + "</a>");
                                }
                                else
                                {
                                    out.print(value);
                                }
                            }

                        }
                        else if (browseIndex != null)
                        {
	                        String argument, value;
	                        if ( values[j].authority != null &&
	                                            values[j].confidence >= MetadataAuthorityManager.getManager()
	                                                .getMinConfidence( values[j].schema,  values[j].element,  values[j].qualifier))
	                        {
	                            argument = "authority";
	                            value = values[j].authority;
	                        }
	                        else
	                        {
	                            argument = "value";
	                            value = values[j].value;
	                        }
	                    	out.print("<a class=\"" + ("authority".equals(argument)?"authority ":"") + browseIndex + "\""
	                                                + "href=\"" + request.getContextPath() + "/browse?type=" + browseIndex + "&amp;" + argument + "="
	                    				+ URLEncoder.encode(value, "UTF-8") + "\">" + Utils.addEntities(values[j].value)
	                    				+ "</a>");
	                    }
                        else
                        {
                            out.print(Utils.addEntities(values[j].value));
                        }
                    }
                }

                out.println("</td></tr>");
            }
        }

        listCollections();

        out.println("</table><br/>");

        listBitstreams();

        if (ConfigurationManager
                .getBooleanProperty("webui.licence_bundle.show"))

        {
            out.println("<br/><br/>");
            showLicence();
        }
    }

    /**
     * Render full item record
     */
    private void renderFull() throws IOException, SQLException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        Context context = UIUtil.obtainContext(request);

        // Get all the metadata
        DCValue[] values = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        out.println("<div class=\"panel panel-info\"><div class=\"panel-heading\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.full") + "</div>");

        // Three column table - DC field, value, language
        out.println("<table class=\"panel-body table itemDisplayTable\">");
        out.println("<tr><th id=\"s1\" class=\"standard\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.dcfield")
                + "</th><th id=\"s2\" class=\"standard\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.value")
                + "</th><th id=\"s3\" class=\"standard\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.lang")
                + "</th></tr>");

        for (int i = 0; i < values.length; i++)
        {
            if (!MetadataExposure.isHidden(context, values[i].schema, values[i].element, values[i].qualifier))
            {
                out.print("<tr><td headers=\"s1\" class=\"metadataFieldLabel\">");
                out.print(values[i].schema);
                out.print("." + values[i].element);

                if (values[i].qualifier != null)
                {
                    out.print("." + values[i].qualifier);
                }

                out.print("</td><td headers=\"s2\" class=\"metadataFieldValue\">");
                out.print(Utils.addEntities(values[i].value));
                out.print("</td><td headers=\"s3\" class=\"metadataFieldValue\">");

                if (values[i].language == null)
                {
                    out.print("-");
                }
                else
                {
                    out.print(values[i].language);
                }

                out.println("</td></tr>");
            }
        }

        listCollections();

        out.println("</table></div><br/>");

        listBitstreams();

        if (ConfigurationManager
                .getBooleanProperty("webui.licence_bundle.show"))
        {
            out.println("<br/><br/>");
            showLicence();
        }
    }

    /**
     * List links to collections if information is available
     */
    private void listCollections() throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        if (collections != null)
        {
            out.print("<tr><td class=\"metadataFieldLabel\">");
            if (item.getHandle()==null)  // assume workspace item
            {
                out.print(LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.submitted"));
            }
            else
            {
                out.print(LocaleSupport.getLocalizedMessage(pageContext,
                          "org.dspace.app.webui.jsptag.ItemTag.appears"));
            }
            out.print("</td><td class=\"metadataFieldValue\""+
            		(style.equals("full")?"colspan=\"2\"":"")
            		+">");

            for (int i = 0; i < collections.length; i++)
            {
                out.print("<a href=\"");
                out.print(request.getContextPath());
                out.print("/handle/");
                out.print(collections[i].getHandle());
                out.print("\">");
                out.print(collections[i].getMetadata("name"));
                out.print("</a><br/>");
            }

            out.println("</td></tr>");
        }
    }

    /**
     * Changed display to list link to globus Endpoint
     * rather than showing individual bitstreams
     *
     */
    private void listBitstreams() throws IOException
    {
        String epName = item.getGlobusEndpoint();
        String epPath = item.getGlobusSharePath();

        // If there's no epName, don't even display this panel
        if (epName == null) {
            return;
        }

        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        out.print("<div class=\"panel panel-info\">");
        out.println("<div class=\"panel-heading\">"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.files")
                + "</div>");


        out.println("<div class=\"panel-body\">");
        if (provideLinkToDataset) {
            out.println("<a href=\"" + GlobusWebAppIntegration.getWebAppFileManagerUrl(epName, epPath, "origin") + "\" target=\"GlobusOps\">" +epName + epPath + "</a>");
        } else {
            out.println(epName + epPath);
        }
        out.println("</div>");
        out.println("</div>");
    }
    private void getThumbSettings()
    {
        showThumbs = ConfigurationManager
                .getBooleanProperty("webui.item.thumbnail.show");
    }

    /**
     * Link to the item licence
     */
    private void showLicence() throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) pageContext
                .getRequest();

        Bundle[] bundles = null;
        try
        {
        	bundles = item.getBundles("LICENSE");
        }
        catch(SQLException sqle)
        {
        	throw new IOException(sqle.getMessage(), sqle);
        }

        out.println("<table align=\"center\" class=\"table attentionTable\"><tr>");

        out.println("<td class=\"attentionCell\"><p><strong>"
                + LocaleSupport.getLocalizedMessage(pageContext,
                        "org.dspace.app.webui.jsptag.ItemTag.itemprotected")
                + "</strong></p>");

        for (int i = 0; i < bundles.length; i++)
        {
            Bitstream[] bitstreams = bundles[i].getBitstreams();

            for (int k = 0; k < bitstreams.length; k++)
            {
                out.print("<div align=\"center\" class=\"standard\">");
                out.print("<strong><a class=\"btn btn-primary\" target=\"_blank\" href=\"");
                out.print(request.getContextPath());
                out.print("/retrieve/");
                out.print(bitstreams[k].getID() + "/");
                out.print(UIUtil.encodeBitstreamName(bitstreams[k].getName(),
                        Constants.DEFAULT_ENCODING));
                out
                        .print("\">"
                                + LocaleSupport
                                        .getLocalizedMessage(pageContext,
                                                "org.dspace.app.webui.jsptag.ItemTag.viewlicence")
                                + "</a></strong></div>");
            }
        }

        out.println("</td></tr></table>");
    }

    /**
     * Return the browse index related to the field. <code>null</code> if the field is not a browse field
     * (look for <cod>webui.browse.link.<n></code> in dspace.cfg)
     *
     * @param field
     * @return the browse index related to the field. Null otherwise
     * @throws BrowseException
     */
    private String getBrowseField(String field) throws BrowseException
    {
        for (String indexName : linkedMetadata.keySet())
        {
            StringTokenizer bw_dcf = new StringTokenizer(linkedMetadata.get(indexName), ".");

            String[] bw_tokens = { "", "", "" };
            int i = 0;
            while(bw_dcf.hasMoreTokens())
            {
                bw_tokens[i] = bw_dcf.nextToken().toLowerCase().trim();
                i++;
            }
            String bw_schema = bw_tokens[0];
            String bw_element = bw_tokens[1];
            String bw_qualifier = bw_tokens[2];

            StringTokenizer dcf = new StringTokenizer(field, ".");

            String[] tokens = { "", "", "" };
            int j = 0;
            while(dcf.hasMoreTokens())
            {
                tokens[j] = dcf.nextToken().toLowerCase().trim();
                j++;
            }
            String schema = tokens[0];
            String element = tokens[1];
            String qualifier = tokens[2];
            if (schema.equals(bw_schema) // schema match
                    && element.equals(bw_element) // element match
                    && (
                              (bw_qualifier != null) && ((qualifier != null && qualifier.equals(bw_qualifier)) // both not null and equals
                                      || bw_qualifier.equals("*")) // browse link with jolly
                           || (bw_qualifier == null && qualifier == null)) // both null
                        )
            {
                return indexName;
            }
        }
        return null;
    }
}
