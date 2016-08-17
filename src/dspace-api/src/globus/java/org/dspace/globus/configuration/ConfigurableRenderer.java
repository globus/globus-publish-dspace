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

import java.util.Set;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 *
 */
public interface ConfigurableRenderer
{
    /**
     * Render in to the renderer's native format from the provided configuration
     *
     * @param config
     *            The configuration containing the values to be rendered
     * @param context
     *            The current context of the operation providing a means of
     *            accessing the {@link GlobusConfigurationManager}.
     * @param dso
     *            The {@link DSpaceObject} which is being configured.
     * @return A native representation of the configuration which is specific to
     *         this renderer
     */
    public Object renderFrom(Configurable config, Context context,
            DSpaceObject dso);

    /**
     * Save the state passed to the renderer for the given configurable in to
     * the {@link GlobusConfigurationManager}. This acts as an inverse of the
     * {@link ConfigurableRenderer#renderFrom(Configurable, Context, DSpaceObject)}
     * operation though it need not strictly use the same type of object input
     * here as returned by
     * {@link ConfigurableRenderer#renderFrom(Configurable, Context, DSpaceObject)}
     *
     *
     * @param valueRepresentation
     *            A representation of the values this renderer should be able to
     *            interpret and copy values in to the provided
     *            {@link Configurable}.
     * @param config
     *            The configurable to write values in to.
     * @param configGroupProperty
     *            An optional name of a property in the Configurable which
     *            stores the group name to be used when saving the
     *            configuration.
     * @param context
     *            The current context of the operation providing a means of
     *            accessing the {@link GlobusConfigurationManager}.
     * @param dso
     *            The {@link DSpaceObject} which is being configured.
     * @return Display names of configuration properties which are required, but not
     *         set. This allows errors to be displayed in the case that some
     *         configurable properties are flagged as required, but they are not
     *         set from this input value representation. Whether this is
     *         considered an error or not is dependent on the caller's
     *         requirements.
     */
    public Set<String> saveConfig(Object valueRepresentation, Configurable config,
            String configGroupProperty, Context context, DSpaceObject dso);

    /**
     * Get the value of a single, Configurable property from a representation of
     * values for the Configurable.
     *
     * @param valueRepresentation
     *            A representation of the values this renderer should be able to
     *            interpret and from which the relevant value will be returned.
     * @param config
     *            The {@link Configurable} defining the configuration definition
     *            to retrieve.
     * @param propName
     *            The name of a property in the config. to get the value for
     * @return The value of the property named as found in the
     *         {@code valueRepresentation}
     */
    public String getConfigValueFromValueRep(Object valueRepresentation,
            Configurable config, String propName);
}
