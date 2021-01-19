package com.aruistar.pgwal2mqtt

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcess
import com.zaxxer.nuprocess.NuProcessBuilder

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class ProcessHandler extends NuAbstractProcessHandler {
    private NuProcess nuProcess;

    @Override
    public void onStart(NuProcess nuProcess) {
        this.nuProcess = nuProcess;
    }

    @Override
    public boolean onStdinReady(ByteBuffer buffer) {
        buffer.put("Hello world!".getBytes());
        buffer.flip();
        return false; // false means we have nothing else to write at this time
    }

    @Override
    public void onStdout(ByteBuffer buffer, boolean closed) {
        if (!closed) {
            byte[] bytes = new byte[buffer.remaining()];
            // You must update buffer.position() before returning (either implicitly,
            // like this, or explicitly) to indicate how many bytes your handler has consumed.
            buffer.get(bytes);
            System.out.println(new String(bytes));

            // For this example, we're done, so closing STDIN will cause the "cat" process to exit
            nuProcess.closeStdin(true);
        }
    }
}

NuProcessBuilder pb = new NuProcessBuilder(Arrays.asList("/bin/cat"));
ProcessHandler handler = new ProcessHandler();
pb.setProcessListener(handler);
NuProcess process = pb.start();

ByteBuffer buffer = ByteBuffer.wrap("Hello, World!".getBytes());
process.writeStdin(buffer);

process.waitFor(0, TimeUnit.SECONDS);

