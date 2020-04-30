#!/usr/bin/python
import time
from bluetooth import *

def connect_wifi(connection_data):
    data = connection_data.decode("utf-8").split('||')

    from wireless import Wireless
    wireless = Wireless()
    wireless.connect(ssid=data[0].strip(), password=data[1].strip())

    with open('./conexion.txt', 'w') as f:
        f.write(data[2])
    f.close()

def run_bluetooth_socket():
    server_sock = BluetoothSocket(RFCOMM)
    server_sock.bind(("", 6))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    uuid = "8ce255c0-223a-11e0-ac64-0803450c9a66"

    advertise_service(
        server_sock, "BluetoothChatInsecure",
        service_id = uuid,
        service_classes = [ uuid, SERIAL_PORT_CLASS ],
        profiles = [ SERIAL_PORT_PROFILE ],
    )

    print(f"Waiting for connection on RFCOMM channel {port}")

    while True:
        client_sock, client_info = server_sock.accept()
        print(f"Accepted connection from {client_info}")

        try:
            while True:
                data = client_sock.recv(1024)
                if len(data) == 0: break
                print(f"received [{data}]")
                connect_wifi(data)
        except IOError as e:
            print(f"ERROR {e}")

        print("disconnected")

        client_sock.close()

    server_sock.close()
    print("all done")

if __name__ == "__main__":
    try:
        run_bluetooth_socket()
    except KeyboardInterrupt:
        print("Bye")
