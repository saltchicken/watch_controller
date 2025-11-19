import socket

HOST = '0.0.0.0'  
PORT = 5001        

def start_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((HOST, PORT))
        s.listen()
        print(f"Server listening on {HOST}:{PORT}...")
        
        while True:
            conn, addr = s.accept()
            with conn:
                print(f"Connected by {addr}")
                while True:
                    data = conn.recv(1024)
                    if not data:
                        break
                    
                    message = data.decode('utf-8').strip()
                    print(f"Received message: {message}")

                    if message == "Swipe Left":
                        print(">> Action: PREVIOUS")
                    elif message == "Swipe Right":
                        print(">> Action: NEXT")
                    elif message == "Swipe Up":
                        print(">> Action: VOLUME UP")
                    elif message == "Swipe Down":
                        print(">> Action: VOLUME DOWN")

if __name__ == "__main__":
    start_server()
