import socket
import base64
import time
import io
import threading
import queue
import numpy as np
from faster_whisper import WhisperModel

# ‼️ REMOVED: evdev/UInput (Server no longer presses keys)

HOST = "0.0.0.0"
PORT = 5001

# ‼️ NEW: Global variable to hold the socket of the execution client
client_connection = None

audio_queue = queue.Queue()
whisper_model = "medium.en"

print(f" >> Loading Whisper Model ({whisper_model})...")
model = WhisperModel(whisper_model, device="cuda", compute_type="int8")
print(" >> Model Loaded & Warming up...")

# Warmup
dummy_audio = np.zeros(16000, dtype=np.float32)
segments, _ = model.transcribe(dummy_audio, beam_size=5)
list(segments)
print(" >> Model Ready!")


# ‼️ NEW: Function to send commands to the designated Client
def send_to_client(command):
    global client_connection
    if client_connection:
        try:
            print(f" >> RELAYING: {command}")
            # We send a specific prefix so the client knows it's a command
            client_connection.sendall(f"CMD:{command}\n".encode("utf-8"))
        except Exception as e:
            print(f" !! Failed to send to client: {e}")
            client_connection = None
    else:
        print(f" !! No Client Connected. Dropping command: {command}")


def transcription_worker():
    while True:
        audio_data = audio_queue.get()
        if audio_data is None:
            break

        try:
            print(f" >> Processing Audio ({len(audio_data)} bytes)...")
            start = time.time()

            # Create virtual WAV
            virtual_file = io.BytesIO()
            import wave

            with wave.open(virtual_file, "wb") as wf:
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(16000)
                wf.writeframes(audio_data)
            virtual_file.seek(0)

            segments, _ = model.transcribe(virtual_file, beam_size=5)
            full_text = " ".join([s.text for s in segments]).strip()

            print(f" >> TRANSCRIPT: {full_text} ({time.time() - start:.2f}s)")

            # ‼️ NEW: Instead of pressing keys, forward text to client
            if full_text:
                send_to_client(full_text)

        except Exception as e:
            print(f"Transcription Error: {e}")
        finally:
            audio_queue.task_done()


# ‼️ NEW: Specialized handler for the Watch Connection
def handle_watch_connection(conn):
    print(" >> Handling WATCH connection")
    current_audio_buffer = bytearray()

    with conn:
        buffer = ""
        while True:
            try:
                data = conn.recv(4096)
                if not data:
                    break
                buffer += data.decode("utf-8")

                while "\n" in buffer:
                    message, buffer = buffer.split("\n", 1)
                    message = message.strip()
                    if not message:
                        continue

                    if message == "AUDIO_START":
                        print(" >> Audio Stream Started")
                        current_audio_buffer = bytearray()

                    elif message == "AUDIO_END":
                        if len(current_audio_buffer) > 0:
                            audio_queue.put(current_audio_buffer)
                        current_audio_buffer = bytearray()

                    elif message.startswith("AUDIO:"):
                        try:
                            b64_data = message.replace("AUDIO:", "")
                            current_audio_buffer.extend(base64.b64decode(b64_data))
                        except:
                            pass

                    # ‼️ NEW: If it's not audio, it's a gesture (Swipe Left, etc). Forward it!
                    elif message != "WATCH_CONNECTED":
                        send_to_client(message)
            except Exception as e:
                print(f"Watch Error: {e}")
                break
    print(" >> Watch Disconnected")


# ‼️ NEW: Specialized handler for the Client Connection
def handle_client_connection(conn):
    global client_connection
    print(" >> CLIENT REGISTERED. Ready to send commands.")
    client_connection = conn

    # Keep connection alive
    try:
        while True:
            # We just listen for a heartbeat or disconnect
            data = conn.recv(1024)
            if not data:
                break
    except:
        pass

    print(" >> CLIENT DISCONNECTED")
    client_connection = None
    conn.close()


def start_server():
    t = threading.Thread(target=transcription_worker, daemon=True)
    t.start()

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        print(f"Router Server listening on {HOST}:{PORT}...")

        while True:
            try:
                conn, addr = s.accept()
                print(f"Incoming connection from {addr}...")

                # ‼️ NEW: Handshake Logic
                # We peek at the first message to see WHO is connecting
                conn.settimeout(5)  # Give them 5 seconds to identify
                try:
                    # Read a small chunk to identify
                    first_msg = conn.recv(1024).decode("utf-8").strip()
                    conn.settimeout(None)  # Remove timeout

                    if "WATCH_CONNECTED" in first_msg:
                        threading.Thread(
                            target=handle_watch_connection, args=(conn,), daemon=True
                        ).start()
                    elif "CLIENT_CONNECTED" in first_msg:
                        threading.Thread(
                            target=handle_client_connection, args=(conn,), daemon=True
                        ).start()
                    else:
                        print(f"Unknown device: {first_msg}")
                        conn.close()
                except Exception as e:
                    print(f"Handshake failed: {e}")
                    conn.close()

            except Exception as ex:
                print(f"Connection Accept Error: {ex}")


if __name__ == "__main__":
    start_server()
