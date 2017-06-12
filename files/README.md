Each [script](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/components/nexus-script/src/main/java/org/sonatype/nexus/script/ScriptXO.groovy)
in the `groovy/` directory is evaluated within a certain
[context](https://docs.oracle.com/javase/8/docs/api/javax/script/ScriptContext.html).
That context includes custom [bindings](https://docs.oracle.com/javase/8/docs/api/javax/script/Bindings.html)
that map names to objects. Those objects are then made available to the script by their associated names.
Available bindings include:

* `log`: `org.slf4j.Logger`
* `args`: `String`  (the arguments that the user passed to the script)

[Reference](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/plugins/nexus-script-plugin/src/main/java/org/sonatype/nexus/script/plugin/internal/rest/ScriptResource.groovy#L160)

* `beanLocator`: `BeanLocator` ([link](https://eclipse.org/sisu/docs/api/org.eclipse.sisu.inject/reference/org/eclipse/sisu/inject/BeanLocator.html))
* `container`: `GlobalComponentLookupHelper` ([link](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/components/nexus-common/src/main/java/org/sonatype/nexus/common/app/GlobalComponentLookupHelper.java))

[Reference](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/components/nexus-core/src/main/java/org/sonatype/nexus/internal/script/ScriptServiceImpl.java#L122)

* `blobStore`: `BlobStoreApiImpl` ([link](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/components/nexus-core/src/main/java/org/sonatype/nexus/internal/provisioning/BlobStoreApiImpl.groovy))
* `core`: `CoreApiImpl` ([link](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/components/nexus-core/src/main/java/org/sonatype/nexus/internal/provisioning/CoreApiImpl.groovy))
* `repository`: `RepositoryApiImpl` ([link](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/plugins/nexus-script-plugin/src/main/java/org/sonatype/nexus/script/plugin/internal/provisioning/RepositoryApiImpl.groovy))
* `security`: `SecurityApiImpl` ([link](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/components/nexus-security/src/main/java/org/sonatype/nexus/security/internal/SecurityApiImpl.groovy))

[Reference](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/components/nexus-core/src/main/java/org/sonatype/nexus/internal/script/ScriptServiceImpl.java#L125).
The classes above represent all of the concrete implementations of
[ScriptApi](https://github.com/sonatype/nexus-public/blob/tag-3.3.1-01/components/nexus-common/src/main/java/org/sonatype/nexus/common/script/ScriptApi.java)
in Nexus.
