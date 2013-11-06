package webmanager;

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Test;

import de.tubs.cs.ibr.hydra.webmanager.server.data.JsonStats;

public class JsonStatsTest {
    
    private static final String TEST_DATA = "{"
            + "\"position\": {\"y\": 0, \"x\": 0, \"state\": 0, \"z\": 0},"
            + "\"dtnd\": {"
                + "\"info\": {\"Neighbors\": 0, \"Uptime\": 38, \"Storage-size\": 0},"
                + "\"bundles\": {\"Received\": 0, \"Generated\": 0, \"Stored\": 0, \"Transmitted\": 0, \"Aborted\": 0, \"Requeued\": 0, \"Expired\": 0, \"Queued\": 0}"
            + "},"
            + "\"iface\": {"
                + "\"lo\": {\"rx\": 0, \"tx\": 0},"
                + "\"eth1\": {\"rx\": 0, \"tx\": 2846},"
                + "\"eth0\": {\"rx\": 7330, \"tx\": 2784}"
            + "},"
            + "\"clock\": {\"Delay\": 0.02614, \"Timex-tick\": 10000, \"Timex-offset\": 0, \"Timex-freq\": 0, \"Offset\": 1.07629}}";
    
    private static final String MULTI_TEST_DATA = "{"
            + "\"1\": {"
                + "\"position\": {\"y\": 0, \"x\": 0, \"state\": 0, \"z\": 0},"
                + "\"dtnd\": {"
                    + "\"info\": {\"Neighbors\": 0, \"Uptime\": 38, \"Storage-size\": 0},"
                    + "\"bundles\": {\"Received\": 0, \"Generated\": 0, \"Stored\": 0, \"Transmitted\": 0, \"Aborted\": 0, \"Requeued\": 0, \"Expired\": 0, \"Queued\": 0}"
                + "},"
                + "\"iface\": {"
                    + "\"lo\": {\"rx\": 0, \"tx\": 0},"
                    + "\"eth1\": {\"rx\": 0, \"tx\": 2846},"
                    + "\"eth0\": {\"rx\": 7330, \"tx\": 2784}"
                + "},"
                + "\"clock\": {\"Delay\": 0.02614, \"Timex-tick\": 10000, \"Timex-offset\": 0, \"Timex-freq\": 0, \"Offset\": 1.07629}"
            + "},"
            + "\"2\": {"
                + "\"position\": {\"y\": 0, \"x\": 0, \"state\": 0, \"z\": 0},"
                + "\"dtnd\": {"
                    + "\"info\": {\"Neighbors\": 0, \"Uptime\": 38, \"Storage-size\": 0},"
                    + "\"bundles\": {\"Received\": 0, \"Generated\": 0, \"Stored\": 0, \"Transmitted\": 0, \"Aborted\": 0, \"Requeued\": 0, \"Expired\": 0, \"Queued\": 0}"
                + "},"
                + "\"iface\": {"
                    + "\"lo\": {\"rx\": 0, \"tx\": 0},"
                    + "\"eth1\": {\"rx\": 0, \"tx\": 2846},"
                    + "\"eth0\": {\"rx\": 7330, \"tx\": 2784}"
                + "},"
                + "\"clock\": {\"Delay\": 0.02614, \"Timex-tick\": 10000, \"Timex-offset\": 0, \"Timex-freq\": 0, \"Offset\": 1.07629}"
            + "}"
        + "}";
    
    @Test
    public void testSplitAll() {
        HashMap<Long, String> data = JsonStats.splitAll(MULTI_TEST_DATA);
        Assert.assertEquals(2, data.size());
    }

}
