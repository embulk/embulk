package org.jruby.util;

import org.joda.time.DateTime;

public class Temporal // TODO better naming
{
    private final DateTime dateTime;
    private final int nano;
    private final String zone;  // +0900, JST, UTC
    // need more fields?

    public Temporal(DateTime dateTime, int nano, String zone)
    {
        this.dateTime = dateTime;
        this.nano = nano;
        this.zone = zone;
    }

    public DateTime getDateTime()
    {
        return dateTime;
    }

    public int getNano()
    {
        return nano;
    }

    public String getZone()
    {
        return zone;
    }
}
