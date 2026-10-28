"""Create the small, readable Java/XML project skeleton. Does not touch other projects."""
from pathlib import Path
import shutil,os
ROOT=Path('D:/capstone-balut/frontend')
if (ROOT/'app/build.gradle').exists():
    raise SystemExit('Initial bootstrap already completed. Edit the existing Android Studio project; do not regenerate it.')
def put(name,text):
    file=ROOT/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(text.strip()+'\n',encoding='utf-8')
put('settings.gradle', '''pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = 'CapstoneBalut'
include ':app' ''')
put('build.gradle', "plugins { id 'com.android.application' version '9.2.0' apply false }")
put('gradle.properties', '''org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.workers.max=2
android.useAndroidX=true
android.nonTransitiveRClass=true
android.builtInKotlin=false''')
put('app/build.gradle', '''plugins { id 'com.android.application' }
android {
    namespace 'com.capstone.balut'
    compileSdk 36
    defaultConfig {
        applicationId 'com.capstone.balut'
        minSdk 26
        targetSdk 36
        versionCode 1
        versionName '1.0.0'
        testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'
    }
    buildTypes { release { minifyEnabled false } }
    compileOptions { sourceCompatibility JavaVersion.VERSION_17; targetCompatibility JavaVersion.VERSION_17 }
}
dependencies {
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'androidx.fragment:fragment:1.8.9'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test:runner:1.6.2'
    androidTestImplementation 'androidx.test.ext:junit:1.2.1'
}''')
sdk=(Path(os.environ['LOCALAPPDATA'])/'Android/Sdk').as_posix()
put('local.properties','sdk.dir='+sdk)
put('app/src/main/AndroidManifest.xml','''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.INTERNET"/>
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
  <application android:name=".BalutApplication" android:allowBackup="false" android:label="Balut Companion"
    android:icon="@drawable/ic_egg" android:theme="@style/Theme.Balut" android:supportsRtl="true"
    android:networkSecurityConfig="@xml/network_security_config">
    <activity android:name=".screens.auth.AuthActivity" android:exported="false"/>
    <activity android:name=".MainActivity" android:exported="true" android:windowSoftInputMode="adjustResize">
      <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
    </activity>
    <receiver android:name=".notifications.ReminderReceiver" android:exported="false">
      <intent-filter><action android:name="android.intent.action.BOOT_COMPLETED"/><action android:name="android.intent.action.TIME_SET"/><action android:name="android.intent.action.TIMEZONE_CHANGED"/></intent-filter>
    </receiver>
  </application>
</manifest>''')
put('app/src/main/res/xml/network_security_config.xml','''<network-security-config>
 <base-config cleartextTrafficPermitted="false"/>
 <domain-config cleartextTrafficPermitted="true"><domain>10.0.2.2</domain><domain>localhost</domain><domain>127.0.0.1</domain></domain-config>
</network-security-config>''')
put('app/src/main/res/values/styles.xml','''<resources>
 <style name="Theme.Balut" parent="Theme.AppCompat.Light.NoActionBar">
  <item name="android:fontFamily">sans</item><item name="android:colorAccent">#19795B</item>
  <item name="android:windowLightStatusBar">true</item><item name="android:statusBarColor">#F4F7F5</item>
  <item name="android:navigationBarColor">#FFFFFF</item><item name="android:windowLightNavigationBar">true</item>
  <item name="android:windowActionModeOverlay">true</item><item name="android:windowBackground">#F4F7F5</item>
 </style>
</resources>''')
put('app/src/main/res/layout/activity_main.xml','''<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/app_root" android:orientation="vertical" android:layout_width="match_parent" android:layout_height="match_parent" android:background="#F4F7F5">
 <LinearLayout android:layout_width="match_parent" android:layout_height="64dp" android:gravity="center_vertical" android:paddingHorizontal="20dp">
  <ImageView android:layout_width="32dp" android:layout_height="32dp" android:src="@drawable/ic_egg" android:contentDescription="Balut Companion logo"/>
  <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:layout_marginStart="10dp" android:text="Balut Companion" android:textSize="19sp" android:textStyle="bold" android:textColor="#143E35"/>
  <Button android:id="@+id/alerts_button" android:layout_width="wrap_content" android:layout_height="48dp" android:minWidth="48dp" android:text="Alerts" android:textAllCaps="false"/>
 </LinearLayout>
 <TextView android:id="@+id/connection_banner" android:layout_width="match_parent" android:layout_height="wrap_content" android:paddingHorizontal="20dp" android:paddingVertical="8dp" android:background="#EAF3ED" android:textColor="#143E35" android:textSize="12sp"/>
 <FrameLayout android:id="@+id/screen_container" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1"/>
 <LinearLayout android:id="@+id/bottom_navigation" android:layout_width="match_parent" android:layout_height="68dp" android:background="#FFFFFF" android:gravity="center" android:orientation="horizontal"/>
</LinearLayout>''')
put('app/src/main/res/layout/fragment_page.xml','''<ScrollView xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="match_parent" android:fillViewport="true" android:clipToPadding="false">
 <LinearLayout android:id="@+id/page_content" android:orientation="vertical" android:layout_width="match_parent" android:layout_height="wrap_content" android:padding="20dp" android:paddingBottom="28dp"/>
</ScrollView>''')
icons={
'egg':'M12,2 C8,2 4,10 4,15 C4,20 7,23 12,23 C17,23 20,20 20,15 C20,10 16,2 12,2 Z',
'home':'M3,11 L12,3 L21,11 L19,11 L19,21 L14,21 L14,15 L10,15 L10,21 L5,21 L5,11 Z',
'dashboard':'M3,3 H10 V10 H3 Z M14,3 H21 V14 H14 Z M3,14 H10 V21 H3 Z M14,18 H21 V21 H14 Z',
'scan':'M2,2 H8 V4 H4 V8 H2 Z M16,2 H22 V8 H20 V4 H16 Z M2,16 H4 V20 H8 V22 H2 Z M20,16 H22 V22 H16 V20 H20 Z M6,10 H18 V14 H6 Z',
'batches':'M3,4 H21 V9 H3 Z M3,11 H21 V16 H3 Z M3,18 H21 V22 H3 Z',
'profile':'M12,2 A5,5 0,1 1,11.99,2 Z M3,22 V19 C3,12 21,12 21,19 V22 Z'}
for name,path in icons.items():
    put('app/src/main/res/drawable/ic_'+name+'.xml',f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24"><path android:fillColor="#19795B" android:pathData="{path}"/></vector>')
put('gradle/wrapper/gradle-wrapper.properties','''distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-9.5.0-bin.zip
networkTimeout=600000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists''')
source=Path('D:/lantern/stellar-lantern/android')
for name in ['gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar']:
    target=ROOT/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source/name,target)
print('Android skeleton created at',ROOT)
