PharmGKB-specific ant tasks:

##### ExpandPropertiesTask

This task goes through existing user properties in the project and expands them.  This allows embedding multiple/nested properties such as:

```
url = ${scheme}://${server.${name}}/${path}
```

This task should be called after all properties have been read/created.
