1. run bluetooth daemon in compatibility mode to provide deprecated command line interfaces.

   sudo vim /etc/systemd/system/dbus-org.bluez.service

2. change the line

   ExecStart=/usr/lib/bluetooth/bluetoothd
   to
   ExecStart=/usr/lib/bluetooth/bluetoothd -C

3. Restart bluetooth

   sudo systemctl daemon-reload
   sudo systemctl restart bluetooth

4. Change permissions on /var/run/sdp

   sudo chmod 777 /var/run/sdp

5. Update piscan

   sudo hciconfig hci0 piscan