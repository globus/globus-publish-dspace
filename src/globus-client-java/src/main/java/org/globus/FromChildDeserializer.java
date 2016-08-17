/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Dec 30, 2015 by pruyne
 */

package org.globus;

import java.io.IOException;
import java.util.Iterator;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.ContextualDeserializer;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.JavaType;

/**
 * @author pruyne
 * Code from:
 * http://stackoverflow.com/questions/11747370/jackson-how-to-process-deserialize-nested-json
 */
public class FromChildDeserializer extends JsonDeserializer<Object> implements
    ContextualDeserializer<Object>
{
    private Class<?> wrappedType;
    private String wrapperKey;

    @Override
    public JsonDeserializer<Object> createContextual(DeserializationConfig config, 
                                                     BeanProperty property)
        throws JsonMappingException
    {
        JsonChildPath skipWrapperObject = property
                            .getAnnotation(JsonChildPath.class);
        if (skipWrapperObject != null) {
            wrapperKey = skipWrapperObject.value();
        } else {
            wrapperKey = null;
        }
        JavaType collectionType = property.getType();
        JavaType collectedType = collectionType.containedType(0);
        if (collectedType != null) {
            wrappedType = collectedType.getRawClass();
        } else {
            wrappedType = collectionType.getRawClass();
        }
        return this;
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(parser);
        JsonNode wrapped = null;
        if (jsonNode instanceof ObjectNode) {
            ObjectNode objNode = (ObjectNode) jsonNode;
            String childKey = null;
            if (wrapperKey != null) {
                childKey = wrapperKey;
            } else {
                Iterator<String> fieldNames = objNode.getFieldNames();
                childKey = fieldNames.next();
            }
            if (childKey != null) {
                wrapped = objNode.get(childKey);
            }
        } else {
            // There is no child object node, so we just use the parent
            wrapped = jsonNode;
        }
        if (wrapped != null) {
            parser = wrapped.traverse();
            Object mapped = mapper.readValue(parser, wrappedType);
            return mapped;
        } else {
            return null;
        }
    }
}
