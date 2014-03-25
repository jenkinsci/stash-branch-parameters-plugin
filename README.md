StashBranchParametersPlugin
===========================

Searches for branches and tags to use as a parameter

###Refspec
To be able to build at a tag, you have to change the refspec to :
```
+refs/heads/*:refs/remotes/origin/* +refs/tags/*:refs/remotes/origin/tags/*
```
