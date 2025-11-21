import socket
import base64
import time
import io # ‼️ Changed: Added for in-memory audio
import threading # ‼️ Changed: Added for non-blocking transcription
import queue # ‼️ Changed: Added for thread safety
from evdev import UInput, ecodes as e
from faster_whisper import WhisperModel

# Input Setup (Perfect for Hyprland/Wayland)
cap = {
    e.EV_KEY: [
        e.KEY_NEXTSONG, 
        e.KEY_PREVIOUSSONG, 
        e.KEY_VOLUMEUP, 
        e.KEY_VOLUMEDOWN,
        e.KEY_PLAYPAUSE,
        e.KEY_H, e.KEY_J, e.KEY_K, e.KEY_L,
        e.KEY_ENTER, e.KEY_BACKSPACE # ‼️ Added useful extra keys
    ]
}

try:
    ui = UInput(cap, name='WatchRemote')
except Exception as e:
    print(f"Warning: Could not create UInput device (needs root?): {e}")
    ui = None

HOST = '0.0.0.0'
PORT = 5001
current_audio_buffer = bytearray()

# ‼️ Changed: Queue to pass audio from Socket Thread to Whisper Thread
audio_queue = queue.Queue()

print(" >> Loading Whisper Model (tiny.en)...")
# ‼️ Check: Ensure you have CUDA drivers on CachyOS, otherwise swap to "cpu"
model = WhisperModel("tiny.en", device="cpu", compute_type="int8")
print(" >> Model Loaded.")

def press_key(key_code):
    if ui:
        ui.write(e.EV_KEY, key_code, 1)
        ui.syn()
        time.sleep(0.01) # ‼️ Added tiny sleep for stability
        ui.write(e.EV_KEY, key_code, 0)
        ui.syn()

# ‼️ New Function: Runs in background thread
def transcription_worker():
    while True:
        audio_data = audio_queue.get()
        if audio_data is None: break # Poison pill to stop thread
        
        try:
            print(f" >> Processing {len(audio_data)} bytes...")
            start = time.time()
            
            # ‼️ Changed: Convert raw PCM (16-bit, 16kHz Mono) to float32 for Whisper
            # Note: fast-whisper usually handles file-like objects. 
            # If raw bytes fail, we wrap in BytesIO imitating a WAV file is safer:
            import wave
            virtual_file = io.BytesIO()
            with wave.open(virtual_file, 'wb') as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(16000)
                wf.writeframes(audio_data)
            virtual_file.seek(0)

            segments, _ = model.transcribe(virtual_file, beam_size=5)
            
            full_text = ""
            for segment in segments:
                full_text += segment.text + " "
                
            print(f" >> TRANSCRIPT: {full_text.strip()} ({time.time() - start:.2f}s)")
            
            # ‼️ Logic: Map voice commands to Hyprland/App actions here
            cmd = full_text.lower().strip()
            if "enter" in cmd: press_key(e.KEY_ENTER)
            
        except Exception as e:
            print(f"Transcription Error: {e}")
        finally:
            audio_queue.task_done()

def start_server():
    global current_audio_buffer
    
    # ‼️ Start the background worker
    t = threading.Thread(target=transcription_worker, daemon=True)
    t.start()

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
                        if not data: break
                        buffer += data.decode('utf-8')
                        
                        while '\n' in buffer:
                            message, buffer = buffer.split('\n', 1)
                            message = message.strip()
                            if not message: continue
                            
                            if message == "AUDIO_START":
                                print(" >> Incoming Audio Stream...")
                                current_audio_buffer = bytearray()
                                
                            elif message == "AUDIO_END":
                                print(" >> Audio End. Queuing.")
                                # ‼️ Changed: Send to thread instead of blocking
                                if len(current_audio_buffer) > 0:
                                    audio_queue.put(current_audio_buffer)
                                current_audio_buffer = bytearray()
                                
                            elif message.startswith("AUDIO:"):
                                try:
                                    b64_data = message.replace("AUDIO:", "")
                                    current_audio_buffer.extend(base64.b64decode(b64_data))
                                except: pass
                                
                            # ‼️ Optimized: Dictionary mapping for cleaner code
                            else:
                                key_map = {
                                    "Swipe Left": e.KEY_PREVIOUSSONG,
                                    "Swipe Right": e.KEY_NEXTSONG,
                                    "Swipe Up": e.KEY_VOLUMEUP,
                                    "Swipe Down": e.KEY_VOLUMEDOWN,
                                    "h": e.KEY_H, "j": e.KEY_J, 
                                    "k": e.KEY_K, "l": e.KEY_L
                                }
                                if message in key_map:
                                    press_key(key_map[message])
                                    print(f" >> Command: {message}")

            except Exception as ex:
                print(f"Connection Reset: {ex}")

if __name__ == "__main__":
    start_server()
