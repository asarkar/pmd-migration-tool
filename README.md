# pmd-migration-tool
[![Build Status](https://travis-ci.org/asarkar/pmd-migration-tool.svg?branch=master)](https://travis-ci.org/asarkar/pmd-migration-tool)

This application migrates a pre-PMD6 ruleset to the new format.

**Build locally**:
```
$ ./gradlew clean installDist
```

> After opening in IDE, run `xjc` Gradle task to generate the PMD classes.

**Run**:

It is distributed in two formats, ZIP and TAR, both available on [Bintray](https://bintray.com/asarkar/mvn/pmd-migration-tool).
Download and extract an archive, navigate to the `bin` directory and execute the script.

For example, to display the help message:
```
$ ./pmd-migration-tool --help
```
Or to run the application:
```
$ ./pmd-migration-tool /path/to/ruleset
```

> There is a sample ruleset in `src/test/resources`.

**Publish to Bintray**:
```
$ ./gradlew -P bintrayUser=user -P bintrayApiKey=secret \
    clean test bintrayUpload
```

## Contribute

This project is a volunteer effort. You are welcome to send pull requests, ask questions, or create issues.
If you like it, you can help by spreading the word!

## License

Copyright 2017-2018 Abhijit Sarkar - Released under [GNU General Public License v3.0](LICENSE).

