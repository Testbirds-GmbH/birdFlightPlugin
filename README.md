# Documentation birdflight-plugin

## Intro

We are pleased you use Birdflight to distribute your apps. If you use Jenkins then this plugin will facilitate your work and help you to integrate Jenkins into your deployment process. 

## Installation 

Please install the current build of the plugin over the Jenkins plugin manager after comipling

### Compile maven to hpi plugin

https://jenkins-ci.org/maven-hpi-plugin/ 

### Test the plugin in temporary jenkins

https://jenkins-ci.org/maven-hpi-plugin/ OR mvn hpi:run

## Setup

### GLobal Configuration

In Jenkins go to “Manage Jenkins” -> Configure System

Go to the section "Birdflight plugin". In there you can add multiple token from Birdflight. To identify them you can also give them a name ("Token Pair Name").
 

### Project configuration

In Jenkins go to Click on your project -> configure

Now you can add the upload to Birdflight under post-build action. 

| Parameter | description |
------------ | --------------
| Token Pair | Chosse your key you have created in the global config |
| Package identifier |	The App ID |
| Build identifier | The build id for later crashlogs and unique identifier of the build |
| IPA/APK Files | path to ur file (either apk or ipa) |
| dSYM File | Only available if ios |
| version | String of version |
| Build Notes | notes u want to generate |
| compatibility | What os version is comaptible |
| distribution | If it is an ios or android app -> leave empty if ios |
| isPublic | should the download link be public |





