osgi-richconsole
================


Introduction
------------

The aim of the richconsole project is to speed up development and testing
iterations. It opens up an "always-on-top" small panel that has some nice
features like deploying bundles just by dropping jar files or the main
folder of maven based projects on it.

Richconsole is as simple as possible. It does not have any special
dependency. However, it can be extended with extra functionalities by
installing add-on bundles. This means that you can use it in any
OSGi container that supports OSGi spec 4.3.

There are tools and solutions that give you the same or better features,
however, they often need a servlet container, a TCP connection or they
are wired to an IDE. With richconsole these extras are not required.


When does the window appear?
----------------------------

When the richconsole bundle starts, it checks whether the OSGi container
was started in [headless][1] mode or not to find out if the environment 
has a graphical interface.

The richconsole will not appear if the OSGi container is started during
a compilation process. More specifically it will not appear if the
EOSGI_STOP_AFTER_TESTS environment variable is set to "true". E.g. the
integration-test goal of [eosgi-maven-plugin][2] uses this variable.


How do I deploy a bundle?
-------------------------

Drag a jar file or a maven project and drop it on the deployer window!
The jar file can be dragged from any tool that supports drag-and-drop
like your favorite file manager application or Eclipse navigation view.

In case you drop folders on the deployer window, the following will
be checked:

 - Does the folder have a target/classes subfolder?
 - Is there a MANIFEST.MF in the target/classes/META-INF folder?
 - Does the MANIFEST.MF contain Bundle-SymbolicName and Bundle-Version
   headers?

You may have found out that during the deployment of a maven project,
the target/classes folder is being used. In case you use Eclipse with m2e 
plugin, your target/classes will have an always up-to-date state. Not only 
the classes will be compiled as soon as you save their source files but also
MANIFEST.MF will alwazs be up-to-date. Other development environments may
support Maven and maven-bundle-plugin at this level but they have not been 
tested.


How is a bundle deployed?
-------------------------

The expectation is that if you use richconsole, you actively develop a
project. This means that if you deploy a bundle, it will always have the
same version. Therefore, if a bundle already exists with the same
Bundle-SymbolicName and same Bundle-Version, then it will be:

 - Updated if the location is the same as the place where you dragged
   the bundle from
 - Deleted and reinstalled if the bundle has a new location. This can
   happen if you started your OSGi console in the usual way and the bundle
   comes from a lib folder but you re-deploy it from the maven project.

For the deployment of the bundles, the context of the richconsole 
bundle is used.


How to write extensions?
------------------------

It is possible to add menu items to the context menu that appears after
simply clicking on the deployer window. An extension can use the
RichConsoleService OSGi service. For more information see the javadoc
of the interface.

What is the number on the deployer window?
------------------------------------------

After Richconsole bundle is activated a TCP server is started as well.
The server accepts a couple of commands that makes it possible for external
tools to deploy and uninstall bundles. E.g. The dist goal of
[eosgi-maven-plugin][2] uses the TCP server to upgrade the bundles without
the necessity of restarting the OSGi container. The number on the window is
the port number on which the TCP server listens.

If not specified otherwise, RichConsole binds the TCP server to a free port.
A port can be specified with the EOSGI_UPGRADE_SERVICE_PORT environment
variable.

The available commands are listed in the RichConsoleConstants class.


What about security?
--------------------

RichConsole is designed to be used during development. It means that
there is no security handling at all. Do not use it for production!


[1]: http://docs.oracle.com/javase/6/docs/api/java/awt/GraphicsEnvironment.html#isHeadless()
[2]: https://github.com/everit-org/eosgi-maven-plugin
[3]: https://github.com/everit-org/osgi-testrunner