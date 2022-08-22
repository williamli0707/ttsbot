
# TTS Bot

A TTS Bot on Discord that can connect to your voice channel and play audio from text messages that you send. 


## Features

- Google Cloud TTS
- Settings including: 
    - User-specific language selection and guild default language selection
    - Sound volume control
    - Specific channel TTS or server-wide TTS
    - Require user to be in a voice channel to send TTS
- Skip, ping, debug commands
- Sound control and custom TTS coming maybe
- Customizable prefix
## How to Build TTS Bot for Personal Use

This allows you to have more access to Google TTS's features, and guarantees that you won't pass Google TTS's monthly quota for Wavenet voices. 

1. Download this project and open it in IntelliJ. 
2. Sign up for Google Cloud at cloud.google.com. You will need a credit card to sign up, but Google will not charge you unless you upgrade your account. 
3. Go to the Google Cloud Console and create a project. On this project, go to the search bar and search for Text to Speech. Select the Cloud Text-to-Speech API under Marketplace and click Enable. 
4. Follow the instructions in https://cloud.google.com/text-to-speech/docs/before-you-begin. You don't need to worry about setting up the environment variable yet. Keep the JSON key in a safe place. 
5. Create a MongoDB account at mongodb.com and create a database. Choose AWS/Google Cloud/Azure based on the proximity of the server region to your VPS location. If you will be self hosting on your own machine, then choose based on your location. Choose Username/Password for authentication and add 0.0.0.0/0 to your IP Address list, or another IP if you know what you're doing. 
6. Go to Browse Collections in your database, and choose the option saying to use your own data. Create a database called "main" with the collection called "guilds," and after you create it create another collection under the "main" database called "members" by hovering on main on the left sidebar and clicking the plus button. 
7. Go back to your databases and click "Connect." Click "Connect your application," "Java," and versions 4.3+. Copy the string given. It should look similar to "mongodb+srv://<username>:<password>@cluster0.-----.mongodb.net/?retryWrites=true&w=majority". Replace the <username> and <password> with your MongoDB database username and password you created when setting up the database. 
8. Go to your IntelliJ project and create a new text file in the base project directory called "Config.txt" without the double quotes. 
9. Paste your connection string into this file. 
10. Sign in to the Discord Developer Portal and create a new application. Add a bot to this application in the "Bot" tab of your application. Copy the token and paste it before the  MongoDB line in your Config.txt. Make it a public bot and turn on the intents. Go to OAuth2 and for Default Authorization Link click In-App Authorization. For scopes click bot and appplication.commands and give it Administrator permissions for now. 
11. Go to URL generator in OAuth2 and give it all the scopes and administrator permission for now. Copy the link and invite it to a test server. 
12. Go to your project and in IntelliJ's terminal type "mvn clean package" without the quotes. After executing this and waiting for it to run a .jar file will appear in the "target" folder in the base project directory. Choose the one that isn't "original-ttsbot....jar" and transfer the file to your VPS through SCP. If you have SSH installed, run the command "scp <-i path-to-ssh-key-if-you-need-it.key> target/<ttsbot name>.jar <username-of-vps>@<ip-address-make-sure-port-forwarding-is-on>:~/path/to/ttsbot/folder".  Also SCP your Config.txt file to your VPS, and make sure it is in the same location. Along with that, SCP your json credentials file. 
13. Access your VPS and go into terminal. Keep in mind that you cannot turn on your bot by using SSH because it will close the program after you exit the session. I recommend using VNC Viewer after turning on VNC hosting, or using tmux through SSH to preserve the session after running the program. 
14. In your terminal, type `nano ~/.bashrc`. Replace `bashrc` with `zshrc` if you use zsh, and so on. In the text editor that opens in terminal, add a line `export GOOGLE_APPLICATION_CREDENTIALS=<path-to-your-json-file>`. Restart your terminal session. 
15. Then, go to wherever the jar file is located and run it using `java -jar <name-of-jar>`. 
## License

Licensed under the [GNU GPL V3](https://www.gnu.org/licenses/gpl-3.0.txt). 
## Screenshots

![App Screenshot](https://media.discordapp.net/attachments/975095245874266112/1011140412594995281/unknown.png)
![App Screeshot](https://media.discordapp.net/attachments/975095245874266112/1011140610956210176/unknown.png)


