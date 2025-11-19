import socket

# Listen on all network interfaces so the watch can find it
HOST = '0.0.0.0'  
PORT = 5001       

def start_server():
    # Create a TCP/IP socket
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        # Bind to the port

        s.bind((HOST, PORT))
        s.listen()
        print(f"Server listening on {HOST}:{PORT}...")
        
        while True:
            # Wait for a connection
            conn, addr = s.accept()
            with conn:
                print(f"Connected by {addr}")
                while True:
                    # Receive data (1024 bytes buffer size)
                    data = conn.recv(1024)
                    if not data:
                        break

                    print(f"Received message: {data.decode('utf-8').strip()}")

if __name__ == "__main__":
    start_server()