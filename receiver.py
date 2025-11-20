import socket
import base64
from evdev import UInput, ecodes as e

# Input Setup
cap = {
    e.EV_KEY: [
        e.KEY_NEXTSONG, 
        e.KEY_PREVIOUSSONG, 
        e.KEY_VOLUMEUP, 
        e.KEY_VOLUMEDOWN,
        e.KEY_PLAYPAUSE,
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
    ui.write(e.EV_KEY, key_code, 1)
    ui.syn()
    ui.write(e.EV_KEY, key_code, 0)
    ui.syn()

def process_audio_data(pcm_data):
    # ‼️ YOUR LOGIC HERE
    print(f"Received audio chunk: {len(pcm_data)} bytes")
    # with open("output.pcm", "ab") as f:
    #     f.write(pcm_data)

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        print(f"Server listening on {HOST}:{PORT}...")
        
        while True:
            try:
                conn, addr = s.accept()
                with conn:
                    print(f"Connected by {addr}")
                    
                    # ‼️ NEW: Initialize a buffer string to hold partial data
                    buffer = ""
                    
                    while True:
                        data = conn.recv(4096)
                        if not data:
                            print("Client disconnected")
                            break
                        
                        # Add new data to the buffer
                        buffer += data.decode('utf-8')
                        
                        # ‼️ NEW: Process the buffer only when we find a newline
                        while '\n' in buffer:
                            # Split at the first newline: [message, rest_of_buffer]
                            message, buffer = buffer.split('\n', 1)
                            message = message.strip()
                            
                            if not message: continue
                            
                            # --- PROCESS MESSAGE ---
                            if message.startswith("AUDIO:"):
                                try:
                                    b64_data = message.replace("AUDIO:", "")
                                    pcm_data = base64.b64decode(b64_data)
                                    process_audio_data(pcm_data)
                                except Exception as ex:
                                    # This handles actual data corruption, not just fragmentation
                                    print(f"Audio decode err: {ex}")
                                    
                            elif message == "Swipe Left":
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
