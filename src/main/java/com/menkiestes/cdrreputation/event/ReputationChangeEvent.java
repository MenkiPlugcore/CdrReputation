package com.menkiestes.cdrreputation.event;

import com.menkiestes.cdrreputation.model.ReputationChange;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ReputationChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ReputationChange change;
    public ReputationChangeEvent(ReputationChange change) { this.change = change; }
    public ReputationChange getChange() { return change; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
