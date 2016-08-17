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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class BaseConfigurable implements Configurable
{

    private String name;

    private Map<String, ConfigurableProperty> propMap;

    public BaseConfigurable(String name, ConfigurableProperty... props)
    {
        this.name = name;
        // Use a linked hash map so that we traverse in order later
        propMap = new LinkedHashMap<String, ConfigurableProperty>();
        for (ConfigurableProperty prop : props)
        {
            propMap.put(prop.id, prop);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.globus.configuration.Configurable#getName()
     */
    @Override
    public String getName()
    {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.dspace.globus.configuration.Configurable#getPropIds()
     */
    @Override
    public Iterator<String> getPropIds()
    {
        return propMap.keySet().iterator();

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
        return propMap.get(id);
    }
}
