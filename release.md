# LSW Serialization Branching and Releasing

For lsw serialization project we use gitflow. You can read more about it below:

  * [Git Flow](http://nvie.com/posts/a-successful-git-branching-model/)
  * [Git Flow Cheat Sheet](https://danielkummer.github.io/git-flow-cheatsheet/)

First you'll have to initialize gitflow for the project, see Configuring git-flow. Once done, just run release.sh (see Performing a release)

## Configuring git-flow

```bash
$ git flow init

Which branch should be used for bringing forth production releases?
   - disposition-hotfix
   - master
   - release
Branch name for production releases: [master] release

Which branch should be used for integration of the "next release"?
   - disposition-hotfix
   - master
Branch name for "next release" development: [master] 

How to name your supporting branch prefixes?
Feature branches? [feature/] 
Release branches? [release/] rel/
Hotfix branches? [hotfix/] 
Support branches? [support/] 
Version tag prefix? [] serialization-
$
```

## Performing a release - Starting from master branch
Obtain the current snapshot version from the pom.xml
It should look like 0.64-SNAPSHOT.  The next release is this version without the SNAPSHOT string.

```bash
./release.sh <currentVersion> <nextVersion>-SNAPSHOT
```
Example:
```bash
./release.sh 0.64 0.65-SNAPSHOT
```
_Note: Make sure you using a 0.NN version number_

Go to [Jenkins Core Serialization](http://dev-lswcore-jenkins-master.dev.aws.lcloud.com/job/lsw-serialization-release/) and click "Build Now"

Leave the pre-populated commit messages as is.  
For the Tag value, use the release version number.  From this example, that would be "0.64".  This value creates the tag for the release.

