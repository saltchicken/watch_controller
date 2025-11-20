import socket
from evdev import UInput, ecodes as e

cap = {
    e.EV_KEY: [
        e.KEY_NEXTSONG, 
        e.KEY_PREVIOUSSONG, 
        e.KEY_VOLUMEUP, 
        e.KEY_VOLUMEDOWN,
        e.KEY_PLAYPAUSE # Added just in case you want to map the button to this later
    ]
}

ui = UInput(cap, name='WatchRemote')

HOST = '0.0.0.0'  
PORT = 5001        

def press_key(key_code):
    """Helper to press and release a key instantly"""
    ui.write(e.EV_KEY, key_code, 1)  # Press (1)
    ui.syn()                         # Sync
    ui.write(e.EV_KEY, key_code, 0)  # Release (0)
    ui.syn()                         # Sync

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((HOST, PORT))
        s.listen()
        print(f"Server listening on {HOST}:{PORT}...")
        print("‼️ Note: Ensure you are running this script with SUDO privileges for uinput access.")
        
        while True:
            try:
                conn, addr = s.accept()
                with conn:
                    print(f"Connected by {addr}")
                    while True:
                        data = conn.recv(1024)
                        if not data:
                            break
                        
                        message = data.decode('utf-8').strip()
                        print(f"Received: {message}")

                        if message == "Swipe Left":
                            print(">> Triggering: PREVIOUS SONG")
                            press_key(e.KEY_PREVIOUSSONG)
                        
                        elif message == "Swipe Right":
                            print(">> Triggering: NEXT SONG")
                            press_key(e.KEY_NEXTSONG)
                        
                        elif message == "Swipe Up":
                            print(">> Triggering: VOLUME UP")
                            press_key(e.KEY_VOLUMEUP)
                        
                        elif message == "Swipe Down":
                            print(">> Triggering: VOLUME DOWN")
                            press_key(e.KEY_VOLUMEDOWN)
                            
                        elif message == "Button Pressed":
                            print(">> Triggering: PLAY/PAUSE")
                            press_key(e.KEY_PLAYPAUSE)

            except Exception as ex:
                print(f"Error: {ex}")
                # Re-initialize or continue listening logic could go here
                pass

if __name__ == "__main__":
    start_server()
