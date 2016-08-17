package org.globus;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.annotate.JsonProperty;

import com.sun.jersey.api.client.ClientResponse;

public class GlobusClientException extends Exception
{
    @JsonProperty("code")
    public String code;
    @JsonProperty("resource")
    public String resource;
    @JsonProperty("request_url")
    public String requestUrl;
    @JsonProperty("detail")
    public String detail;
    @JsonProperty("status")
    public String status;
    @JsonProperty("id")
    public String id;
    
    String message;
    Exception otherException;
    private static final long serialVersionUID = 1L;


    public GlobusClientException()
    {
        super();
    }


    public GlobusClientException(String message)
    {
        super(message);
        this.message = message;
    }


    @SuppressWarnings("unchecked")
    private void setFromMap(Map<String, Object> vals) 
    {
        if (vals != null) {
            Set<String> keys = new HashSet<String>(vals.keySet());
            for (String fieldName : keys) {
                try {
                    Object val = vals.get(fieldName);
                    if (val != null) {
                        Field field = null;
                        try {
                            field = this.getClass().getField(fieldName);
                        } catch (Exception e) {

                        }
                        if (field != null && field.getType().isAssignableFrom(val.getClass())) {
                            field.set(this, val);
                            // Empty it out so it doesn't get double reported
                            vals.remove(fieldName);
                        } else if (val instanceof Map) {
                            Map<String, Object> map = (Map<String, Object>) val;
                            setFromMap(map);
                            if (map.isEmpty()) {
                                vals.remove(fieldName);
                            }
                        } else if (val instanceof Collection) {
                            @SuppressWarnings("unchecked")
                            Collection<Object> childVals = (Collection<Object>) val;
                            for (Iterator<Object> itr = childVals.iterator(); itr.hasNext();) {
                                Object obj = itr.next();
                                if (obj instanceof Map) {
                                    Map<String, Object> map = (Map<String, Object>) obj;
                                    setFromMap(map);
                                    if (map.isEmpty()) {
                                        vals.remove(fieldName);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {

                }
            }
        }      
    }

    
    /**
     * @param response
     */
    public GlobusClientException(ClientResponse response, String path)
    {
        Map<String, Object> responseFields = null;
        try {
            responseFields = response.getEntity(Map.class);
        } catch (Exception e) {
            // If that fails, we just do our best below
        }
        /*
        this.requestUrl = path;
        this.code = String.valueOf(response.getStatus());
        */
        this.message = "";
        // We're gonna look at all the values in the response body, where we can, we assign them
        // to our declared fields. Where not, we'll add them to the message at the end
        setFromMap(responseFields);
        String errorMsg =
            "Got unexpected HTTP response code: " + response.getStatus() + " on URL: "
                + path;
        this.message = errorMsg;
        MultivaluedMap<String, String> headers = response.getHeaders();
        List<String> errorHeaders = headers.get("X-Transfer-API-Error");
        if (errorHeaders != null) {
            for (String errorHdrMsg : errorHeaders) {
                errorMsg = errorMsg + " " + errorHdrMsg;
            }
        }
        this.message = errorMsg + this.message;
        if (responseFields != null && responseFields.size() > 0) {
            this.message = this.message + " " + responseFields.toString();
        }
    }

    /**
     * @return the serialversionuid
     */
    public static long getSerialversionuid()
    {
        return serialVersionUID;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "GlobusClientException [" + (message != null ? "message=" + message + ", " : "")
            + (code != null ? "code=" + code + ", " : "")
            + (resource != null ? "resource=" + resource + ", " : "")
            + (requestUrl != null ? "requestUrl=" + requestUrl + ", " : "")
            + (detail != null ? "detail=" + detail + ", " : "") + "status=" + status + ", "
            + (id != null ? "id=" + id : "") + "]";
    }


}
