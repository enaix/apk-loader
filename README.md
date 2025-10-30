## Android dynamic APK loader

Proof-of-concept Android developer verification circumvention tool

**Note: this code is not working, early WIP. No activity handling is performed**

### Compilation

Compilation is done using the `compile` script. Usage:

```
Usage: [ANDROID_HOME=path-to-sdk-bin] compile SOURCE_FOLDER [PACKAGE_NAME]
Compiles and signs the apk from the source folder with the optional package name. ANDROID_HOME environment should be set to the android sdk.
Package name should be set for non-standard target in the form of com/example/package (default moe/enx/loader)
```

Run `ANDROID_HOME="..." ./compile src` to build the loader app

You may also build the example target app using `ANDROID_HOME="..." ./compile example com/example/dynamic`
