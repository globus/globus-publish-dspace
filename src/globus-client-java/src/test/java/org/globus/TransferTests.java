/**
 * Copyright 2014 University of Chicago
 * All rights reserved.
 * Created Jan 2, 2015 by pruyne
 */

package org.globus;

import org.globus.transfer.TransferInterface;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author pruyne
 *
 */
@RunWith(Categories.class)
@IncludeCategory(TransferInterface.class)
@SuiteClasses({ GlobusClientTest.class })
public class TransferTests
{

}
