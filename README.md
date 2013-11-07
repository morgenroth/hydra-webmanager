Hydra WebManager
================

Hydra is a distributed emulation framework for large-scale software testing in disrupted networks. The WebManager is the central component to manage and run simulation sessions.

----------


#### Build The WebManager

To build the webmanager module you need to install maven2 first. Then
call the build process from your source directory.

```
mvn package
```

If that process finishes there will be a .war file in the `target` subfolder.
That file can deployed to a tomcat application server.


#### Workspace Setup

 - Copy the files in `<repository>/workspace` to the new **workspace** of Hydra.
 - Create a webserver directory e.g. `<hydra-workspace>/htdocs` and make it accessable via HTTP. The URL of this directory has to be set as web.dir in the config.properties files in the hydra **workspace**.
 - Create a directory for images download `<hydra-workspace>/htdocs/dl` and put OpenWrt image files there.


#### Database

Hydra used a MySQL database to store statistic data and session configurations. In order
to run the Hydra WebManager you need to set-up a dedicated database and inject the
`hydra.sql` file to generate all the necessary structures.

```
mysql -u hydra-username -p hydra-database < hydra.sql
```

Finally, it is necessary to change the credentials in the Hydra configuration file
`<hydra-workspace>config.properties`.

#### Configuration

Add the hydra **workspace** to the tomcat launch parameters. On Debian use the
default file for tomcat7 for that.

```
vim /etc/default/tomcat7
```

In this file add the **workspace** parameter below the declaration of `JAVA_OPTS`.
For the path /opt/hydrasim it may looks like this:

```
# hydra workspace
JAVA_OPTS="${JAVA_OPTS} -Dconfig.hydra=/opt/hydrasim"
```

Now restart the tomcat server and deploy the hydra WAR file.

