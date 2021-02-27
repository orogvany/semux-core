package org.semux.consensus;

import org.semux.Kernel;
import org.semux.crypto.Key;
import org.slf4j.LoggerFactory;

/**
 * Allow multiple validators
 */
public class SlaveBft extends SemuxBft
{


    public SlaveBft(Kernel kernel, Key key, String keyKey)
    {
        super(kernel);

        //override the coinbase
        this.coinbase = key;

        logger = LoggerFactory.getLogger(keyKey);
    }

    protected void enterFinalize()
    {
        // don't do it, main one will handle it

        state = State.FINALIZE;
        resetTimeout(config.bftFinalizeTimeout());

        sync(height + 1);

    }

    protected void sync(long target)
    {
        if (status == Status.RUNNING)
        {
            // change status
            status = Status.SYNCING;

            // reset votes, timer, and events
            clearVotes();
            clearTimerAndEvents();

            // start syncing
            //syncMgr.start(target);

            // restore status if not stopped
            if (status != Status.STOPPED)
            {
                status = Status.RUNNING;

                // enter new height
                enterNewHeight();
            }
        }
    }
}
