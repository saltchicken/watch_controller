import socket
import base64
import time
import wave  # ‼️ ADDED: Library for writing WAV files
from evdev import UInput, ecodes as e

# Input Setup
cap = {
    e.EV_KEY: [
        e.KEY_NEXTSONG, 
        e.KEY_PREVIOUSSONG, 
        e.KEY_VOLUMEUP, 
        e.KEY_VOLUMEDOWN,
        e.KEY_PLAYPAUSE,
        e.KEY_H, e.KEY_J, e.KEY_K, e.KEY_L
    ]
}
ui = UInput(cap, name='WatchRemote')

HOST = '0.0.0.0'
PORT = 5001

# Audio Configuration (Must match Android settings)
CHANNELS = 1
SAMPLE_WIDTH = 2  # 2 bytes (16-bit)
FRAME_RATE = 16000  # 16kHz

current_audio_buffer = bytearray()

def press_key(key_code):
    ui.write(e.EV_KEY, key_code, 1)
    ui.syn()
    ui.write(e.EV_KEY, key_code, 0)
    ui.syn()

def save_complete_audio(audio_data):
    """
    ‼️ UPDATED: Saves the buffer as a .wav file
    """
    if not audio_data:
        print("Received empty audio chunk.")
        return

    timestamp = int(time.time())
    filename = f"voice_command_{timestamp}.wav"
    
    print(f"Saving {filename} ({len(audio_data)} bytes)...")
    
    try:
        # Open a WAV file for writing
        with wave.open(filename, 'wb') as wf:
            wf.setnchannels(CHANNELS)
            wf.setsampwidth(SAMPLE_WIDTH)
            wf.setframerate(FRAME_RATE)
            wf.writeframes(audio_data)
        print(f"Saved successfully: {filename}")
    except Exception as e:
        print(f"Error saving wav: {e}")

def start_server():
    global current_audio_buffer
    
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
                    buffer = ""
                    
                    while True:
                        data = conn.recv(4096)
                        if not data:
                            print("Client disconnected")
                            break
                        
                        buffer += data.decode('utf-8')
                        
                        while '\n' in buffer:
                            message, buffer = buffer.split('\n', 1)
                            message = message.strip()
                            if not message: continue
                            
                            # 1. Start Recording
                            if message == "AUDIO_START":
                                print(" >> Start Recording...")
                                current_audio_buffer = bytearray()

                            # 2. Stop Recording & Save WAV
                            elif message == "AUDIO_END":
                                print(" >> Stop Recording.")
                                save_complete_audio(current_audio_buffer)
                                current_audio_buffer = bytearray()

                            # 3. Stream Data
                            elif message.startswith("AUDIO:"):
                                try:
                                    b64_data = message.replace("AUDIO:", "")
                                    pcm_data = base64.b64decode(b64_data)
                                    current_audio_buffer.extend(pcm_data)
                                except Exception as ex:
                                    print(f"Decode error: {ex}")

                            # Standard Commands
                            elif message == "Swipe Left": press_key(e.KEY_PREVIOUSSONG)
                            elif message == "Swipe Right": press_key(e.KEY_NEXTSONG)
                            elif message == "Swipe Up": press_key(e.KEY_VOLUMEUP)
                            elif message == "Swipe Down": press_key(e.KEY_VOLUMEDOWN)
                            elif message == "h": press_key(e.KEY_H)
                            elif message == "j": press_key(e.KEY_J)
                            elif message == "k": press_key(e.KEY_K)
                            elif message == "l": press_key(e.KEY_L)

            except Exception as ex:
                print(f"Error: {ex}")
                pass

if __name__ == "__main__":
    start_server()
