# BeaconAutomation
> original code from https://github.com/bjaanes/BeaconMqtt

## About
Android app to forward beacon events over MQTT or Webhooks (for home automation scenarios).

The basic idea behind the app is to allow you to integrate beacons into your home automation systems using MQTT or Webhooks.

In the app you can search for and add beacons. 
You can then configure an MQTT broker and/or Webhook broker that will be notified each time the beacons enter or exists the range of the phone.

There are a lot of configuration options like:
* MQTT Authentication
* MQTT Beacon Enter Topic
* MQTT Beacon Enter Distance Topic
* MQTT Beacon Exit Topic
* MQTT Beacon Exit Distance Topic
* Webhook URL + Content type + request method (POST/GET/PUT, ...)
* Webhook Beacon Enter Payload
* Webhook Beacon Enter Distance Payload
* Webhook Beacon Exit Payload
* Webhook Beacon Exit Distance Payload
* Time between scans
* Time spent on scans
* Minimum distance to trigger an event
* Notifications for debug purposes
* Log on the app for debug purposes

Libraries used:
* Eclipse MQTT
* Android AltBeacon

## Payload of MQTT Topics
All topics will have the following payload:
```
{
	uuid: "String - Beacon UUID",
	mac: "String - Beacon Bluetooth address,
	major: "String - Beacon Major",
	minor: "String - Beacon Minor",
	androidId: "String - Unique Android identification"
}
```
In addition, `distance` topics will have one extra property:
```
{
	distance: Number - current distance between Beacon and device
}
```

## Payload of Webhook calls
You can add your own properties to the payload JSON used by Webhooks.
In addition all payload will have the following JSON properties:
```
{
	uuid: "String - Beacon UUID",
	mac: "String - Beacon Bluetooth address,
	major: "String - Beacon Major",
	minor: "String - Beacon Minor",
	androidId: "String - Unique Android identification"
}
```
In addition, `distance` events will have one extra property:
```
{
	distance: Number - current distance between Beacon and device
}
```

## Screenshots

### Overview

![Overview](https://github.com/ben8p/BeaconMqtt/raw/development/screenshots/overview.jpg)

### Adding Beacons

![Adding Beacons](https://github.com/ben8p/BeaconMqtt/raw/development/screenshots/add_beacon.jpg)

### Settings

![Settings](https://github.com/ben8p/BeaconMqtt/raw/development/screenshots/settings.jpg)

### Beacons Settings

![Beacon Settings](https://github.com/ben8p/BeaconMqtt/raw/development/screenshots/beacon_settings.jpg)

### MQTT Settings

![MQTT Settings](https://github.com/ben8p/BeaconMqtt/raw/development/screenshots/mqtt_settings.jpg)

### Webhook Settings

![Webhook Settings](https://github.com/ben8p/BeaconMqtt/raw/development/screenshots/webhook_settings.jpg)

### Notifications

![Notifications](https://github.com/ben8p/BeaconMqtt/raw/development/screenshots/notification.png)

### Logs

![Logs](https://github.com/ben8p/BeaconMqtt/raw/development/screenshots/log.png)
