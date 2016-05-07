# MagicCarpet

The Magic Carpet is a fun game played in a field with a group of guys who aren't fully aware of their own mortality, the basic idea is that you tow a piece of carpet around a field behind a tractor, on the carpet are a group of people who then fight to see who is the last man standing. Shockingly there have been no recorded instances of major injuries from this activity, however it was retired quite a few years ago now, but the legend lives on.

## Purpose

This library is intended as a auto updating tool for the application environment which will be triggered when the application is started, though the details of how it is triggered are left as an exercise for the developer, though suggestions will be given within the README.

## Configuration

An XML document is used to define the updates that will be run, at the present moment it is only used for performing changes to a database such as adding or updating tables and columns. Each change set needs to be given a change ID which is a numerical value and will allow for versioning of the form <Major>.<Minor>.<Patch>... and will work for any number of given revision numbers, where the first number is classed as the most significant value reducing in significance as it moves to the right.

Each change ID needs to be unique, they do not need to be sequential, however they will be ordered depending on the value given.

If two changes are found with the same ID, then the application will give an error and will not perform any updates from the list it is given.

The application also has a devMode flag, this was put in as an easy way to disable the function for when you don't want it to make any changes, such as while you are still working on a change and so do not want it to apply half the changes you have created and then never do any more when it is run again. This value can be passed in to the setup of the objects and so can be obtained from a properties file or something similar.

## Database Access

The database connection used will need to have write access to the database given, it will also create a new table to track which changes it has applied to, currently this will be written to the same schema as the connection.

## Usage

Using this library is at your own risk, we are not responsible for any loss of data or database corruption caused by using this code for your own projects, remember to carefully test all changes before deploying the application anywhere but dev environments and always back up your database.

One way to set this up within a web application is to create a custom ServletContextListener implementation and then register that in the web.xml ahead of any spring listeners you might have:

```
public class MagicCarpetContextListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagicCarpetContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            InputStream in = getClass().getClassLoader().getResourceAsStream("farmhand.properties");
            Properties properties = new Properties();
            properties.load(in);
            String jdbcName = properties.getProperty("uk.co.agware.datasource.name");
            if(jdbcName == null || "".equals(jdbcName)){
                LOGGER.error("Unable to find property uk.co.agware.datasource.name");
                return;
            }
            String devModeString = properties.getProperty("uk.co.agware.devMode");
            boolean devMode = true;
            if(devModeString == null || "".equals(devModeString)){
                LOGGER.warn("Unable to find devMode property, setting to true for safety");
            }
            else {
                devMode = Boolean.parseBoolean(devModeString);
            }
            DataSource dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/" +jdbcName);
            DatabaseConnector databaseConnector = new DefaultDatabaseConnector();
            databaseConnector.setConnection(dataSource.getConnection());
            MagicCarpet magicCarpet = new MagicCarpet(databaseConnector, devMode);
            magicCarpet.run();
        } catch (NamingException | SQLException | IOException | MagicCarpetException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
```

Then in web.xml add the following:

```
<listener>
    <listener-class>path.to.MagicCarpetContextListener</listener-class>
</listener>
```

You can also do more manual processes if you want to double check that the changes have been loaded up correctly, the ``MagicCarpet.run()`` method runs the ``MagicCarpet.parseChanges();`` and then ``MagicCarpet.executeChanges();`` methods for you, however if you wish you can run ``MagicCarpet.parseChanges();`` and then check the number of changes that have been loaded and if you wish even manually execute a single change or a list of changes using the ``DatabaseConnector.executeChanges(List<Change>)`` or ``DatabaseConnector.executeChange(Change)`` methods. 

## ChangeSet.xml

By default, MagicCarpet will search the classpath for a ChangeSet.xml file, however it is possible to override this with a file from a different location if you choose:

Path object that points to the file:
```
magicCarpet.setChangeSetFile(Paths.get("src/test/java/ChangeSet.xml"));
```

InputStream that points to the file:
```
magicCarpet.setChangeSetFile(new FileInputStream(new File("src/test/java/ChangeSet.xml")));
```

Inside the ChangeSet.xml is where the definitions for all the changes you want to create sit:
```
<changeList>
    <change id="1.0.0" delimiter=";">
        <script>
            create table test(id integer, test date);
            alter table test add column another varchar(64);
            create table second(id varchar(64))
        </script>
    </change>
    <change id="1.0.1">
        <file>src\test\java\test.sql</file>
    </change>
    <change id="1.1.0">
        <file>classpath:classpathTest.sql</file>
    </change>
</changeList>
```
Each change configuration has an ID and either a script or the name of a file that holds the SQL to execute. File paths are either the full path, relative or absolute, to the file you wish to use, or are prefixed by "classpath:" which tells the library to look for the file in the classpath.

##### Delimiter
The files can also have a custom delimiter for when there are multiple statements to execute. By default the delimiter is set to semi colon, however if you wish to make it something else then you can specify it there.