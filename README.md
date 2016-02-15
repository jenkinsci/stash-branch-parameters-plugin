StashBranchParametersPlugin
===========================

Wiki page: https://wiki.jenkins-ci.org/display/JENKINS/StashBranchParameter

Searches for branches and tags to use as a parameter

### Usage

In **This build is parameterized** section of your job you will have a new type of parameter to add - "Stash Branch Parameter". After adding it you have to, apart from entering a name (example: BRANCH_NAME), choose the repository to select branch names from.

You can use this branch name later on in your job config, for example in "Source Code Management" section for Git -> Branches to build -> Branch Specifier (blank for 'any') -> ${BRANCH_NAME}.

### Configuration

After installing this plugin you will find a new section **Stash branch parameter plugin** in your Jenkins configuration page.

Enter your Stash instance API URL (example: `https://stash.yourcompany.com/rest/api/1.0`) and a Stash user's with appropriate permissions username and password. You will be immediately notified if the authentication succeeded (no message) or failed (a red error message will be shown). 

###Refspec
To be able to build at a tag, you have to change the refspec to :
```
+refs/heads/*:refs/remotes/origin/* +refs/tags/*:refs/remotes/origin/tags/*
```
