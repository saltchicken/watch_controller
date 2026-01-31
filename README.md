sudo pacman -S jdk17-openjdk android-tools gradle unzip wget
mkdir -p ~/Android/Sdk
cd ~/Android/Sdk/
mv commandlinetools-linux-14742923_latest.zip cmdline-tools.zip
unzip cmdline-tools.zip

mkdir -p cmdline-tools/latest
mv cmdline-tools/bin cmdline-tools/latest/
mv cmdline-tools/lib cmdline-tools/latest/
mv cmdline-tools/NOTICE.txt cmdline-tools/latest/
mv cmdline-tools/source.properties cmdline-tools/latest/

ls cmdline-tools/latest
yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

----------------------

git clone https://github.com/saltchicken/watch_controller
cd watch_controller/

adb pair 10.0.0.15:41039
adb connect 10.0.0.15:42221
adb uninstall com.example.watchtestapp
gradle installDebug


may need to run
sudo ufw allow 5001/tcp

