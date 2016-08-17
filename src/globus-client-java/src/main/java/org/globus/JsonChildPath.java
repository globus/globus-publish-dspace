/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Dec 30, 2015 by pruyne
 */

package org.globus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.codehaus.jackson.annotate.JacksonAnnotation;

/**
 * @author pruyne
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonChildPath {
    String value();
}
