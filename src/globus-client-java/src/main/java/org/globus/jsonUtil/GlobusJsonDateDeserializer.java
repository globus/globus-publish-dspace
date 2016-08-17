/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Aug 21, 2014 by pruyne
 */

package org.globus.jsonUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

/**
 * @author pruyne
 *
 */
public class GlobusJsonDateDeserializer extends JsonDeserializer<Date>
{
    // input is in the form 2014-08-21 07:15:53+00:00
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ssz");

    /*
     * (non-Javadoc)
     *
     * @see org.codehaus.jackson.map.JsonDeserializer#deserialize(org.codehaus.jackson.JsonParser,
     * org.codehaus.jackson.map.DeserializationContext)
     */
    @Override
    public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
        JsonProcessingException
    {

        String dateText = jp.getText();
        int lastColon = dateText.lastIndexOf(':');
        String fixedDateText = dateText.substring(0, lastColon);
        fixedDateText = fixedDateText + dateText.substring(lastColon + 1);
        Date parsed = null;
        try {
            parsed = format.parse(fixedDateText);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return parsed;
    }

}
