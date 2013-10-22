package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.util.ArrayList;
import java.util.List;

public abstract class Event {
    private EventData mData;
    
    public Event(EventType t) {
        mData = this.createEventData();
        mData.setType(t);
    }
    
    public Event(EventData data) {
        mData = data;
    }
    
    public void setExtras(List<EventDataExtra> extras) {
        mData.setExtras(extras);
    }
    
    public List<EventDataExtra> getExtras() {
        return mData.getExtras();
    }
    
    public EventData getEventData() {
        return mData;
    }
    
    public void setExtra(String key, String data) {
        if ((key == null) || (data == null)) return;
        
        List<EventDataExtra> extras = mData.getExtras();
        
        if (extras == null) {
            extras = new ArrayList<EventDataExtra>();
        }
        
        for (EventDataExtra e : extras) {
            if (key.equals(e.getKey())) {
                e.setData(data);
                mData.setExtras(extras);
                return;
            }
        }
        
        EventDataExtra e = createExtra();
        e.setKey(key);
        e.setData(data);
        extras.add(e);
        mData.setExtras(extras);
    }
    
    protected abstract EventDataExtra createExtra();
    protected abstract EventData createEventData();
    
    public String getExtraString(String key) {
        if (mData.getExtras() == null) return null;
        
        for (EventDataExtra e : mData.getExtras()) {
            if (key.equals(e.getKey())) {
                return e.getData();
            }
        }
        
        return null;
    }
    
    public Long getExtraLong(String key) {
        String value = getExtraString(key);
        if (value == null) return null;
        return Long.valueOf(value);
    }
    
    public Double getExtraDouble(String key) {
        String value = getExtraString(key);
        if (value == null) return null;
        return Double.valueOf(value);
    }
    
    public boolean hasExtra(String key) {
        if (key == null) return false;
        
        for (EventDataExtra e : mData.getExtras()) {
            if (key.equals(e.getKey())) {
                return true;
            }
        }
        
        return false;
    }

    public boolean equals(EventType obj) {
        return mData.getType().equals(obj);
    }

    @Override
    public String toString() {
        return mData.getType().toString();
    }
}
