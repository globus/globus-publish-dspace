/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Aug 21, 2014 by pruyne
 */

package org.globus.jsonUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.ContextualDeserializer;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * @author pruyne
 *
 */
public class EnumDeserializer extends JsonDeserializer<Enum<?>>
    implements ContextualDeserializer<Enum<?>>
{
    private Class<? extends Enum<?>> wrappedType;

    @Override
    public JsonDeserializer<Enum<?>> createContextual(DeserializationConfig config,
                                                      BeanProperty property)
                                                          throws JsonMappingException
    {
        wrappedType = (Class<? extends Enum<?>>) property.getType().getRawClass();
        return this;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.codehaus.jackson.map.JsonDeserializer#deserialize(org.codehaus.jackson.JsonParser,
     * org.codehaus.jackson.map.DeserializationContext)
     */
    @Override
    public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,        
        JsonProcessingException
    {
        String enumVal = jp.getText();
        Enum<?> val = null;
        Method[] methods = wrappedType.getMethods();
        for (Method method : methods) {
            try {
                if (method.getReturnType().equals(wrappedType) && 
                                Modifier.isStatic(method.getModifiers())) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length == 1 && paramTypes[0].equals(String.class)) {
                        Object value = method.invoke(null, enumVal);
                        if (value != null) {
                            return (Enum<?>) value;
                        }                        
                    }
                }
            } catch (Exception e) {
                // Do nothing here as we'll loop around and try the next method we find
            }
        }
        return null;
    }

}
