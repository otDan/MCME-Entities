package com.mcmiddleearth.entities.ai.goal;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.pathfinding.Pathfinder;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.events.events.goal.GoalFinishedEvent;
import com.mcmiddleearth.entities.events.events.goal.GoalVirtualEntityIsClose;
import org.bukkit.Location;

public class GoalLocationTargetGoto extends GoalLocationTarget {

    public GoalLocationTargetGoto(GoalType type, VirtualEntity entity, Pathfinder pathfinder, Location target) {
        super(type, entity, pathfinder, target);
    }

    @Override
    public void update() {
        if(isCloseToTarget(GoalDistance.POINT)) {
            EntitiesPlugin.getEntityServer().handleEvent(new GoalVirtualEntityIsClose(getEntity(),this));
            clearHeadGoals();
            setIsMoving(false);
            setFinished();
        }
        super.update();
    }

}