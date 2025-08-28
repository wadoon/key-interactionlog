# key-interactionlog -- A Logger for User Interactions in KeY

*InteractionLog* is a plugin for the graphical user interface in KeY.
It helps you to understand your steps during the proof, to gather
statistical information and to create proof script for automating your
interactions. to achieve this, InteractionLog hooks into KeY to record
interactions triggered by the user. The recorded logbooks including
the information to redo these actions can be stored as JSON files.
Redos are not limited on the original proofs, you can open a logbook
and also apply the stored interactions on different proofs.

<img src="doc/screen.png" alt="" width="100%">````

## Getting Started

### Using official releses on maven central

As a KeY developer, you can also add InteractionLog into your KeY build
as a dependency in `build.gradle`. Just add the following lines to `key.ui/build.gradle`:

```gradle 
dependencies {
  //...
  runtimeOnly("io.github.wadoon.key:key-interactionlog:1.0.0")
}
```

Current version: ![Maven Central Version](https://img.shields.io/maven-central/v/io.github.wadoon.key/key-interactionlog)

#### Using SNAPSHOT version

If you prefer a SNAPSHOT version use the following setup: 
```
repositories {
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots")}
}

dependencies {
  //...
  runtimeOnly("io.github.wadoon.key:key-interactionlog:1.1.0-SNAPSHOT")
}
```

### Manual installation

Receive the key-interactionlog and its dependencies by download from maven central. A download script is provided ([download.sh](https://github.com/wadoon/key-interactionlog/refs/heads/main/download.sh)).

Use: 
```sh
$ curl https://raw.githubusercontent.com/wadoon/key-interactionlog/refs/heads/main/download.sh | sh
```
or download the Jar files provided as a Zip file: [key-interactionlog-1.0.0.zip](https://github.com/wadoon/key-interactionlog/releases/download/KEY-INTERACTIONLOG-v1.0.0/key-interactionlog-1.0.0.zip)


<!--
`mvn dependency:get -Dartifact=io.github.wadoon.key:key-interactionlog:1.0.0:jars -Dmaven.repo.local=key-interactionlog-jars`

   -DremoteRepositories=https://myrepo.com/maven2

    A Jar file should be `interactionlog/build/libs/keyext.interactionlog-*all.jar`.  
-->

Start KeY by supplying the downloaded Jar files: 
```
$ export INTERACTIONLOG=key-interactionlog-1.0.0
$ java -cp key-2.12.4-dev-exe.jar:$INTERACTIONLOG/* de.uka.ilkd.key.core.Main
```

using the download folder `$INTERACTIONLOG` and the shadow Jar of KeY (`key-2.12.4-dev-exe.jar`).

Interaction Log should be automatically loaded on start up. A log is created for each loaded proof.

Note, Interaction Log is now compiled against KeY 2.12.4-dev. KeY's ABI/API is unstable, so please use the current main or SNAPSHOT version of KeY. Therefore, key-interactionlog is **not** useable with KeY 2.12.3 and below.


## History

* Version: 1.0.0 (released: 2025-08-25)  
  - The plugin was migrated to this repository to make it finally freely available.
  - Compiled against KEY-2.12.4-dev (current development version).

## User Interface

... work in progress ... 


## Releasing `key-abbrevmgr`

1. Remove SNAPSHOT from version number
2. Update `README.md`
3. Create new commit and tag.
4. `gradle makeDownloadScript`
5. `bash download.sh && zip -r key-interaction-version.zip key-interaction-version`
6. `gradle publishToCentral closeAndReleaseCentralStagingRepository`
7. Create new GitHub release, upload `key-interaction-version.zip`
8. Set version++, re-add to `SNAPSHOT` and commit.

