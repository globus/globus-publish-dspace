/**
 * This file is a modified version of a DSpace file.
 * All modifications are subject to the following copyright and license.
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

/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Thumbnail;
import org.dspace.core.Context;

import java.sql.SQLException;

public class ItemService
{
    private static Logger log = Logger.getLogger(ItemService.class);

    public static Thumbnail getThumbnail(Context context, int itemId, boolean requireOriginal) throws SQLException
    {
        ItemDAO dao = ItemDAOFactory.getInstance(context);

        Bitstream thumbBitstream = null;
        Bitstream primaryBitstream = dao.getPrimaryBitstream(itemId, "ORIGINAL");
        if (primaryBitstream != null)
        {
            if (primaryBitstream.getFormat().getMIMEType().equals("text/html"))
            {
                return null;
            }

            thumbBitstream = dao.getNamedBitstream(itemId, "THUMBNAIL", primaryBitstream.getName() + ".jpg");
        }
        else
        {
            if (requireOriginal)
            {
                primaryBitstream = dao.getFirstBitstream(itemId, "ORIGINAL");
            }

            thumbBitstream   = dao.getFirstBitstream(itemId, "THUMBNAIL");
        }

        if (thumbBitstream != null)
        {
            return new Thumbnail(thumbBitstream, primaryBitstream);
        }

        return null;
    }

    public static DCValue[] getMetadataByMetadataString(Item item, String mdString)
    {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");

        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens())
        {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        DCValue[] values;
        if ("*".equals(qualifier))
        {
            values = item.getMetadata(schema, element, Item.ANY, Item.ANY);
        }
        else if ("".equals(qualifier))
        {
            values = item.getMetadata(schema, element, null, Item.ANY);
        }
        else
        {
            values = item.getMetadata(schema, element, qualifier, Item.ANY);
        }

        return values;
    }

    /**
     * Service method for knowing if this Item should be visible in the item list.
     * Items only show up in the "item list" if the user has READ permission
     * and if the Item isn't flagged as unlisted.
     * @param context
     * @param item
     * @return
     */
    public static boolean isItemListedForUser(Context context, Item item) {
        try {
            if (AuthorizeManager.isAdmin(context)) {
                return true;
            }

            if (AuthorizeManager.authorizeActionBoolean(context, item, org.dspace.core.Constants.READ)) {
                if(item.isDiscoverable()) {
                    return true;
                }
            }

            log.debug("item(" + item.getID() + ") " + item.getName() + " is unlisted.");
            return false;
        } catch (SQLException e) {
            log.error(e.getMessage());
            return false;
        }

    }
}
