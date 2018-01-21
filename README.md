# pmd-migration-tool

This application migrates a pre-PMD6 ruleset to the new format.

To build:
```
$ ./gradlew clean installDist
```

The wrapper scripts are generated in `build/install/pmd-migration-tool/bin/`.
```
$ cd build/install/pmd-migration-tool/bin
```

Execute the script to try it out. For example, to display the help message:
```
$ ./pmd-migration-tool --help
```
Or to run the application:
```
$ ./pmd-migration-tool /path/to/ruleset
```

> There is a sample ruleset in `src/test/resources`.

After opening in IDE, run `xjc` Gradle task to generate the PMD classes.
