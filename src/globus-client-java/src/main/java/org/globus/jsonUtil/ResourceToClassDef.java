/*
 * Copyright 2014 University of Chicago
 * All Rights Reserved.
 */

package org.globus.jsonUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @author pruyne
 *
 */
public class ResourceToClassDef
{

    Client restClient;


    public ResourceToClassDef()
    {
    }


    /**
     * @param file
     * @throws IOException
     */
    public ResourceToClassDef(File accessTokenFile) throws IOException
    {
        restClient = ClientCreation.createRestClient(accessTokenFile);
    }


    public String propertyDefs(String pathToResource)
    {
        StringBuffer propDefs = new StringBuffer();
        WebResource resource = restClient.resource(pathToResource);
        ClientResponse response =
            resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        String className = pathToResource.substring(pathToResource.lastIndexOf('/') + 1);
        className = underscoreToCamelCase(className);

        // resource.method(method.name(), ClientResponse.class, requestEntity);
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            Object responseObj = response.getEntity(Object.class);
            if (responseObj instanceof List) {
                List respList = (List) responseObj;
                if (!respList.isEmpty()) {
                    responseObj = respList.get(0);
                }
            }
            if (responseObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) responseObj;
                appendPropsToDef(propDefs, className, map, 0);
            } else {
                System.err.println("Got unexpected return from HTTP call of type: "
                    + responseObj.getClass());
            }
        } else {
            System.err.println("Got failure response: " + response);
            String failResp = response.getEntity(String.class);
            System.err.println(failResp);
        }

        return propDefs.toString();
    }

    private void appendPropsToDef(StringBuffer buf, String className, JSONObject jsonObj,
                                  int indentLevel)
    {
        Map<String, Object> toDef = jsonObjToMap(jsonObj);
        appendPropsToDef(buf, className, toDef, indentLevel);
    }


    /**
     * @param jsonObj
     * @return
     */
    private Map<String, Object> jsonObjToMap(JSONObject jsonObj)
    {
        Map<String, Object> toDef = new HashMap<>();
        Iterator<String> keys = jsonObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object val = jsonObj.get(key);
                toDef.put(key, val);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return toDef;
    }
    

    @SuppressWarnings("unchecked")
    private void appendPropsToDef(StringBuffer buf, String className, Map<String, Object> props,
                                  int indentLevel)
    {
        indentBuf(indentLevel, buf);
        // If this is an inner class (indentLevel > 0), we must declare it static, else we consider
        // it public
        buf.append(String
            .format("%s class %s\n", indentLevel == 0 ? "public" : "static", className));
        indentBuf(indentLevel, buf);
        buf.append("{\n");
        Set<Entry<String, Object>> propEntries = props.entrySet();
        for (Entry<String, Object> propEntry : propEntries) {
            String propName = propEntry.getKey();
            Object propVal = propEntry.getValue();
            String javaPropName = underscoreToCamelCase(propName);
            String javaClassName = null;
            boolean isArray = false;
            if (propVal instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) propVal;
                List<Object> l = new ArrayList<Object>();
                if (jsonArray.length() > 0) {
                    try {
                        l.add(jsonArray.get(0));
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                propVal = l;
            }
            if (propVal instanceof List) {
                isArray = true;
                List asList = (List) propVal;
                if (asList.isEmpty()) {
                    propVal = null;
                } else {
                    propVal = asList.get(0);
                }
            }
            if (propVal instanceof JSONObject) {
                propVal = jsonObjToMap((JSONObject) propVal);
            }
            if (propVal instanceof Map) {
                javaClassName = "ClassFor" + javaPropName;
                appendPropsToDef(buf, javaClassName, (Map<String, Object>) propVal, indentLevel + 1);
            } else if (propVal != null) {
                Class<? extends Object> propType = propVal.getClass();
                javaClassName = propType.getSimpleName();
            } else {
                javaClassName = "Object";
            }
            indentBuf(indentLevel + 1, buf);
            buf.append(String.format("@JsonProperty(\"%s\")\n", propName));
            indentBuf(indentLevel + 1, buf);
            buf.append(String.format("public %s%s %s;\n\n", javaClassName, isArray ? "[]" : "",
                                     javaPropName));
        }
        indentBuf(indentLevel, buf);
        buf.append("}\n");

    }


    /**
     * @param indentLevel
     */
    private void indentBuf(int indentLevel, StringBuffer buf)
    {
        for (int i = 0; i < indentLevel; i++) {
            buf.append("    ");
        }
    }


    /**
     * @param propName
     * @return
     */
    private String underscoreToCamelCase(String propName)
    {
        StringBuilder javaPropName = new StringBuilder();
        int underscoreLoc = -1;
        while ((underscoreLoc = propName.indexOf('_')) > 0) {
            String prefix = propName.substring(0, underscoreLoc).toLowerCase();
            char nextWordChar = propName.charAt(underscoreLoc + 1);
            nextWordChar = Character.toUpperCase(nextWordChar);
            propName = propName.substring(underscoreLoc + 2);
            javaPropName.append(prefix);
            javaPropName.append(nextWordChar);
        }
        javaPropName.append(propName.toLowerCase());
        return javaPropName.toString();
    }


    public static void main(String[] args) throws IOException
    {
        ResourceToClassDef deffer = null;

        boolean firstTime = true;
        boolean didDef = false;
        for (String url : args) {
            // First time, we assume it is the file name with the access token
            if (firstTime) {
                deffer = new ResourceToClassDef(new File(url));
                firstTime = false;
            } else {
                System.out.println(url + ":\n\n");
                String propDefs = deffer.propertyDefs(url);
                System.out.println(propDefs);
                System.out.println("\n\n");
                didDef = true;
            }
        }

        // No arguments, so read JSON from stdin
        if (!didDef) {
            InputStream is = null;
            String name = null;
            if (firstTime) {
                is = System.in;
                name = "FromStdIn";
            } else {
                is = new FileInputStream(args[0]);
                name = args[0];
            }

            try {
                String json = streamToString(is);
                JSONObject jsonObj = new JSONObject(json);
                deffer = new ResourceToClassDef();
                StringBuffer buf= new StringBuffer();
                deffer.appendPropsToDef(buf, name, jsonObj, 0);
                System.out.println(buf.toString());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    /**
     * @param is
     * @return
     * @throws IOException 
     */
    private static String streamToString(InputStream is) throws IOException
    {
        StringBuffer buf = new StringBuffer();
        byte[] bytes = new byte[1024];
        int count;
        while ((count = is.read(bytes)) > 0) {
            String string = new String(bytes, 0, count, Charset.forName("UTF-8"));
            buf.append(string);
        }
        return buf.toString();
    }
}
