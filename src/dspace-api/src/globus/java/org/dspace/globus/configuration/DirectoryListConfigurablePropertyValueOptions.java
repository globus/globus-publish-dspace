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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 *
 */
public class DirectoryListConfigurablePropertyValueOptions extends
        Configurable.ConfigurablePropertyValueOptions
{
    private String dirName;

    private boolean removeExtension;

    private String removePrefix;

    private boolean prettyText;

    private boolean recurse;

    private boolean sort;

    private Collection<String> recurseDirs;

    /**
     *
     */
    public DirectoryListConfigurablePropertyValueOptions(String dirName,
            boolean removeExtension, String removePrevix, boolean prettyText,
            boolean recurse, boolean sort, Collection<String> recurseDirs)
    {
        this.dirName = dirName;
        this.removeExtension = removeExtension;
        this.removePrefix = removePrevix;
        this.prettyText = prettyText;
        this.recurse = recurse;
        this.sort = sort;
        this.recurseDirs = recurseDirs;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.globus.configuration.Configurable.ConfigurablePropertyValueOptions
     * #getValueOptions()
     */
    @Override
    public List<Object> getValueOptions()
    {
        return listDirectory(dirName, "");
    }

    private List<Object> listDirectory(String root, String path)
    {
        String dirName = root + path;
        File d = new File(dirName);
        List<Object> files = new ArrayList<Object>();
        if (d.isDirectory())
        {
            String[] fileNames = d.list();
            List<String> dirs = new ArrayList<String>();
            for (String fileName : fileNames)
            {
                String relativePath = path + File.separator + fileName;
                String fullFilePath = dirName + File.separator + fileName;
                File f = new File(fullFilePath);
                if (f.isDirectory())
                {
                    if (recurseDirs != null && recurseDirs.contains(f.getName().toLowerCase())) {
                        dirs.add(relativePath);
                    }
                }
                else
                {
                    files.add(relativePath);
                }
            }
            if (sort)
            {
                files = sortOptionList(files);
            }
            if (recurse)
            {
                for (String dirPath : dirs)
                {
                    List<Object> subDir = listDirectory(root, dirPath);
                    if (subDir != null && subDir.size() > 0)
                    {
                        files.add(super.GROUP_SEPERATOR);
                        files.addAll(subDir);
                    }
                }
            }
        }
        return files;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.dspace.globus.configuration.Configurable.ConfigurablePropertyValueOptions
     * #getDisplayValueForOption(java.lang.Object)
     */
    @Override
    public String getDisplayValueForOption(Object option)
    {
        if (option == null)
        {
            return null;
        }

        String rVal = super.getDisplayValueForOption(option);

        // Strip off any ending slash as it messes up the removing the last
        // slash below if present
        if (rVal.endsWith(File.separator))
        {
            rVal = rVal.substring(0, rVal.length() - 1);
        }

        // Now that we support recursing through a directory tree, we must
        // remove everything before
        // the last path sep. char before making it pretty

        int lastSlashIdx = rVal.lastIndexOf(File.separatorChar);
        if (lastSlashIdx >= 0)
        {
            rVal = rVal.substring(lastSlashIdx + 1);
        }

        if (removePrefix != null)
        {
            if (rVal.startsWith(removePrefix))
            {
                rVal = rVal.substring(removePrefix.length());
            }
        }

        if (removeExtension)
        {
            int lastDot = rVal.lastIndexOf('.');
            if (lastDot >= 0)
            {
                rVal = rVal.substring(0, lastDot);
            }
        }

        if (prettyText)
        {
            rVal = rVal.replace('_', ' ');
            rVal = rVal.replace('-', ' ');
            rVal = rVal.trim();
            // Could theoretically be down to a zero length string at this point
            if (rVal.length() > 0)
            {
                char firstChar = rVal.charAt(0);
                if (Character.isLowerCase(firstChar))
                {
                    char upperChar = Character.toUpperCase(firstChar);
                    rVal = String.valueOf(upperChar) + rVal.substring(1);
                }
            }
        }

        return rVal;
    }
}
