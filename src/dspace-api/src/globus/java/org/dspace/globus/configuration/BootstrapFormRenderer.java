/**
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


package org.dspace.globus.configuration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.globus.GlobusHtmlUtil;
import org.dspace.globus.configuration.Configurable.ConfigurableProperty;
import org.dspace.globus.configuration.Configurable.ConfigurablePropertyValueOptions;
import org.dspace.globus.configuration.Configurable.DataType;

/**
 *
 */
public class BootstrapFormRenderer implements ConfigurableRenderer
{
    /**
     *
     */
    private static final String ID_SPACE_REPLACEMENT_STRING = "___";

    private int labelWidth;

    private int controlWidth;

    /**
     *
     */
    public BootstrapFormRenderer(int labelWidth, int controlWidth)
    {
        this.labelWidth = labelWidth;
        this.controlWidth = controlWidth;
    }

    /**
     *
     */
    public BootstrapFormRenderer()
    {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.globus.configuration.Renderer#renderFrom(org.dspace.globus
     * .configuration.Configurable)
     */
    @Override
    public Object renderFrom(Configurable config, Context context,
            DSpaceObject dso)
    {
        StringBuffer buf = new StringBuffer();
        Iterator<String> ids = config.getPropIds();
        buf.append("<div id=\"" + divIdForConfig(config) + "\">");
        while (ids.hasNext())
        {
            String propId = ids.next();
            ConfigurableProperty configProp = config.getProperty(propId);
            Object value = null;
            // If the user doesn't provide a dso to lookup values, skip the value lookup
            if (dso != null) {
                value = GlobusConfigurationManager.getProperty(context,
                    propId, null, dso);
            }
            String controlName = getControlName(config, propId);

            appendFieldForProp(buf, controlName, configProp, value);
            buf.append("<br/>");
        }
        buf.append("</div>");
        return buf.toString();
    }

    /**
     * Create an id that can be used in tags for an input name. The constraint
     * is that we don't want any spaces in ids as they mess up processing in
     * other places.
     *
     * @param name
     * @return
     */
    public static String tagIdForName(String name)
    {
        if (name == null)
        {
            return null;
        }
        return name.replace(" ", ID_SPACE_REPLACEMENT_STRING);
    }

    public static String configNameForDivId(String divId)
    {
    	if (divId == null){
    		return null;
    	}

        int lastStep = divId.lastIndexOf(ID_SPACE_REPLACEMENT_STRING);
        if (lastStep >= 0) {
            divId = divId.substring(0, lastStep);
        }
        return nameForTagId(divId);
    }


    /**
     * Get the id to be used when creating the root div for the representation
     * of a particular Configurable. We incorporate the hash code of the
     * Configurable in the name so that if a single form or page is being
     * generated that contains more than one Configurable with the same name,
     * the div ids are unique.
     *
     * @param config
     * @return
     */
    public static String divIdForConfig(Configurable config)
    {
        if (config == null) {
            return null;
        }
        String divId = tagIdForName(config.getName());
        String configHashStr = String.valueOf(config.hashCode());
        if (divId == null) {
            divId = "";
        }
        divId = divId + ID_SPACE_REPLACEMENT_STRING + configHashStr;
        return divId;
    }

    public static String nameForTagId(String tagId)
    {
        if (tagId == null) {
            return null;
        }
        return tagId.replace(ID_SPACE_REPLACEMENT_STRING, " ");
    }

    /**
     * @param buf
     * @param fieldName
     * @param configProp
     * @param value
     */
    private void appendFieldForProp(StringBuffer buf, String fieldName,
            ConfigurableProperty configProp, Object value)
    {
        DataType dataType = configProp.dataType;
        String displayName = configProp.displayName;
        String placeholder = configProp.description;
        if (value == null)
        {
            value = configProp.defaultValue;
        }
        String valueAttr;
        String valueStr = getValueForField(configProp, value);
        if (valueStr != null)
        {
            valueAttr = "value=" + valueStr;
        }
        else
        {
            valueAttr = "";
        }

        String controlValue = null;
        String fieldId = tagIdForName(fieldName);
        // If there's no option enumerator, we create an input control, else we
        // do a select control
        if (configProp.valueOptions == null)
        {
            controlValue = inputControl(fieldId, dataType, valueAttr,
                    valueStr, placeholder);
        }
        else
        {
            controlValue = selectControl(fieldId, configProp.valueOptions,
                    valueStr);
        }

        if (configProp.required) {
            displayName = displayName + "*";
        }
        buf.append(GlobusHtmlUtil.div("class=row", GlobusHtmlUtil.label(
                "class=col-md-" + labelWidth + ",for=side_bar_text",
                displayName), GlobusHtmlUtil.span("class=col-md-"
                + controlWidth, controlValue)));
    }

    /**
     * @param valueOptions
     * @param currentValStr
     * @return
     */
    private String selectControl(String controlName,
            ConfigurablePropertyValueOptions valueOptions, String currentValStr)
    {
        String selectControl = "";
        String controlId = tagIdForName(controlName);
        List<Object> values = valueOptions.getValueOptions();
        if (values != null && values.size() > 0)
        {
            StringBuffer buf = new StringBuffer();
            for (Object value : values)
            {
                // There could, in general, be a null or empty value in the list. If so, we skip it
                if (value == null) {
                    continue;
                }
                // If this is a seperator, insert it now and move on to the next one
                if (valueOptions.GROUP_SEPERATOR.equals(value)) {
                    buf.append(GlobusHtmlUtil.tag("option", "disabled", "&mdash;&mdash;&mdash;"));
                    continue;
                }
                String optionValue = value.toString();
                String toDisplay = valueOptions.getDisplayValueForOption(value);
                if (toDisplay.length() == 0) {
                    continue;
                }
                String selected = "";
                if (optionValue.equals(currentValStr))
                {
                    selected = ",selected";
                }
                buf.append(GlobusHtmlUtil.tag("option", "value=" + tagIdForName(optionValue)
                        + selected, toDisplay));
            }
            selectControl = GlobusHtmlUtil
                    .tag("select", "class=form-control,name=" + controlName
                            + ",id=" + controlId, buf.toString());
        }
        return selectControl;
    }

