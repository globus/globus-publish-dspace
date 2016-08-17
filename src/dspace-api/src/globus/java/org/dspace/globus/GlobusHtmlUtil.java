/*******************************************************************************
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
 *******************************************************************************/


package org.dspace.globus;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 */
public class GlobusHtmlUtil
{

    /** Make one static so we don't have to new one every time */
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Map<String, String> EMPTY_STRING_MAP = new HashMap<String, String>();

    public static String tag(String tagName, boolean endTag, String propList,
            String... content)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<");
        buf.append(tagName);
        String[] tagProps = EMPTY_STRING_ARRAY; // Init it this way so its not
                                                // null but empty if no tags
        if (propList != null)
        {
            tagProps = propList.split(",");
        }
        for (int i = 0; i < tagProps.length; i++)
        {
            buf.append(" ");
            String prop = tagProps[i].trim();
            String[] parts = prop.split("=");
            String propName = parts[0].trim();
            String propVal = null;
            if (parts.length > 1)
            {
                propVal = parts[1].trim();
            }
            buf.append(propName);
            if (propVal != null && propVal.length() > 0)
            {
                buf.append('=');
                buf.append('"');
                buf.append(propVal);
                buf.append('"');
            }
        }
        if (content != null && content.length > 0)
        {
            buf.append(">");
            for (String contentString : content)
            {
                buf.append(contentString);
            }
            if (endTag)
            {
                buf.append("</");
                buf.append(tagName);
                buf.append(">");
            }
        }
        else if (endTag)
        {
            buf.append("/>");
        }
        else
        {
            buf.append(">");
        }
        buf.append("\n");
        return buf.toString();
    }

    public static String tag(String tagName, String propList, String... content)
    {
        return tag(tagName, true, propList, content);
    }

    public static String tag(String tagName, boolean endTag,
            Map<String, String> tagAttrs, String... content)
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<");
        buf.append(tagName);

        // If null attributes, just create an empty one for purposes of loop
        if (tagAttrs == null) {
            tagAttrs = EMPTY_STRING_MAP;
        }

        Set<Entry<String, String>> tagEntries = tagAttrs.entrySet();
        for (Entry<String, String> tag : tagEntries)
        {
            buf.append(" ");
            String propName = tag.getKey().trim();
            buf.append(propName);
            String propVal = tag.getValue();
            if (propVal != null && propVal.length() > 0)
            {
                buf.append('=');
                buf.append('"');
                buf.append(propVal);
                buf.append('"');
            }
        }
        if (content != null && content.length > 0)
        {
            buf.append(">");
            for (String contentString : content)
            {
                buf.append(contentString);
            }
            if (endTag)
            {
                buf.append("</");
                buf.append(tagName);
                buf.append(">");
            }
        }
        else if (endTag)
        {
            buf.append("/>");
        }
        else
        {
            buf.append(">");
        }
        buf.append("\n");
        return buf.toString();
    }

    public static String div(boolean endTag, String propList, String... content)
    {
        return tag("div", endTag, propList, content);
    }

    public static String div(String propList, String... content)
    {
        return div(true, propList, content);
    }

    public static String label(boolean endTag, String propList,
            String... content)
    {
        return tag("label", endTag, propList, content);
    }

    public static String label(String propList, String... content)
    {
        return label(true, propList, content);
    }

    public static String span(boolean endTag, String propList,
            String... content)
    {
        return tag("span", endTag, propList, content);
    }

    public static String span(String propList, String... content)
    {
        return span(true, propList, content);
    }

    public static String button(boolean endTag, String propList,
            String... content)
    {
        return tag("button", endTag, propList, content);
    }

    public static String button(String propList, String... content)
    {
        return button(true, propList, content);
    }

    public static String ul(boolean endTag, String propList, String... content)
    {
        return tag("ul", endTag, propList, content);
    }

    public static String ul(String propList, String... content)
    {
        return ul(true, propList, content);
    }

    public static String li(String propList, String... content)
    {
        return tag("li", true, propList, content);
    }

    public static String form(String propList, String... content)
    {
        return tag("form", propList, content);
    }

    public static String input(String propList, String... content)
    {
        return tag("input", propList, content);
    }

    public static String br()
    {
        return tag("br", true, "");
    }
}
