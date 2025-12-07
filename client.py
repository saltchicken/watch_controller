import socket
import subprocess
import time

SERVER_IP = "10.0.0.3"  # ‼️ REPLACE with your Server IP
PORT = 5001


def execute_command(cmd_text):
    cmd_text = cmd_text.lower()
    print(f"Received: {cmd_text}")

    # 1. Handle Gestures (From Watch)
    if "swipe right" in cmd_text:
        subprocess.run(["playerctl", "next"])
    elif "swipe left" in cmd_text:
        subprocess.run(["playerctl", "previous"])
    elif "swipe up" in cmd_text:
        subprocess.run(["pactl", "set-sink-volume", "@DEFAULT_SINK@", "+5%"])
    elif "swipe down" in cmd_text:
        subprocess.run(["pactl", "set-sink-volume", "@DEFAULT_SINK@", "-5%"])

    # 2. Handle Voice Commands (From Whisper)
    elif "terminal" in cmd_text:
        subprocess.Popen(["kitty"])  # Or alacritty/foot
    elif "browser" in cmd_text:
        subprocess.Popen(["firefox"])
    elif "close" in cmd_text:
        subprocess.run(["hyprctl", "dispatch", "killactive"])

    # 3. Hyprland Specifics
    elif "workspace" in cmd_text:
        # naive parsing for "workspace two"
        if "one" in cmd_text or "1" in cmd_text:
            subprocess.run(["hyprctl", "dispatch", "workspace", "1"])
        elif "two" in cmd_text or "2" in cmd_text:
            subprocess.run(["hyprctl", "dispatch", "workspace", "2"])


def main():
    while True:
        try:
            print(f"Connecting to Server at {SERVER_IP}...")
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.connect((SERVER_IP, PORT))

                # ‼️ HANDSHAKE: Tell the server we are the Client
                s.sendall(b"CLIENT_CONNECTED\n")
                print("Connected! Waiting for commands...")

                buffer = ""
                while True:
                    data = s.recv(4096)
                    if not data:
                        break
                    buffer += data.decode("utf-8")

                    while "\n" in buffer:
                        message, buffer = buffer.split("\n", 1)
                        message = message.strip()

                        # ‼️ The server sends commands prefixed with CMD:
                        if message.startswith("CMD:"):
                            raw_cmd = message.split(":", 1)[1]
                            execute_command(raw_cmd)

        except Exception as e:
            print(f"Connection lost: {e}. Retrying in 5s...")
            time.sleep(5)


if __name__ == "__main__":
    main()
