package org.semux.consensus;

import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.semux.Kernel;
import org.semux.crypto.Hex;
import org.semux.crypto.Key;
import org.semux.net.Channel;
import org.semux.net.msg.Message;

public class MultiBft extends SemuxBft
{

    private SemuxBft bft;
    private List<SlaveBft> slaves = new ArrayList<>();

    public MultiBft(Kernel kernel)
    {
        //unused
        super(kernel);

        this.bft = new SemuxBft(kernel);

        Map<String, Key> keys = new HashMap<>();

        //todo - configuration of keys or get from wallet

        for (Map.Entry<String, Key> key : keys.entrySet())
        {
            slaves.add(new SlaveBft(kernel, key.getValue(), key.getKey()));
        }
    }

    @Override
    public void start()
    {
        new Thread(bft::start, "consensus").start();
        for (SlaveBft slave : slaves)
        {
            new Thread(slave::start, "consensus").start();
        }
    }

    @Override
    public void stop()
    {
        bft.stop();
        for (SlaveBft slave : slaves)
        {
            slave.stop();
        }
    }

    @Override
    public boolean isRunning()
    {
        return bft.isRunning();
    }

    @Override
    public void onMessage(Channel channel, Message msg)
    {
        bft.onMessage(channel, msg);
        for (SlaveBft slave : slaves)
        {
            slave.onMessage(channel, msg);
        }
    }
}
