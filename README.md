[![Build Status](https://travis-ci.org/PharmGKB/pgkb-ant.svg?branch=master)](https://travis-ci.org/PharmGKB/pgkb-ant)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.pharmgkb/pgkb-ant/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.pharmgkb/pgkb-ant)
[ ![Download](https://api.bintray.com/packages/pharmgkb/maven/pgkb-ant/images/download.svg) ](https://bintray.com/pharmgkb/maven/pgkb-ant/_latestVersion)

This repo contains [ant](https://ant.apache.org) tasks used by PharmGKB.


##### ExpandPropertiesTask

This task goes through existing user properties in the project and expands them.  This allows embedding multiple/nested properties such as:

```
url = ${scheme}://${server.${name}}/${path}
```

This task should be called after all properties have been read/created.


##### ExpandingPropertyTask

This is a drop-in replacement for `<property>` with supports property expansion (when set using the `value` attribute).

Example:

```xml
<property name="url" value="${scheme}://${server.${name}}/${path}" />
```

This also adds an `override` attribute (defaults to false) that allows the value of a property to be overridden.


##### MinimumJavaVersionTask

This task stops the build if the Java version used to perform the build is less than the specified version.

Example:
```xml
<minimumJavaVersion version="1.8" />
<minimumJavaVersion version="14" />
```


##### MaximumJavaVersionTask

This task stops the build if the Java version used to perform the build is greater than the specified version.

Example:
```xml
<maximumJavaVersion version="1.8" />
<maximumJavaVersion version="14" />
```
