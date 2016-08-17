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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dspace.identifier.IdentifierProvider;

/**
 *
 */
public class ObjectConfigurable implements Configurable
{

    private Class<?> configClass;

    private Object configObject;

    /**
     *
     */
    public ObjectConfigurable(Class<?> clazz)
    {
        this.configClass = clazz;
    }

    /**
     *
     */
    public ObjectConfigurable(Object obj)
    {
        this.configObject = obj;
        this.configClass = obj.getClass();
    }

    public Class<?> getConfigClass()
    {
        return configClass;
    }

    public Object getConfigObject()
    {
        return configObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.globus.configuration.Configurable#getName()
     */
    @Override
    public String getName()
    {
        ConfigurableClass configAnno = configClass
                .getAnnotation(ConfigurableClass.class);
        if (configAnno == null)
        {
            return configClass.getCanonicalName();
        }
        return configAnno.name();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.globus.configuration.Configurable#getNames()
     */
    @Override
    public Iterator<String> getPropIds()
    {
        Field[] fields = configClass.getFields();
        List<String> ids = new ArrayList<String>();
        for (Field field : fields)
        {
            ConfigurableField fieldAnno = field
                    .getAnnotation(ConfigurableField.class);
            if (fieldAnno != null)
            {
                String id = fieldAnno.propId();
                if (id == null || id.length() == 0)
                {
                    id = field.getName();
                }
                ids.add(id);
            }
        }

        return ids.iterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.globus.configuration.Configurable#getProperty(java.lang.String
     * )
     */
    @Override
    public ConfigurableProperty getProperty(String id)
    {
        ConfigurableField anno = findFieldAnnoForId(id);
        ConfigurableProperty prop = null;
        if (anno != null)
        {
            prop = new ConfigurableProperty(id, anno.displayName(),
                    anno.defaultValue(), anno.type(), anno.description(), anno.required());
        }
        return prop;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.globus.configuration.Configurable#getValue(java.lang.String)
     */
    public Object getValue(String id)
    {
        if (configObject == null)
        {
            return null;
        }
        Field field = findFieldForId(id);
        if (field != null)
        {
            try
            {
                return field.get(configObject);
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * @param javaType
     * @param valString
     * @return
     */
    private Object getJavaObjForStringVal(Class<?> javaType, String valString)
    {
        // Quick short circuit in case we're really looking for a string
        if (java.lang.String.class.equals(javaType))
        {
            return valString;
        }
        if (byte[].class.equals(javaType))
        {
            // TODO: THis should really be a base64 decode
            return valString.getBytes();
        }
        try
        {
            Method method = javaType.getMethod("valueOf", String.class);
            return method.invoke(null, valString);
        }
        catch (Exception e)
        {
        }
        return null;
    }

    /**
     * @param field
     * @param anno
     * @return
     */
    private DataType dataTypeForField(Field field, ConfigurableField anno)
    {
        if (anno == null)
        {
            anno = field.getAnnotation(ConfigurableField.class);
            if (anno == null)
            {
                return null;
            }
        }
        DataType type = anno.type();
        if (type != DataType.AUTO)
        {
            return type;
        }
        // Else we scan through for one that matches the underlying java type
        Class<?> javaType = field.getType();
        DataType dataType = DataType.getDataTypeForClass(javaType);
        return dataType;
    }

    private ConfigurableField findFieldAnnoForId(String id)
    {
        Field field = findFieldForId(id);
        if (field != null)
        {
            return field.getAnnotation(ConfigurableField.class);
        }
        else
        {
            return null;
        }
    }

    private Field findFieldForId(String name)
    {
        if (name == null)
        {
            return null;
        }
        Field[] fields = configClass.getFields();
        Field foundField = null;
        for (Field field : fields)
        {
            ConfigurableField fieldAnno = field
                    .getAnnotation(ConfigurableField.class);
            if (fieldAnno != null)
            {
                String annoName = fieldAnno.propId();
                if (annoName.equals(name))
                {
                    foundField = field;
                    break;
                }
                else if ("".equals(annoName) && field.getName().equals(name))
                {
                    /*
                     * If there's no name given on the annotation and the input
                     * name is the same as the name of the field, this could be
                     * a match, but keep searching in case there's an explicit
                     * annotation with this name which will override the class
                     * declaraion name match
                     */
                    foundField = field;
                }
            }
        }
        return foundField;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.globus.configuration.Configurable#setValue(java.lang.String,
     * java.lang.Object)
     */
    public void setValue(String id, Object value)
    {
        if (configObject == null)
        {
            return;
        }
        Field field = findFieldForId(id);
        if (field != null)
        {
            Class<?> fieldType = field.getType();
            Class<? extends Object> valueClass = value.getClass();
            if (!fieldType.isAssignableFrom(valueClass))
            {
                try
                {
                    Method valueOfMethod = fieldType.getMethod("valueOf",
                            valueClass);
                    if (valueOfMethod != null)
                    {
                        value = valueOfMethod.invoke(null, value);
                    }
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return;
                }
            }
            try
            {
                field.set(configObject, value);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Test whether a particular object has the needed annotation to be managed as an {@link ObjectConfigurable}
     * @param configObj The candidate object for management by the {@link ObjectConfigurable} class.
     * @return {@code true} if the object's class is properly annotated, {@code false} otherwise.
     */
    public static boolean isAnnotated(Object configObj)
    {
        Class<? extends Object> configClass = configObj.getClass();
        ConfigurableClass configClassAnno = configClass.getAnnotation(ConfigurableClass.class);
        return configClassAnno != null;
    }
}