    /**
     * @param controlName
     * @param dataType
     * @param valueAttr
     * @param valueStr
     * @param placeholder
     * @return
     */
    private String inputControl(String controlName, DataType dataType,
            String valueAttr, String valueStr, String placeholder)
    {
        String controlValue = null;
        String inputType = null;
        String classIdName = ",class=form-control,id=" + tagIdForName(controlName) + ",name="
                + controlName;
        String classIdNameVal = classIdName + "," + valueAttr;
        String placeholderAttr = "";
        if (placeholder != null)
        {
            placeholderAttr = ",placeholder=" + placeholder;
        }
        String checked = "";
        switch (dataType)
        {
        case STRING:
            if (inputType == null) {
                inputType = "text";
            }
            // Intentionally no break here
        case PASSWORD:
            // If we didn't set it above, set it now
            if (inputType == null)
            {
                inputType = "password";
            }
        case DATE:
            if (inputType == null)
            {
                inputType = "datetime";
            }
        case INTEGER:
        case FLOAT:
            if (inputType == null)
            {
                inputType = "number";
            }
        case BOOLEAN:
            if (inputType == null)
            {
                inputType = "checkbox";
            }
            checked = "";
            if (Boolean.parseBoolean(valueStr))
            {
                checked = ", checked";
            }
            controlValue = GlobusHtmlUtil.tag("input", "type=" + inputType
                    + "," + classIdNameVal + checked + placeholderAttr);

            break;
        case ENUM:
            break;
        case BLOB:
            controlValue = GlobusHtmlUtil.tag("textarea", "rows=5"
                    + placeholderAttr + classIdName, valueStr);
            break;
        case AUTO:
            // SHould not get here as it should be converted at a lower
            // level
            break;
        }
        return controlValue;
    }

    /**
     * @param configProp
     * @param value
     * @return
     */
    private String getValueForField(ConfigurableProperty configProp,
            Object value)
    {
        // If there's no value for this yet, we use the default
        if (value == null)
        {
            value = configProp.defaultValue;
        }
        if (value == null)
        {
            // We return as empty string if we don't have a value
            return "";
        }
        String valueStr;
        switch (configProp.dataType)
        {
        case PASSWORD:
            valueStr = value.toString().replaceAll(".", "."); // Replace every
                                                              // char with a .
            break;
        case BLOB:
            if (value instanceof byte[] || value instanceof Byte[])
            {
                byte[] bytes = (byte[]) value;
                // For JDK 1.6
                valueStr = DatatypeConverter.printBase64Binary(bytes);
                // For JDK 1.8 a somewhat cleaner looking version
                // byte[] encoded = Base64.getEncoder().encode(bytes);
                // valueStr = new String(encoded);
            }
            else
            {
                valueStr = value.toString();
            }
            break;
        default:
            valueStr = value.toString();
        }

        return valueStr;
    }

    private String getControlName(Configurable config, String propId)
    {
        // Remove spaces in the name as it messes up code based on the id name
        return tagIdForName(config.getName() + "-" + propId);
    }

    @Override
    public String getConfigValueFromValueRep(Object valueRepresentation,
            Configurable config, String propName)
    {
        ConfigurableProperty prop = null;
        if (valueRepresentation instanceof HttpServletRequest
                && propName != null && config != null && (prop = config.getProperty(propName)) != null)
        {
            HttpServletRequest request = (HttpServletRequest) valueRepresentation;
            String controlName = getControlName(config, propName);
            String parameter = request.getParameter(controlName);
            // Values in select options are encoded as tag values, reverse that here if need be
            parameter = nameForTagId(parameter);
            // If we have a PASSWORD field, check for an all "." value
            // generated in the renderer
            // and assume that we don't really want to change it
            if (prop.dataType == DataType.PASSWORD)
            {
                if (parameter != null
                        && parameter.replace(".", "").length() == 0)
                {
                    parameter = null;
                }
            }
            else if (prop.dataType == DataType.BOOLEAN)
            {
                if (parameter == null)
                {
                    parameter = "false";
                }
            }
            if ("".equals(parameter))
            {
                parameter = null;
            }
            return parameter;
        } else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.globus.configuration.Renderer#renderTo(java.lang.Object,
     * org.dspace.globus.configuration.Configurable)
     */
    @Override
    public Set<String> saveConfig(Object valueRepresentation, Configurable config, String configGroupProperty,
            Context context, DSpaceObject dso)
    {
        Set<String> missingConfigs = new HashSet<String>();
        Iterator<String> propIds = config.getPropIds();
        String groupName = null;
        if (configGroupProperty != null) {
            groupName = getConfigValueFromValueRep(valueRepresentation, config, configGroupProperty);
        }
        while (propIds.hasNext())
        {
            String propId = propIds.next();
            String parameter = getConfigValueFromValueRep(valueRepresentation,
                    config, propId);
            ConfigurableProperty configProperty = config.getProperty(propId);
            if (parameter != null)
            {
                GlobusConfigurationManager.setProperty(context, propId,
                        groupName, dso, parameter);
            } else if (configProperty != null && configProperty.required) {
                missingConfigs.add(configProperty.displayName);
            }
        }

        return missingConfigs;
    }
}
