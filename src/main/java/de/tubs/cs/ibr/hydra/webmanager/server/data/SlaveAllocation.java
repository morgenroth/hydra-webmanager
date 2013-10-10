package de.tubs.cs.ibr.hydra.webmanager.server.data;

public class SlaveAllocation {
    public Long slaveId = null;
    public Long capacity = null;
    public Long allocation = null;
    
    public SlaveAllocation(Long slaveId) {
        this.slaveId = slaveId;
        this.capacity = 0L;
        this.allocation = 0L;
    }
    
    public Long getAvailableSlots() {
        return capacity - allocation;
    }
}
