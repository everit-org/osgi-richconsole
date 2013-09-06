osgi-richconsole
================


Introduction
------------

The aim of the richconsole project is to speed up development and testing
iterations. It opens up an "always-on-top" small panel that has some nice
features like deploying bundles just by dropping jar files or the main
folder of maven based projects onto it.

Richconsole is as simple as possible. It does not have any special
dependency. However, it can be extended with extra functionality by
installing add-on bundles. This means that you can use it in any
OSGi container that supports OSGi spec 4.3.

There are tools and solutions that give you the same or better features,
however, they often need a servlet container, a TCP connection or they
are wired to an IDE. With richconsole you will not need them.


When does the window appear?
----------------------------

When the richconsole bundle starts it checks if the environment has a
graphical interface. To know that it checks if the OSGi container
was started in [headless][1] mode or not.

The richconsole will not appear if the OSGi container is started in the
integration-test phase of the compilation by [eosgi-maven-plugin][2].


How do I deploy a bundle?
-------------------------

Drag a jar file or a maven project and drop it onto the deployer window!
The jar file can be dragged from any tool that supports drag-and-drop
like your favourite file manager application or Eclipse navigation view.

In case you drop folder's onto the deployer window, the followings will
be checked?

 - Does the folder have a target/classes subfolder?
 - Is there a MANIFEST.MF in target/classes/META-INF folder?
 - Does the MANIFEST.MF contain Bundle-SymbolicName and Bundle-Version
   headers?

You may have found out that during the deployment of a maven project
it uses target/classes folder. In case you use Eclipse with m2e plugin
your target/classes will have always up-to-date state. Not only the
classes will be compiled as soon as you save their source files but also
MANIFEST.MF will be up-to-date. Other development environments may
support Maven and maven-bundle-plugin at this level but I have not tried
them.


How is a bundle deployed?
-------------------------

The expectation is that if you use richconsole you actively develop a
project. That means that if you deploy a bundle it will always have the
same version. Therefore if a bundle already exists with the same
Bundle-SymbolicName and same Bundle-Version it will be:

 - Updated if the location is the same as the place where you dragged
   the bundle from
 - Deleted and reinstalled if the bundle has a new location. This can
   happen if you started your OSGi console in normal way and the bundle
   comes from a lib folder but now you re-deploy it from the maven
   project

To deploy the bundles the context of the richconsole bundle is used.


How can I extend the functionality of richconsole?
--------------------------------------------------

When someone simply clicks on the deployer window a pop-up menu appears.
The be able to sit into the menu as an item someone has to implement the
org.everit.osgi.dev.richconsole.MenuItem interface and register it as
an OSGi service. That implementation of the service can catch the event
when the menu item is selected and do whatever it wants. For example, it
can open up a new panel.


Are there any extensions that I can use now?
--------------------------------------------

Extensions are under development. They normally support a piece of logic
that is useful. The following projects are under development:

 - __Bundle management panel:__ Opens up a panel where all bundles are
   listed and they can be removed or updated
 - __Test selection panel:__ This panel will be useful for programmers
   who write many OSGi tests with the help of [everit-osgi-testrunner][3].
   In this case they can switch re-run tests or switch them off so they
   will not started after a re-deployment. In case someone writes many
   tests in a bundle it can be annoying if all of them runs after a
   deployment.
 - For more extensions google for expressions like "everit osgi richconsole".
   If we find an extension written by others we will list them here. 


Where can I find the binaries?
------------------------------

You can find the binaries at [Everit public maven repository][4]

The project is compiled by maven. Every information about the project
is available at the [generated site][5].


What about the security?
------------------------

RichConsole is designed to be used during development. It means that
there is no security handling at all. Do not use it in production!


Where can I ask?
----------------

In case you found a bug or need a feature, create an issue in Github
issue tracker. If you have a question it might mean that the documentation
here is not clear so in this case you should create an issue, too.

In case you would like to let us know that you like the solution please
mark the project with a star and we will be happy. 


[1]: http://docs.oracle.com/javase/6/docs/api/java/awt/GraphicsEnvironment.html#isHeadless()
[2]: https://github.com/everit-org/eosgi-maven-plugin
[3]: https://github.com/everit-org/osgi-testrunner
[4]: https://repository.everit.biz/nexus/content/groups/public/org/everit/osgi/dev/org.everit.osgi.dev.richconsole/
[5]: http://everit.org/mvnsites/osgi-richconsole/