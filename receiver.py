import socket
from evdev import UInput, ecodes as e

# ‼️ 1. Update capabilities to include letters
cap = {
    e.EV_KEY: [
        # Media Keys
        e.KEY_NEXTSONG, 
        e.KEY_PREVIOUSSONG, 
        e.KEY_VOLUMEUP, 
        e.KEY_VOLUMEDOWN,
        e.KEY_PLAYPAUSE,
        # VIM Navigation Keys
        e.KEY_H,
        e.KEY_J,
        e.KEY_K,
        e.KEY_L
    ]
}

ui = UInput(cap, name='WatchRemote')

HOST = '0.0.0.0'  
PORT = 5001        

def press_key(key_code):
    """Helper to press and release a key instantly"""
    ui.write(e.EV_KEY, key_code, 1)  # Press
    ui.syn()
    ui.write(e.EV_KEY, key_code, 0)  # Release
    ui.syn()

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((HOST, PORT))
        s.listen()
        print(f"Server listening on {HOST}:{PORT}...")
        print("Note: Ensure you are running this script with SUDO privileges.")
        
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

                        # Existing Swipe Logic
                        if message == "Swipe Left":
                            press_key(e.KEY_PREVIOUSSONG)
                        elif message == "Swipe Right":
                            press_key(e.KEY_NEXTSONG)
                        elif message == "Swipe Up":
                            press_key(e.KEY_VOLUMEUP)
                        elif message == "Swipe Down":
                            press_key(e.KEY_VOLUMEDOWN)
                        
                        elif message == "h":
                            press_key(e.KEY_H)
                        elif message == "j":
                            press_key(e.KEY_J)
                        elif message == "k":
                            press_key(e.KEY_K)
                        elif message == "l":
                            press_key(e.KEY_L)

            except Exception as ex:
                print(f"Error: {ex}")
                pass

if __name__ == "__main__":
    start_server()
