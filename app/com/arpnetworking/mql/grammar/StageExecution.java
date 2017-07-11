package com.arpnetworking.mql.grammar;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Stage execution model.
 *
 * @author Brandon Arp (brandon dot arp at smartsheet dot com)
 */
public interface StageExecution {
    CompletionStage<TimeSeriesResult> execute();
    List<StageExecution> dependencies();
}
