package de.tubs.cs.ibr.hydra.webmanager.server.data;

public class NodeAddress {
    
    private Long address = 0L;
    private Long netmask = 0L;
    
    public NodeAddress(long address, Long netmask) {
        this.address= address;
        this.netmask = netmask;
    }
    
    public NodeAddress(String address, String netmask) {
        this.address = toNumber(address);
        this.netmask = toNumber(netmask);
    }
    
    public NodeAddress(String address) {
        // split at '/'
        if (address.contains("/")) {
            String[] data = address.split("/", 1);
            this.address = toNumber(data[0]);
            this.netmask = toNumber(data[1]);
        }
        else {
            this.address = toNumber(address);
            this.netmask = null;
        }
    }

    public String toString() {
        return toString(address) + "/" + toString(netmask);
    }
    
    private static String toString(long number) {
        StringBuilder sb = new StringBuilder(15);

        for (int i = 0; i < 4; i++) {
            sb.insert(0, Long.toString(number & 0xff));

            if (i < 3) {
                sb.insert(0, '.');
            }

            number >>= 8;
        }

        return sb.toString();
    }
    
    private static long toNumber(String address) {
        long result = 0;
        String[] atoms = address.split("\\.");
        
        for (int i = 3; i >= 0; i--) {
            result |= (Long.parseLong(atoms[3 - i]) << (i * 8));
        }
        
        return result & 0xFFFFFFFF;
    }
    
    public Long getLongAddress() {
        return address;
    }
    
    public Long getLongNetmask() {
        return netmask;
    }
    
    public String getAddress() {
        return toString(address);
    }
    
    public String getNetmask() {
        if (netmask == null) return "0.0.0.0";
        return toString(netmask);
    }
}
