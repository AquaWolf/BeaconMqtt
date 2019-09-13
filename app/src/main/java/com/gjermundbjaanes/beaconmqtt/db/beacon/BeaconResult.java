package com.bsantalucia.beaconmqtt.db.beacon;

public class BeaconResult {

    private String uuid;
    private String mac;
    private String major;
    private String minor;
    private String informalName;

    public BeaconResult(String uuid, String mac, String major, String minor, String informalName) {
        this.uuid = uuid;
        this.mac = mac;
        this.major = major;
        this.minor = minor;
        this.informalName = informalName;
    }

    public String getUuid() {
        return uuid;
    }

    public String getMac() {
        return mac;
    }

    public String getMajor() {
        return major;
    }

    public String getMinor() {
        return minor;
    }

    public String getInformalName() {
        return informalName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BeaconResult) {
            BeaconResult other = (BeaconResult) obj;
            return this.uuid.equals(other.getUuid()) && this.mac.equals(other.getMac()) && this.major.equals(other.getMajor()) && this.minor.equals(other.getMinor());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (41 * (41 + (uuid + mac + major + minor).hashCode()));
    }
}
