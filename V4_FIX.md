# V4 fix

The supplied GitHub Actions log showed the actual failure was:
`:app:checkDebugDuplicateClasses`

It found Kotlin stdlib 1.8.22 together with old kotlin-stdlib-jdk7/jdk8 1.6.21.
V4 excludes the obsolete split JDK7/JDK8 jars and aligns kotlin-stdlib to 1.8.22.
The workflow also prints the resolved runtime dependency tree before building.
