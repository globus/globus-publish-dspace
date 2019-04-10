Globus Data Publication Open Source
===================================

This repository contains the source code for the [Globus Data Publication](https://www.globus.org/data-publication) service. Globus Data Publication enables sharing, description, discovery and long-term identification of datasets with public or controlled access, and is operated as a [service](https://publish.globus.org) by the Globus team. 

This source code is provided as is without support as a demonstration of using [Globus platform](https://www.globus.org/platform) capabilities with new or existing service implementations. In particular, Globus Data Publication augments [DSpace](http://dspace.org) with Globus platform services for:

* User identity and login using [Globus Auth](https://docs.globus.org/api/auth).
* Role based authorization using [Globus groups](https://www.globus.org/groups)
* High-performance [data transfer](https://www.globus.org/file-transfer) operations for importing data into and exporting data out of the Data Publication service.

In addition to changes to support integration with the Globus platform services, this code release contains further changes to the DSpace source to facilitate operation as a service such as updates to some administrative interfaces to allow for self-service management of Communities and Collections, styling and appearance to match other Globus services, and use of additional persistent identifier services for creating citeable references to published datasets among others. These updates are present in this release, but may not be applicable in other environments.

The code base contains files that are modified versions of original [DSpace source files](https://github.com/DSpace/DSpace) and other files which are original to this release. The modified DSpace source files are based on DSpace version 4.9. It is not expected that this code will work with other versions of the DSpace codebase, particularly major versions later than version 4. The new and modified source files are maintained under separate directory trees than the original DSpace source code. Where files are present in this release, the original DSpace files should be replaced. This can be achieved automatically using scripts, and is described below in the Installation Instructions.



Building and  Installation Instructions
---------------------------------------

### Pre-requisite software

The following must be installed to develop or run this software.

* [Java SDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) version 1.8 or greater.
* [Maven Build tool](http://maven.apache.org/download.html)
* [Ant build tool](http://ant.apache.org/bindownload.cgi)
* [Tomcat J2EE/Servlet Engine](http://tomcat.apache.org/download-80.cgi)
* [Postgres database](http://www.postgresql.org/download/)
  - Postgres need not be installed locally, but you need access to a Postgres server and have the ability to administer the database for example to create users and tables. You will need to edit the file build.properties in the dspace source directory to reflect the location and permissions for your database.

These can be installed using appropriate package management tools or using the links provided.

### Acquiring and configuring the source code

* Download the standard [DSpace 4.9 source distribution](https://github.com/DSpace/DSpace/tree/dspace-4.9) from github to your local filesystem. We refer to the root of the unpacked source distribution as `[dspace]`.

* Download this [source code](https://github.com/globus/globus-publish-dspace) to a separate directory. We refer to the root of this source repo as `[globus_publish]`.

* As a convenience, many basic build and installation steps outlined below can be bypassed by running the script found in `[globus_publish]/bin/dspace-build.sh` providing the path to `[dspace]` as the first argument (as in `[globus_publish]/bin/dspace-build.sh [dspace]`). Steps below marked with (Scripted) will be performed automatically by this script.

### Configuration Steps

A variety of configuration files are present under `[globus-publish]/src/dspace/config`. Most notably are the files `modules/globus.cfg` and `modules/globus-auth.cfg`. These files are templated and use a "handlebars" style notation for various values that need to be set in deployment. Current practice is to edit these files in place in a deployment prior to starting the Globus Data Publication service.

Some specific edits to be aware of:

* Edit the file `[globus_publish]/src/dspace/config/modules/globus-auth.cfg` to define the superuser Globus user for this instance. This superuser must be a `globusid.org` identity, and the configured username must be the entire identity name including `@globusid.org`.
* (Scripted) Copy `[globus_publish]/src/dspace/config/registries/globus-metadata.xml` to `[dspace]/dspace/config/registries/globus-metadata.xml`
	- New metadata definitions can be created by creating new XML files in the directory `[globus_publish]/src/dspace/config/registries`. Inclusion of this metadata must be enabled by editing the file `build.xml`. View this file to see examples of including other metadata definitions.
* (Scripted) Copy `[globus_publish]/src/dspace/config/item-submission.xml` to `[dspace]/dspace/config/item-submission.xml`. This file determines the workflow steps show to a user during the submission process, and includes use of Globus to transfer data into the publication repository.
* Copy [globus_publish]/src/dspace/config/email/*` to `[dspace]/dspace/config/email. In new environments, the email templates in this directory should be updated to reflect appropriate messages and contact details for this deployment.

### Build

* As outlined in the DSpace documentation, edit `[dspace]/build.properties` appropriately.
* (Scripted) Copy the build file from `[globus_publish]/src/dspace/src/globus/build.xml` to `[dspace]/dspace/src/main/config/build.xml`
* (Scripted) In the directory `[globus_publish]/src/globus-client-java` run `mvn install -DskipTests`.

* For each of the projects `dspace-api` and `dspace-jspui` in `[dspace]`:

  - (Scripted) Replace the file `pom.xml` with the file from `[globus_publish]/src/dspace-xxx` source repo. Symlinks are fine. For example:
    + `mv [dspace]/dspace-xxx/pom.xml [dspace]/dspace-xxx/pom.xml.orig`
	+ `ln -s [globus_publish]/src/dspace-xxx/pom.xml [dspace]/dspace-xxx`

  - (Scripted) Copy the directory hierarchy from `[globus_publish]/src/dspace-xxx/src/globus` to `[dspace]/dspace-xxx/src/globus`. For example:
    + `ln -s [globus_publish]/src/dspace-xxx/src/globus [dspace]/dspace-xxx/src`
    + The DSpace modules of particular importance here are `dspace-api` and `dspace-jspui`.

  - (Scripted) Run `[globus_publish]/bin/dupFileRenamer` to rename the duplicated files in the `dspace-xxx` source tree. For example:
    + `[globus_publish]/bin/dupFileRenamer [dspace]/dspace-xxx/src/globus [dspace]/dspace-xxx/src/main`.

*  For a fresh install copy and run the ant target to update the database. These operations are not scripted as they typically only need to be run once on initial deployment.
    + `[globus_publish]/dspace/etc/postgres/globus_database_schema.sql [dspace]/dspace/etc/postgres/globus_database_schema.sql`
    + `ant update_globus_database`

* (Scripted) Use the standard dspace build/installation mechanism (`mvn package` from `[dspace]` and `ant fresh_install` from `[dspace]/dspace/target/dspace-build`)
	- On subsequent builds, using `ant update update_configs` can be performed rather than `ant fresh_install`
		+ Scripting note: the `dspace-build.sh` uses the update and update\_config flags. On first install, these will fail so a manual run of `ant fresh_install` will be required rather then relying on the script.

### Deploy

* Install the webapps created in `dspace.install.dir` as defined in `build.properties` to the Tomcat webapps directory.
	- For example `ln -s [dspace.install.dir]/webapps/* $CATALINA_HOME/webapps`

* Edit the configuration files in `[dpsace.install.dir]/config` including `dspace.cfg`, `modules/globus.cfg` and `modules/globus-auth.cfg` to reflect appropriate values for your environment.

* (Scripted) Re-start Tomcat so that the changes are picked up.

## Support
This work was partially supported under financial assistance award 70NANB14H012 from U.S. Department of Commerce, National Institute of Standards and Technology as part of the Center for Hierarchical Material Design (CHiMaD).
