/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Dec 24, 2015 by pruyne
 */

package org.globus;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * @author pruyne
 *
 */
public class GlobusEntity
{
    protected GlobusRestInterface intfImpl;


    /**
     * @return the intfImpl
     */
    @JsonIgnore // Or else it tries to serialize it
    public GlobusRestInterface getIntfImpl()
    {
        return intfImpl;
    }


    /**
     * @param intfImpl the intfImpl to set
     */
    public void setIntfImpl(GlobusRestInterface intfImpl)
    {
        this.intfImpl = intfImpl;
    }

    /**
     * @param string
     * @param attrMap
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    protected void setChildren(String rootPath, Map<String, Object> attrMap)
        throws IllegalArgumentException, IllegalAccessException
    {
        Field[] fields = this.getClass().getFields();
        for (Field field : fields) {
            JsonChildPath jsonChild = field.getAnnotation(JsonChildPath.class);
            if (jsonChild != null) {
                String childPath = jsonChild.value();
                if (childPath != null) {
                    // We're just going to split in two, then check if the root part is the same
                    // as the passed in root
                    String[] pathParts = childPath.split("/", 2);                    
                    if (pathParts != null && pathParts.length == 2 && pathParts[0].equals(rootPath)) {
                        Object childObj = attrMap.get(pathParts[1]);
                        Class<?> fieldType = field.getType();
                        if (childObj != null && fieldType.isAssignableFrom(childObj.getClass())) {
                            field.set(this, childObj);
                        } else if (childObj instanceof Map) {
                            // TODO: If the value is a Map, that means we have a further embedded
                            // obj and we should walk down to allow deeper nesting. This prob.
                            // means this whole thing needs to be in an outter loop of some sort.
                            // Doesn't seem like recursing will do the job as we'll start
                            // iterating over all the fields again.
                        }
                    }
                }
            }
        }
    }

    /**
     * @param rootPath
     * @param jsonNode
     */
    protected void setChildren(String rootPath, JsonNode rootNode)
    {
        Field[] fields = this.getClass().getFields();
        for (Field field : fields) {
            JsonChildPath jsonChild = field.getAnnotation(JsonChildPath.class);
            if (jsonChild != null) {
                String childPath = jsonChild.value();
                String[] parts = childPath.split("[/\\.]");
                if (parts == null || !parts[0].equals(rootPath)) {
                    continue;
                }
                JsonNode jsonNode = rootNode;
                for (int i = 1; i < parts.length && jsonNode != null; i++) {
                    jsonNode = jsonNode.get(parts[i]);
                }

                if (jsonNode != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        Class<?> fieldType = field.getType();
                        Object fieldVal = null;
                        if (fieldType.isArray()) {
                            JsonNode jsonArrayNode = null;
                            String firstFieldName = null;
                            if (jsonNode instanceof ArrayNode) {
                                jsonArrayNode = jsonNode;
                                firstFieldName = rootPath;
                            } else if (jsonNode instanceof ObjectNode) {
                                firstFieldName = jsonNode.getFieldNames().next();
                                jsonArrayNode = jsonNode.get(firstFieldName);                                
                            }
                            if (jsonArrayNode == null) {
                                continue; // Couldn't find the array in the json, so bail on this field
                            }
                            int numElements = jsonArrayNode.size();
                            Class<?> componentType = fieldType.getComponentType();
                            fieldVal = Array.newInstance(componentType, numElements);
                            for (int i = 0; i < numElements; i++) {
                                JsonNode valNode = jsonArrayNode.get(i);
                                Object newInstance = componentType.newInstance();
                                if (newInstance instanceof GlobusEntity) {
                                    GlobusEntity ge = (GlobusEntity) newInstance;
                                    ge.setChildren(firstFieldName, valNode);
                                } else {
                                    newInstance = mapper.readValue(valNode, componentType);
                                }
                                Array.set(fieldVal, i, newInstance);
                            }
                        } else {
                            fieldVal = mapper.readValue(jsonNode, fieldType);                            
                        }
                        if (fieldVal != null) {
                            field.set(this, fieldVal);
                        }
                    } catch (JsonParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (JsonMappingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static ObjectMapper mapper = null;


    public synchronized static <T> T fromJson(String json, Class<T> cls) throws IOException
    {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }

        T obj = mapper.readValue(json, cls);
        return obj;
    }
    
    public synchronized String toJson() throws JsonGenerationException, JsonMappingException, IOException
    {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.configure(Feature.WRITE_NULL_MAP_VALUES, false);
        }
        
        return mapper.writeValueAsString(this);
    }
    
    public String toString()
    {
        try {
            return toJson();
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to create Json for: " + super.toString() + " due to " + e;
        }
    }
    
    protected void copy(GlobusEntity otherEntity, boolean overWriteCurrentValues)
    {
        Field[] fields = getClass().getFields();
        for (Field field : fields) {
            JsonProperty anno = field.getAnnotation(JsonProperty.class);
            if (anno != null) {
                try {
                    if (overWriteCurrentValues || field.get(this) == null) {
                        Object fieldVal = field.get(otherEntity);
                        if (fieldVal != null) {
                            field.set(this, fieldVal);
                        }
                    }
                } catch (Exception e) {
                    // We're going to quietly ignore this and just copy whatever fields we can
                }
            }
        }
    }    
}
