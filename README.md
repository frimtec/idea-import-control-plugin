# IntelliJ-IDEA Import Control Plugin 
[![Build Status](https://travis-ci.com/frimtec/idea-import-control-plugin.svg?branch=main)](https://travis-ci.com/frimtec/idea-import-control-plugin) 
[![license](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

![Icon](images/icon.png)


IntelliJ-IDEA inspection rule for Java [Import Control](href="https://github.com/frimtec/import-control/blob/main/README.md).

Reports imports and references to module private classes.
![Icon](images/errors.png)
Java previous to JDK 9 has a visibility concept that does not support modules very well.
If modules are structured into various packages, and only dedicated packages should be exported to other modules, the visibilty modifiers do not fit at all. All public classes can be imported to other modules.<br>
With JDK 9 JPMS was introduced to solve this issue. Using JPMS in a project is not an easy task and sometimes even not possible due to runtime restrictions.
This is where [Import Control](href="https://github.com/frimtec/import-control/blob/main/README.md) comes to rescue. It fully supports the need for import control between modules but with no runtime impact nor restrictions.
Packages of modules that shall be exported to other modules can be annotated with ```@ExportPackage``` (or any other annotation of your choice). While editing in the IDE imports to non exported classes from other modules are immediatly marked as errors.
                
## How to use
* Install the Plugin ```Import Control``` from the Jetbrains Plugin-Repository.
* Configure the inspection rule ```Import/reference of module private classes``` (in group Java / Visibility).

## Configuration 
![Icon](images/settings.png)
### Package export annotation
Defines the package annotation that flags the package as module export (default ```com.github.frimtec.libraries.importcontrol.api.ExportPackage```).

### Root packages
Defines root package (or several if separated by ';') of your multi module project (only references to these root packages will be analysed). 
