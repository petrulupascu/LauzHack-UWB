# When error "Plugin with id 'com.android.application' not found" in Android Studio:
Go to project directory. Then
```sudo chmod 777 gradlew```
Version check: 
```./gradlew -v```
Add these lines on the very top of the file `build.gradle`:
```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.3'
    }
}

allprojects {
    repositories {
        jcenter()
    }
}
```
And add this in the `android` field:
```
android {
	.
	.
	.
	sourceSets {
            main {
            	manifest.srcFile 'AndroidManifest.xml'
            	java.srcDirs = ['java']
            	res.srcDirs = ['res']
            }
    	}
}
```
Execute in terminal
```
./gradlew wrapper
```
Try the Graddle thing again and follow the instructions in the error messages.
Then go to File > Invalidate Caches / Restart and Invalidate and Restart.
