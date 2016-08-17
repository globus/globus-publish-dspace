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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 */
public interface Configurable
{
    public enum DataType {
        STRING(String.class), BLOB(byte[].class), PASSWORD(String.class), DATE(
                Date.class), INTEGER(Integer.class, Long.class, Short.class,
                Byte.class), FLOAT(Float.class, Double.class), ENUM(Enum.class), BOOLEAN(
                Boolean.class), AUTO(Object.class);

        private Class<?>[] javaTypes;

        public static DataType getDataTypeForClass(Class<?> inputClass)
        {
            DataType[] dataTypes = DataType.values();
            for (DataType dataType : dataTypes)
            {
                for (Class<?> javaType : dataType.javaTypes)
                {
                    if (javaType.equals(inputClass))
                    {
                        return dataType;
                    }
                }
            }
            return null;
        }

        DataType(Class<?>... javaType)
        {
            this.javaTypes = javaType;
        }
    }

    public abstract class ConfigurablePropertyValueOptions
    {
        /**
         * Indicate that a value should be considered a group seperator for layout purposes
         */
        // Using the mdash as the value as that's what we'll use in the control
        public static final String GROUP_SEPERATOR = "&mdash;";

        public abstract List<Object> getValueOptions();

        public String getDisplayValueForOption(Object option)
        {
            return (option != null) ? option.toString() : "null-option-value";
        }
        /**
         * @param allFiles
         */
        protected List<Object> sortOptionList(List<Object> allFiles)
        {
            Map<String, Object> mappedList = new TreeMap<String, Object>();

            for (Object fileName : allFiles)
            {
                String displayName = getDisplayValueForOption(fileName);
                mappedList.put(displayName, fileName);
            }
            List<Object> sortedList = new ArrayList<Object>(mappedList.size());
            for (Entry<String, Object> entry: mappedList.entrySet())
            {
                sortedList.add(entry.getValue());
            }
            return sortedList;
        }
    }

    public class ConfigurableProperty
    {
        public String id;

        public String displayName;

        public Object defaultValue;

        public DataType dataType;

        public ConfigurablePropertyValueOptions valueOptions;

        public String description;

        public boolean required;

        /**
         * Create a new ConfigurableProperty object from all the input
         * parameters
         *
         * @param id
         *            The id used when storing the configuration value in to the
         *            persistence system
         * @param displayName
         *            The name to use when displaying the configuration value to
         *            a user
         * @param defaultValue
         *            A default value which will be used any time no user-input
         *            value is present
         * @param dataType
         *            The {@link DataType} for the data. In some cases,
         *            conversions to/from this type and the corresponding Java
         *            type will be performed when saving/retrieving a property.
         * @param description
         *            A description used to provide more information to the
         *            user, typically in placeholder text in web controls.
         * @param required
         *            Whether this property is required to have a value for a
         *            configuration to be complete or if it may be ignored in
         *            some cases.
         */
        public ConfigurableProperty(String id, String displayName,
                Object defaultValue, DataType dataType, String description, boolean required)
        {
            this.id = id;
            this.displayName = displayName;
            this.defaultValue = defaultValue;
            this.dataType = dataType;
            this.description = description;
            this.required = required;
        }

        /**
         * @param id
         * @param displayName
         * @param defaultValue
         * @param dataType
         */
        public ConfigurableProperty(String id, String displayName,
                Object defaultValue, DataType dataType,
                ConfigurablePropertyValueOptions valueOptions)
        {
            this.id = id;
            this.displayName = displayName;
            this.defaultValue = defaultValue;
            this.dataType = dataType;
            this.valueOptions = valueOptions;
        }
    }

    public String getName();

    public Iterator<String> getPropIds();

    public ConfigurableProperty getProperty(String id);
}
