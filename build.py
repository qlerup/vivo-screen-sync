"""Build a release APK. Signing material is supplied privately through environment."""
import os, subprocess, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
BT=Path(os.environ['ANDROID_BUILD_TOOLS'])
JAR=Path(os.environ['ANDROID_PLATFORM_JAR'])
BUILD=ROOT/'build'; BUILD.mkdir(exist_ok=True)
CLASSES=BUILD/'classes'; CLASSES.mkdir(exist_ok=True)
def run(*args):subprocess.run([str(x) for x in args],check=True,cwd=ROOT)
run('javac','-encoding','UTF-8','-source','8','-target','8','-classpath',JAR,'-d',CLASSES,*ROOT.glob('src/**/*.java'))
run('java','-cp',BT/'lib/d8.jar','com.android.tools.r8.D8','--lib',JAR,'--min-api','26','--output',BUILD,*CLASSES.glob('**/*.class'))
suffix='.exe' if os.name=='nt' else ''
apk=BUILD/'unsigned.apk'
run(BT/('aapt'+suffix),'package','-f','-M',ROOT/'AndroidManifest.xml','-I',JAR,'-S',ROOT/'res','-F',apk)
with zipfile.ZipFile(apk,'a',compression=zipfile.ZIP_DEFLATED) as z:
 z.write(BUILD/'classes.dex','classes.dex')
 z.write(ROOT/'studio-settings.json','assets/studio-settings.json')
run(BT/('zipalign'+suffix),'-f','4',apk,BUILD/'aligned.apk')
run('java','-jar',BT/'lib/apksigner.jar','sign','--ks',os.environ['SCREEN_SYNC_KEYSTORE'],'--ks-pass','env:SCREEN_SYNC_KEY_PASSWORD','--out',ROOT/'app.apk',BUILD/'aligned.apk')
run('java','-jar',BT/'lib/apksigner.jar','verify','--verbose',ROOT/'app.apk')
