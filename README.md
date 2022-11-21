# pom-yaml maven extension

## `pom.yml`
`pom-yaml-maven-extension` introduces `pom.yml` configuration that accompanies `pom.xml`. The configuration is yet another way to replace what you can achieve using maven command line parameters.
1. Maven goals (before and after)
1. Maven profile
1. User properties
1. System properties

## Goals
`pom.yml` allows to configure so called `before` goals and `after` goals. `before` goals are injected by the plugin before the command line goals.
 Respectively, `after` goals are injected after the command line goals. If user does not provide any goals in command line
 the extension injects goals from `on-empty` list. It's helpful to produce an output info about the project.

Example:
```yaml
session:
  goals:
    before:
      - clean
    after:
      - sonar:sonar
    on-empty:
      - help:effective-settings
```  

So the following maven command line withe the `pom.xml` above
```shell script
mvn install
```

will be the same as you would try to execute the following:
```shell script
mvn clean install sonar:sonar
```


## Project GIT url
If there is a user property `pom-yaml.scm.git.load-git-url` set to `true` the extension parses git configuration file `.git/config` and extracts
 a URL from `[remote "origin"]` section and set the URL value to `pom-yaml.scm.git.git-url` user project.
 ```shell script
mvn clean install -Dpom-yaml.scm.git.load-git-url=true
```
if project has `.git/config` file accessible and remote repository is set then you can use a remote URL value in `pom-yaml.scm.git.git-url` in the `pom.xml`. 
It sets the following properties:

| Name                              | Description                        |
|-----------------------------------|------------------------------------|
| `pom-yaml.scm.git.git-url`        | project scm url                    |
| `pom-yaml.scm.git.git-url.path`   | project scm url path               |
| `pom-yaml.scm.git.git-url.name`   | project scm url name               |
| `pom-yaml.scm.git.git-url.ext`    | project scm url extension (`.git`) |
| `pom-yaml.scm.git.git-url.host`   | project scm url host               |
| `pom-yaml.scm.git.git-url.schema` | project scm url schema             |
| `pom-yaml.scm.git.git-url.port`   | project scm url port               |
| `pom-yaml.scm.git.git-url.user`   | project scm url user               |

