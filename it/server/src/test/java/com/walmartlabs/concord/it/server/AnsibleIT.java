package com.walmartlabs.concord.it.server;

import com.walmartlabs.concord.server.api.process.ProcessResource;
import com.walmartlabs.concord.server.api.process.ProcessStatus;
import com.walmartlabs.concord.server.api.process.ProcessStatusResponse;
import com.walmartlabs.concord.server.api.process.StartProcessResponse;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;

import static org.junit.Assert.assertEquals;

public class AnsibleIT extends AbstractServerIT {

    @Test
    public void testHello() throws Exception {
        URI dir = AnsibleIT.class.getResource("ansible").toURI();
        byte[] payload = archive(dir, getDependenciesDir());

        // ---

        ProcessResource processResource = proxy(ProcessResource.class);
        StartProcessResponse spr = processResource.start(new ByteArrayInputStream(payload));

        // ---

        ProcessStatusResponse pir = waitForCompletion(processResource, spr.getInstanceId());
        assertEquals(ProcessStatus.FINISHED, pir.getStatus());

        // ---

        byte[] ab = getLog(pir);
        assertLog(".*Hello, world.*", ab);
    }
}
