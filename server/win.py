"""
A simple Python script to send messages to a sever over Bluetooth using
Python sockets (with Python 3.3 or above).
"""
import sys
import socket
import bluetooth

def server():
    hostMACAddress = '98:2C:BC:46:88:B2'
    port = 8000
    backlog = 1
    size = 1024
    s = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    s.bind((hostMACAddress, port))
    s.listen(backlog)
    try:
        client, clientInfo = s.accept()
        while 1:
            data = client.recv(size)
            if data:
                print(data)
                client.send(data) # Echo back to client
    except:
        print("Closing socket")
        client.close()
        s.close()

def client():
    serverMACAddress = '98:2C:BC:46:88:B2' # The MAC address of a Bluetooth adapter on the server. The server might have multiple Bluetooth adapters.
    port = 8000
    s = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    s.connect((serverMACAddress, port))
    while 1:
        text = raw_input() # Note change to the old (Python 2) raw_input
        if text == "quit":
            break
        s.send(text)
    sock.close()

if __name__ == '__main__':
    try:
        if sys.argv[1] == 'server':
            server()
        else:
            client()
    except KeyboardInterrupt:
        print("\nBye")