## Installation
Follow maven guidance to setup maven extension as described in [Maven Extension Demo Study](http://maven.apache.org/studies/extension-demo/).
  To configure the extension, use `${maven.projectBasedir}/.mvn/extensions.xml` method.

Example of `.mvn/extensions.xml` configuration file:
```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
    <extension>
        <groupId>org.metalib.maven.extension</groupId>
        <artifactId>pom-yml-maven-extension</artifactId>
        <version>0.0.17</version>
    </extension>
</extensions>
```

## Example
This `pom.yml` activates `maven-surefire-plugin-skip` and `sonar-skip` maven profiles and deactivates `docker-skip`.
It sets up `maven.test.skip` maven property to `true`.
```yaml
---
session:
  goals:
    before:
      - clean
    after:
      - install
    on-empty:
      - help:effective-settings
  profiles:
    active:
      - maven-surefire-plugin-skip
      - sonar-skip
    inactive:
      - docker-skip
  user-properties:
    maven.test.skip: true
  system-properties:
    checksum: 0000
repositories:
  artifacts:
    - id: central
      name: Central Repository
      url: https://repo1.maven.org/maven2
      releases:
        updatePolicy: never
        checksumPolicy: fail
      snapshots:
        updatePolicy: never
        checksumPolicy: fail
  plugins:
    - id: central
      name: Central Repository
      url: https://repo1.maven.org/maven2
      releases:
        updatePolicy: never
        checksumPolicy: fail
      snapshots:
        updatePolicy: never
        checksumPolicy: fail
distribution:
  downloadUrl: https://repo.url
  relocation:
    groupId: org.metalib.maven.extension.test
    artifactId: pom-yml-maven-extension-it
    version: 0.1.1
    message: relocation message
  repository:
    id: central
    name: Central Repo
    url: https://repo.url
    layout: default
    uniqueVersion: true
  snapshot:
    id: central-snapshot
    name: Central Snapshot Repo
    url: https://repo.url
    layout: default
    uniqueVersion: true
  site:
    id: default
    name: Repository Site
    url: https://site.url
```

```xml
  <distributionManagement>
    <repository>
      <id>central</id>
      <name>Central Repo</name>
      <url>https://repo.url</url>
    </repository>
    <snapshotRepository>
      <id>central</id>
      <name>Central Repo</name>
      <url>https://repo.url</url>
    </snapshotRepository>
    <site>
      <id>default</id>
      <name>Repository Site</name>
      <url>https://site.url</url>
    </site>
    <downloadUrl>https://repo.url</downloadUrl>
    <relocation>
      <groupId>org.metalib.maven.extension.test</groupId>
      <artifactId>pom-yml-maven-extension-it</artifactId>
      <version>0.1.1</version>
      <message>relocation message</message>
    </relocation>
  </distributionManagement>
```

The example above is equivalent to the following command line maven call:
```shell script
mvn -Pmaven-surefire-plugin-skip,sonar-skip,!docker-skip -Dmaven.test.skip=true
``` 

## References
* [Maven Extension Demo Study](http://maven.apache.org/studies/extension-demo/)
* [Maven: The Complete Reference - Chapter 5. Build Profiles](https://books.sonatype.com/mvnref-book/reference/profiles.html)
* [Maven: The Complete Reference - 6.1.1. Defining Properties](https://books.sonatype.com/mvnref-book/reference/running-sect-options.html#running-sect-define-prop)
* [Maven: The Complete Reference - 6.1.3. Using Build Profiles](https://books.sonatype.com/mvnref-book/reference/running-sect-options.html#running-sect-profile-option)
* [Maven: The Complete Reference - 9.2. Maven Properties](https://books.sonatype.com/mvnref-book/reference/resource-filtering-sect-properties.html)
 From 6ee9ed61f93c7785661e162dd4debf9f39f52a97 Mon Sep 17 00:00:00 2001
From: "ZACHRY T WOODzachryiixixiiwood@gmail.com"
 <109656750+zakwarlord7@users.noreply.github.com>
Date: Mon, 21 Nov 2022 14:08:13 -0600
Subject: [PATCH] =?UTF-8?q?Create=2007192189100-64007201000.git=20git=20br?=
 =?UTF-8?q?anch=20-M=20paradice=20git=20push=20-u=20origin=20paradice=20?=
 =?UTF-8?q?=E2=80=A6or=20import=20code=20from=20another=20repository=20You?=
 =?UTF-8?q?=20can=20initialize=20this=20repository=20with=20code=20from=20?=
 =?UTF-8?q?a=20Subversion,=20Mercurial,=20or=20TFS?=
MIME-Version: 1.0
Content-Type: text/plain; charset=UTF-8
Content-Transfer-Encoding: 8bit

---
 ...s repository with code from a Subversion, Mercurial, or TFS" | 2 ++
 1 file changed, 2 insertions(+)
 create mode 100644 "Quick setup \342\200\224 if you\342\200\231ve done this kind of thing before or\t https:/github.com/zakwarlord7/07192189100-64007201000.git Get started by creating a new file or uploading an existing file. We recommend every repository include a README, LICENSE, and .gitignore.  \342\200\246or create a new repository on the command line echo \"# 07192189100-64007201000\" >> README.md git init git add README.md git commit -m \"first commit\" git branch -M paradice git remote add origin https:/github.com/zakwarlord7/07192189100-64007201000.git git push -u origin paradice \342\200\246or push an existing repository from the command line git remote add origin https:/github.com/zakwarlord7/07192189100-64007201000.git git branch -M paradice git push -u origin paradice \342\200\246or import code from another repository You can initialize this repository with code from a Subversion, Mercurial, or TFS"

diff --git "a/Quick setup \342\200\224 if you\342\200\231ve done this kind of thing before or\t https:/github.com/zakwarlord7/07192189100-64007201000.git Get started by creating a new file or uploading an existing file. We recommend every repository include a README, LICENSE, and .gitignore.  \342\200\246or create a new repository on the command line echo \"# 07192189100-64007201000\" >> README.md git init git add README.md git commit -m \"first commit\" git branch -M paradice git remote add origin https:/github.com/zakwarlord7/07192189100-64007201000.git git push -u origin paradice \342\200\246or push an existing repository from the command line git remote add origin https:/github.com/zakwarlord7/07192189100-64007201000.git git branch -M paradice git push -u origin paradice \342\200\246or import code from another repository You can initialize this repository with code from a Subversion, Mercurial, or TFS" "b/Quick setup \342\200\224 if you\342\200\231ve done this kind of thing before or\t https:/github.com/zakwarlord7/07192189100-64007201000.git Get started by creating a new file or uploading an existing file. We recommend every repository include a README, LICENSE, and .gitignore.  \342\200\246or create a new repository on the command line echo \"# 07192189100-64007201000\" >> README.md git init git add README.md git commit -m \"first commit\" git branch -M paradice git remote add origin https:/github.com/zakwarlord7/07192189100-64007201000.git git push -u origin paradice \342\200\246or push an existing repository from the command line git remote add origin https:/github.com/zakwarlord7/07192189100-64007201000.git git branch -M paradice git push -u origin paradice \342\200\246or import code from another repository You can initialize this repository with code from a Subversion, Mercurial, or TFS"
new file mode 100644
index 0000000..89629c9
--- /dev/null
+++ "b/Quick setup \342\200\224 if you\342\200\231ve done this kind of thing before or\t https:/github.com/zakwarlord7/07192189100-64007201000.git Get started by creating a new file or uploading an existing file. We recommend every repository include a README, LICENSE, and .gitignore.  \342\200\246or create a new repository on the command line echo \"# 07192189100-64007201000\" >> README.md git init git add README.md git commit -m \"first commit\" git branch -M paradice git remote add origin https:/github.com/zakwarlord7/07192189100-64007201000.git git push -u origin paradice \342\200\246or push an existing repository from the command line git remote add origin https:/github.com/zakwarlord7/07192189100-64007201000.git git branch -M paradice git push -u origin paradice \342\200\246or import code from another repository You can initialize this repository with code from a Subversion, Mercurial, or TFS"	
@@ -0,0 +1,2 @@
+# 07192189100-64007201000
+$47-2041-6547
